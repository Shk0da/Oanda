package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.util.LSTMNetwork;
import com.oanda.bot.util.StockDataSetIterator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.joda.time.DateTime;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private volatile Double lastCandleClose = 0D;

    @Autowired
    private CandleRepository candleRepository;

    @Getter
    @Setter
    public volatile Status status = Status.NOTHING;

    private double closeMin = Double.MAX_VALUE;
    private double closeMax = Double.MIN_VALUE;

    @Value("${oandabot.sensitivity.trend}")
    private Double sensitivityTrend;

    public LearnActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
    }

    @Override
    public void onReceive(Object message) {
        try {
            if (Messages.WORK.equals(message) && neuralNetwork != null) {
                predict();
            }

            boolean needTraining = (lastLearn == null)
                    || (DateTime.now().getMillis() - lastLearn.getMillis()) < TimeUnit.HOURS.toMillis(24);
            if (message instanceof Candle && needTraining) {
                training();
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
    }

    private void training() {
        if (status.equals(Status.TRAINED)) return;

        setStatus(Status.TRAINED);
        log.info("Start training {} {}", instrument.getDisplayName(), step.name());

        List<Candle> candles = candleRepository.getLastCandles(instrument, step, 10_000);
        StockDataSetIterator iterator = new StockDataSetIterator(candles, 256, 1);
        closeMin = iterator.getCloseMin();
        closeMax = iterator.getCloseMax();
        neuralNetwork = LSTMNetwork.buildLstmNetworks(iterator);

        lastLearn = DateTime.now();
        log.info("Stop training {} {}", instrument.getDisplayName(), step.name());
        setStatus(Status.READY);
    }

    private void predict() {
        List<Candle> last = candleRepository.getLastCandles(instrument, step, VECTOR_SIZE);

        // check vector
        if (last.size() < VECTOR_SIZE) return;

        // check new data
        double vectorClose = last.get(4).getCloseMid();
        if (lastCandleClose > 0 && lastCandleClose == vectorClose) return;
        lastCandleClose = vectorClose;

        INDArray input = Nd4j.create(new int[]{1, VECTOR_SIZE}, 'f');
        input.putScalar(new int[]{0, 0}, normalize(last.get(0).getCloseMid(), closeMin, closeMax));
        input.putScalar(new int[]{0, 1}, normalize(last.get(1).getCloseMid(), closeMin, closeMax));
        input.putScalar(new int[]{0, 2}, normalize(last.get(2).getCloseMid(), closeMin, closeMax));
        input.putScalar(new int[]{0, 3}, normalize(last.get(3).getCloseMid(), closeMin, closeMax));
        input.putScalar(new int[]{0, 4}, normalize(last.get(4).getCloseMid(), closeMin, closeMax));

        INDArray output = neuralNetwork.rnnTimeStep(input);
        double closePrice = Precision.round(deNormalize(output.getDouble(0), closeMin, closeMax), 5);
        if (closePrice != Double.NaN && closePrice > 0 && closePrice != lastPredict) {
            if (lastPredict > 0) {
                Messages.Predict.Signal signal = Messages.Predict.Signal.NONE;
                if (closePrice > lastPredict && (closePrice / (lastPredict / 100) - 100) > sensitivityTrend) {
                    signal = Messages.Predict.Signal.UP;
                }

                if (closePrice < lastPredict && (lastPredict / (closePrice / 100) - 100) > sensitivityTrend) {
                    signal = Messages.Predict.Signal.DOWN;
                }

                if (!Messages.Predict.Signal.NONE.equals(signal)) {
                    getContext()
                            .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "TradeActor_" + instrument.getInstrument() + "_" + step.name())
                            .tell(new Messages.Predict(signal), self());
                }
            }

            lastPredict = closePrice;
        }
    }
}
