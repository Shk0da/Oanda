package com.oanda.bot.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.util.learning.LSTMNetwork;
import com.oanda.bot.util.learning.StockDataSetIterator;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;
import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.learning.SupervisedLearning;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.learning.BackPropagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.oanda.bot.util.learning.StockDataSetIterator.deNormalize;
import static com.oanda.bot.util.learning.StockDataSetIterator.normalize;

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
    public void preStart() {
        predicatorActor = getContext().actorOf(
                Props.create(SpringDIActor.class, PredicatorActor.class, instrument, step), "PredicatorActor_" + instrument.getInstrument() + "_" + step.name()
        );
        log.info("LearnActor make PredicatorActor_" + instrument.getInstrument() + "_" + step.name());
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof Candle) {
            if (!status.equals(Status.TRAINED)) {
                train();
            }
        }

        if (Messages.WORK.equals(message)) {
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

    @Synchronized
    private void train() {
        List<Candle> candles = candleRepository.getCandles(instrument, step);
        if (candles.isEmpty()) return;

        setStatus(Status.TRAINED);

        log.info("Training {} {}", instrument.getDisplayName(), step.name());

        int batchSize = 64; // mini-batch size
        double splitRatio = log.isDebugEnabled()
                ? 0.95  // 95% for training, 10% for testing
                : 0.95; //todo!
        int epochs = 500; // training epochs
        StockDataSetIterator iterator = new StockDataSetIterator(candles, batchSize, splitRatio);
        MultiLayerNetwork net = LSTMNetwork.buildLstmNetworks(iterator.inputColumns(), iterator.totalOutcomes());
        for (int epoch = 0; epoch < epochs; epoch++) {
            while (iterator.hasNext()) {
                // fit model using mini-batch data
                net.fit(iterator.next());
            }
            // reset iterator
            iterator.reset();
            net.rnnClearPreviousState();
        }

        try {
            String rnnName = instrument.getInstrument() + step.name() + String.valueOf(System.currentTimeMillis());
            model = File.createTempFile(rnnName, ".rnn");
            ModelSerializer.writeModel(net, model, true);
            model.deleteOnExit();
            openMin = iterator.getOpenMin();
            openMax = iterator.getOpenMax();
            lowMin = iterator.getLowMin();
            lowMax = iterator.getLowMax();
            highMin = iterator.getHighMin();
            highMax = iterator.getHighMax();
            closeMin = iterator.getCloseMin();
            closeMax = iterator.getCloseMax();
            volumeMin = iterator.getVolumeMin();
            volumeMax = iterator.getVolumeMax();
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }



//        Map<Double, Double> a2p = Maps.newLinkedHashMap();
//        NeuralNetwork<BackPropagation> neuralNetwork = getBackPropagationNeuralNetwork(iterator.getTrain());
//        int split = (int) Math.round(candles.size() * splitRatio);
//        List<Candle> train = candles.subList(0, split);
//        for (Candle candle : candles.subList(split, candles.size())) {
//            neuralNetwork.calculate();
//            double[] nnResult = neuralNetwork.getOutput();
//            double predict = deNormalize(nnResult[0], iterator.getCloseMin(), iterator.getCloseMax());
//
//            a2p.put(
//                    Precision.round(candle.getCloseMid(), 5),
//                    Precision.round(predict, 5)
//            );
//
//            DataSet trainingSet = new DataSet(5, 1);
//            trainingSet.addRow(new DataSetRow(
//                            new double[] {
//                                    normalize(train.get(train.size()-4).getCloseMid(), closeMin, closeMax),
//                                    normalize(train.get(train.size()-3).getCloseMid(), closeMin, closeMax),
//                                    normalize(train.get(train.size()-2).getCloseMid(), closeMin, closeMax),
//                                    normalize(train.get(train.size()-1).getCloseMid(), closeMin, closeMax),
//                                    normalize(candle.getCloseMid(), closeMin, closeMax)
//                            },
//                            new double[] {normalize(candle.getCloseMid(), closeMin, closeMax)}
//                    )
//            );
//            neuralNetwork.learn(trainingSet);
//            train.add(candle);
//        }
//
//        System.out.println(a2p);


        if (log.isDebugEnabled()) {
            log.debug("Testing...");
            List<Pair<INDArray, Double>> testDataSet = iterator.getTest();
            Map<Double, Double> actualToPredict = Maps.newLinkedHashMap();
            int steps = testDataSet.size() - 1;
            for (int i = 0; i < steps; i++) {
                INDArray input = testDataSet.get(i).getKey();
                INDArray output = net.rnnTimeStep(input);
                double predict = deNormalize(output.getDouble(0), iterator.getCloseMin(), iterator.getCloseMax());
                double actual = deNormalize(testDataSet.get(i + 1).getValue(), iterator.getCloseMin(), iterator.getCloseMax());
                actualToPredict.put(
                        Precision.round(actual, 5),
                        Precision.round(predict, 5)
                );
            }

            log.debug(actualToPredict.toString());
            log.debug("...end");
        }

        setStatus(Status.READY);
    }

    private NeuralNetwork<BackPropagation> getBackPropagationNeuralNetwork(final List<Candle> train) {
        NeuralNetwork<BackPropagation> neuralNetwork = new MultiLayerPerceptron(5, 11, 1);
        int maxIterations = 1000;
        double learningRate = 0.5;
        double maxError = 0.00001;
        SupervisedLearning learningRule = neuralNetwork.getLearningRule();
        learningRule.setMaxError(maxError);
        learningRule.setLearningRate(learningRate);
        learningRule.setMaxIterations(maxIterations);

        DataSet trainingSet = new DataSet(5, 1);
        int lineSize = 5;
        int linesSize = train.size() / lineSize;
        Double[][] tickLines = new Double[linesSize + 1][lineSize];
        int x = 0;
        int y = 0;
        for (Candle tick : train) {
            tickLines[x][y++] = normalize(tick.getCloseMid(), closeMin, closeMax);
            if (y >= 5) {
                x++;
                y = 0;
            }
        }

        for (int i = 0; i <= linesSize; i++) {
            Double[] tokens = tickLines[i];
            Double[] trainValues = new Double[5];
            int expect = 0;
            double tmp = 0D;
            for (int j = 0; j < 5; j++) {
                if (tokens[j] == null) {
                    trainValues[j] = tmp;
                } else {
                    trainValues[j] = tokens[j];
                    expect = j;
                    tmp = trainValues[j];
                }
            }
            double[] set = Stream.of(trainValues).mapToDouble(Double::doubleValue).toArray();
            if (tokens[expect] == null) continue;
            double[] expected = new double[]{tokens[expect]};
            trainingSet.addRow(new DataSetRow(set, expected));
        }

        neuralNetwork.learn(trainingSet);
        return neuralNetwork;
    }
}
