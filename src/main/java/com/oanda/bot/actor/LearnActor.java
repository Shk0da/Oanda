package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Lists;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.provider.ApplicationContextProvider;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.util.CSVUtil;
import com.oanda.bot.util.LSTMNetwork;
import com.oanda.bot.util.StockDataSetIterator;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.joda.time.DateTime;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.oanda.bot.actor.Messages.Predict.Signal;
import static com.oanda.bot.util.StockDataSetIterator.*;

@Slf4j
@Scope("prototype")
@Component("LearnActor")
public class LearnActor extends UntypedAbstractActor {

    public enum Status {NOTHING, TRAINED, READY}

    private final Instrument instrument;
    private final Step step;

    private volatile MultiLayerNetwork neuralNetwork;
    private volatile DateTime lastLearn;
    private volatile Double lastPredict = 0D;
    private volatile List<Double> lastPredicts = Lists.newArrayList();
    private volatile Double lastCandleClose = 0D;

    @Autowired
    private CandleRepository candleRepository;

    @Getter
    @Setter
    public volatile Status status = Status.NOTHING;

    private volatile double closeMin;
    private volatile double closeMax;

    @Value("${predictor.sensitivity.trend}")
    private Double sensitivityTrend;

    @Value("${predictor.learn.interval}")
    private Integer learnInterval;

    @Value("${neuralnetwork.store.disk}")
    private Boolean storeDisk;

    private String locationToSave;

    private TaskScheduler taskScheduler = ApplicationContextProvider.getApplicationContext().getBean(TaskScheduler.class);

    public LearnActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
        this.locationToSave = "NeuralNetwork" + instrument.getName().replaceAll("_", "") + step;
    }

    @Override
    public void onReceive(Object message) {
        if (Messages.LEARN.equals(message)) {
            if (!status.equals(Status.TRAINED)) {
                taskScheduler.schedule(this::trainNetwork, new Date());
            }
        }

        if (Messages.PREDICT.equals(message)) {
            if (getNeuralNetwork() != null) {
                taskScheduler.schedule(this::predict, new Date());
            }
        }
    }

    @Synchronized
    private void predict() {
        Signal signal = Signal.NONE;
        List<Candle> last = candleRepository.getLastCandles(instrument, step, VECTOR_SIZE);

        // check vector
        if (last.size() < VECTOR_SIZE) return;

        // check new data
        double vectorClose = last.get(last.size() - 1).getCloseMid();
        if (lastCandleClose > 0 && lastCandleClose == vectorClose) return;
        lastCandleClose = vectorClose;

        INDArray output;
        try {
            INDArray input = Nd4j.create(new int[]{1, VECTOR_SIZE}, 'f');
            for (int j = VECTOR_SIZE, k = 0; j > 0; j--, k++) {
                input.putScalar(new int[]{0, k}, normalize(last.get(k).getCloseMid(), closeMin, closeMax));
            }
            output = neuralNetwork.rnnTimeStep(input);
        } catch (Exception ex) {
            log.error("Predict {}{} failed: {}", instrument, step, ex.getMessage());
            return;
        }

        double closePrice = Precision.round(deNormalize(output.getDouble(0), closeMin, closeMax), 5);
        if (closePrice != Double.NaN && closePrice > 0 && closePrice != lastPredict) {
            if (lastPredict > 0) {
                int trend = 0;
                int checkCount = (int) (sensitivityTrend * 100);
                if (lastPredicts.size() > checkCount + 1) {
                    double valPrev = lastPredicts.get(lastPredicts.size() - checkCount - 1);
                    for (int i = lastPredicts.size() - checkCount; i < lastPredicts.size(); i++) {
                        double val = lastPredicts.get(i);
                        if (val > valPrev) trend++;
                        if (val < valPrev) trend--;
                        valPrev = val;
                    }
                }
                double spread = last.get(0).getBid().getC() - last.get(0).getAsk().getC();
                boolean diffMoreSpread = Math.abs(closePrice - lastPredict) > spread;

                if (trend >= checkCount - 1 && closePrice > lastPredict && diffMoreSpread && (closePrice / (lastPredict / 100) - 100) > sensitivityTrend) {
                    signal = Signal.UP;
                }

                if (trend <= -(checkCount - 1) && closePrice < lastPredict && diffMoreSpread && (lastPredict / (closePrice / 100) - 100) > sensitivityTrend) {
                    signal = Signal.DOWN;
                }
            }

            lastPredict = closePrice;
            lastPredicts.add(lastPredict);
        }

        if (!Messages.Predict.Signal.NONE.equals(signal)) {
            getContext()
                    .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "TradeActor_" + instrument.getInstrument() + "_" + step.name())
                    .tell(new Messages.Predict(signal), self());
        }
    }

    private void trainNetwork() {
        if (lastLearn != null && (DateTime.now().getMillis() - lastLearn.getMillis()) < TimeUnit.MINUTES.toMillis(learnInterval)) {
            return;
        }

        List<Candle> candles = candleRepository.getLastCandles(instrument, step, candleRepository.getLimit());
        if (candles.size() < candleRepository.getLimit()) return;

        setStatus(Status.TRAINED);

        try {
            StockDataSetIterator iterator = new StockDataSetIterator(candles, 1);
            neuralNetwork = LSTMNetwork.buildLstmNetworks(iterator);
            closeMin = iterator.getCloseMin();
            closeMax = iterator.getCloseMax();

            if (storeDisk) {
                try {
                    String filePath = locationToSave + "_" + closeMin + "_" + closeMax;
                    ModelSerializer.writeModel(neuralNetwork, filePath, true);
                    log.info("The model is saved to disk: {}", filePath);
                    CSVUtil.saveCandles(candles, filePath + "Data");
                    log.info("The data is saved to disk also");
                } catch (Exception ex) {
                    log.error("Failed save to disk {}{}: {}", instrument, step, ex.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("Failed create network {}{}: {}", instrument, step, ex.getMessage());
        }

        lastLearn = DateTime.now();
        setStatus(Status.READY);
    }

    @Synchronized
    private MultiLayerNetwork getNeuralNetwork() {
        if (storeDisk && neuralNetwork == null && locationToSave != null) {
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get("."), locationToSave + "*")) {
                List<Path> paths = Lists.newArrayList(dirStream.iterator())
                        .stream()
                        .filter(path -> !path.toString().contains(".csv"))
                        .sorted(Comparator.comparing(o -> o.toFile().lastModified()))
                        .collect(Collectors.toList());

                if (!paths.isEmpty()) {
                    String fileName = paths.get(paths.size() - 1).toFile().getName();
                    neuralNetwork = ModelSerializer.restoreMultiLayerNetwork(fileName);
                    int firstDelimiter = fileName.indexOf('_');
                    int secondDelimiter = fileName.lastIndexOf('_');
                    closeMin = Double.valueOf(fileName.substring(firstDelimiter + 1, secondDelimiter));
                    closeMax = Double.valueOf(fileName.substring(secondDelimiter + 1, fileName.length()));
                    log.info("The model is loaded from the disk: {}", fileName);
                    lastLearn = DateTime.now();
                }
            } catch (IOException ex) {
                log.error(ex.getMessage());
            }
        }

        return neuralNetwork;
    }
}
