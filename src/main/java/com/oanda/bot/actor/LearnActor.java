package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Maps;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Scope("prototype")
@Component("LearnActor")
public class LearnActor extends UntypedAbstractActor {

    private final Instrument instrument;
    private final Step step;

    private volatile Double lastPredict = 0D;
    private int vector = 14;

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
    public void onReceive(Object message) {
        if (message instanceof Candle) {
            if (!status.equals(Status.TRAINED)) {
                setStatus(Status.TRAINED);
                List<Candle> candles = candleRepository.getLastCandles(instrument, step, 10_000);
                log.info("Start training {} {}", instrument.getDisplayName(), step.name());
                NeuralNetwork<BackPropagation> neuralNetwork = getBackPropagationNeuralNetwork(candles);
                log.info("Stop training {} {}", instrument.getDisplayName(), step.name());
                setStatus(Status.READY);

                List<Candle> last = candleRepository.getLastCandles(instrument, step, vector);
                double[] set = new double[vector];
                for (int j = 0; j < vector; j++) {
                    set[j] = normalize(last.get(j).getCloseMid(), closeMin, closeMax);
                }
                neuralNetwork.setInput(set);

                neuralNetwork.calculate();
                double closePrice = deNormalize(neuralNetwork.getOutput()[0], closeMin, closeMax);

                if (closePrice != Double.NaN && closePrice > 0 && closePrice != lastPredict) {
                    lastPredict = closePrice;
                    sender().tell(new Messages.Predict(lastPredict), self());
                }
            }
        }
    }

    private NeuralNetwork<BackPropagation> getBackPropagationNeuralNetwork(final List<Candle> candles) {
        int shiftVector = 5;
        int maxIterations = 10000;
        double learningRate = 0.45;
        double maxError = 0.00001;
        NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(vector, vector * 2 + 1, 1);
        BackPropagation learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);
        learningRule.setBatchMode(true);
        neuralNetwork.setLearningRule(learningRule);

        candles.parallelStream().forEach(candle -> {
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

        double splitRatio = (log.isDebugEnabled()) ? 0.8 : 1;
        int split = (int) Math.round(candles.size() * splitRatio);
        List<Candle> train = candles.subList(0, split);
        DataSet trainingSet = getDataSet(train, vector, shiftVector);
        neuralNetwork.learn(trainingSet);

        if (log.isDebugEnabled()) {
            log.debug("Testing...");

            Map<String, String> actualToPredict = Maps.newLinkedHashMap();
            List<Candle> test = candles.subList(split, candles.size());
            for (int i = 0; i < test.size() - vector - shiftVector; i = i + vector) {
                double[] set = new double[vector];
                for (int j = 0; j < vector; j++) {
                    set[j] = normalize(test.get(i + j).getCloseMid(), closeMin, closeMax);
                }
                neuralNetwork.setInput(set);
                neuralNetwork.calculate();
                double predict = deNormalize(neuralNetwork.getOutput()[0], closeMin, closeMax);

                actualToPredict.put(
                        String.format("%.5f", test.get(i + shiftVector).getCloseMid()),
                        String.format("%.5f", predict)
                );
            }

            log.debug(actualToPredict.toString());
            log.debug("...end");
        }

        return neuralNetwork;
    }

    private DataSet getDataSet(List<Candle> train, int vector, int shiftVector) {
        DataSet trainingSet = new DataSet(vector, 1);
        for (int i = 0; i < train.size() - vector - shiftVector; i = i + vector) {
            double[] set = new double[vector];
            for (int j = 0; j < vector; j++) {
                set[j] = normalize(train.get(i + j).getCloseMid(), closeMin, closeMax);
            }
            double[] expected = new double[]{
                    normalize(train.get(i + vector + shiftVector).getCloseMid(), closeMin, closeMax)
            };
            trainingSet.addRow(new DataSetRow(set, expected));
        }

        return trainingSet;
    }

    private double normalize(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    private double deNormalize(double input, double min, double max) {
        return min + (input - 0.1) * (max - min) / 0.8;
    }
}
