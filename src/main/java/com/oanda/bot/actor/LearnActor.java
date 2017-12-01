package com.oanda.bot.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.util.learning.LSTMNetwork;
import com.oanda.bot.util.learning.StockDataSetIterator;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Slf4j
@Scope("prototype")
@Component("LearnActor")
public class LearnActor extends UntypedAbstractActor {

    private final Instrument instrument;
    private final Step step;

    private ActorRef predicatorActor;

    @Autowired
    private CandleRepository candleRepository;

    public enum Status {NOTHING, TRAINED, READY}

    @Getter
    @Setter
    public volatile Status status = Status.NOTHING;

    @Getter
    @Setter
    public volatile File model;

    private double openMin = Double.MAX_VALUE;
    private double openMax = Double.MIN_VALUE;
    private double lowMin = Double.MAX_VALUE;
    private double lowMax = Double.MIN_VALUE;
    private double highMin = Double.MAX_VALUE;
    private double highMax = Double.MIN_VALUE;
    private double closeMin = Double.MAX_VALUE;
    private double closeMax = Double.MIN_VALUE;
    private double volumeMin = Double.MAX_VALUE;
    private double volumeMax = Double.MIN_VALUE;

    public LearnActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
    }

    @Override
    public void preStart() throws Exception {
        predicatorActor = getContext().actorOf(
                Props.create(SpringDIActor.class, PredicatorActor.class, instrument, step), "PredicatorActor_" + instrument.getInstrument() + "_" + step.name()
        );
        log.info("LearnActor make PredicatorActor_" + instrument.getInstrument() + "_" + step.name());
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (Messages.WORK.equals(message)) {
            if (!status.equals(Status.TRAINED)) {
                train();
            }

            if (status.equals(Status.READY)) {
                predicatorActor.tell(new Messages.LearnModel(
                        getModel(),
                        openMin,
                        openMax,
                        lowMin,
                        lowMax,
                        highMin,
                        highMax,
                        closeMin,
                        closeMax,
                        volumeMin,
                        volumeMax
                ), sender());
            }
        }
    }

    private void train() {
        List<Candle> candles = candleRepository.getCandles(instrument, step);
        if (candles.isEmpty()) return;

        setStatus(Status.TRAINED);

        log.info("Training {} {}", instrument.getDisplayName(), step.name());

        int batchSize = 64; // mini-batch size
        double splitRatio = log.isDebugEnabled()
                ? 0.9  // 90% for training, 10% for testing
                : 1;
        int epochs = 100; // training epochs
        StockDataSetIterator iterator = new StockDataSetIterator(candles, batchSize, splitRatio);
        MultiLayerNetwork net = LSTMNetwork.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());
        for (int epoch = 0; epoch < epochs; epoch++) {
            while (iterator.hasNext()) {
                // fit model using mini-batch data
                net.fit(iterator.next());
            }
            // reset iterator
            iterator.reset();
            // clear current stance from the last example
            net.rnnClearPreviousState();
        }

        try {
            String rnnName = instrument.getInstrument() + step.name() + String.valueOf(System.currentTimeMillis());
            model = File.createTempFile(rnnName, ".rnn");
            ModelSerializer.writeModel(net, model, true);
            model.deleteOnExit();
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }

        if (log.isDebugEnabled()) {
            log.debug("Testing...");
            List<Pair<INDArray, Double>> test = iterator.getTestDataSet();
            double max = iterator.getMaxNum()[1];
            double min = iterator.getMinNum()[1];
            double[] predicts = new double[test.size()];
            double[] actuals = new double[test.size()];
            for (int i = 0; i < test.size(); i++) {
                predicts[i] = net.rnnTimeStep(test.get(i).getKey()).getDouble(candles.size() - 1) * (max - min) + min;
                actuals[i] = test.get(i).getValue();
            }

            // print out
            log.debug("Predict, Actual");
            for (int i = 0; i < predicts.length; i++) {
                log.debug(predicts[i] + ", " + actuals[i]);
            }
        }

        candles.forEach(candle -> {
            openMin = (candle.getOpenMid() < openMin) ? candle.getOpenMid() : openMin;
            openMax = (candle.getOpenMid() > openMax) ? candle.getOpenMid() : openMax;

            lowMin = (candle.getLowMid() < lowMin) ? candle.getLowMid() : lowMin;
            lowMax = (candle.getLowMid() > lowMax) ? candle.getLowMid() : lowMax;

            highMin = (candle.getHighMid() < highMin) ? candle.getHighMid() : highMin;
            highMax = (candle.getHighMid() > highMax) ? candle.getHighMid() : highMax;

            closeMin = (candle.getCloseMid() < closeMin) ? candle.getCloseMid() : closeMin;
            closeMax = (candle.getCloseMid() > closeMax) ? candle.getCloseMid() : closeMax;

            volumeMin = (candle.getVolume() < volumeMin) ? candle.getVolume() : volumeMin;
            volumeMax = (candle.getVolume() > volumeMax) ? candle.getVolume() : volumeMax;
        });

        setStatus(Status.READY);
    }
}
