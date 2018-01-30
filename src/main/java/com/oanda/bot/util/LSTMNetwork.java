package com.oanda.bot.util;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.GravesLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions;


public class LSTMNetwork {

    private static final double learningRate = 0.0001;
    private static final int iterations = 1;
    private static final long seed = 777L;

    private static final int lstmLayer1Size = 120;
    private static final int lstmLayer2Size = 40;
    private static final int denseLayerSize = 5;
    private static final double dropoutRatio = 0.5; //0.5

    public static MultiLayerNetwork buildLstmNetworks(DataSetIterator iterator) {
        int layer = 0;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .iterations(iterations)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.ADAM)
                .learningRate(learningRate)
                .l2(0.001)
                .regularization(true)
                .list()
                .layer(layer++, new GravesLSTM.Builder()
                        .nIn(iterator.inputColumns())
                        .nOut(lstmLayer1Size)
                        .activation(Activation.TANH) //TANH
                        .gateActivationFunction(Activation.HARDSIGMOID) //HARDSIGMOID
                        .dropOut(dropoutRatio)
                        .updater(Updater.RMSPROP) //-
                        .build())
                .layer(layer++, new GravesLSTM.Builder()
                        .nIn(lstmLayer1Size)
                        .nOut(lstmLayer2Size)
                        .activation(Activation.HARDTANH) //TANH
                        .gateActivationFunction(Activation.HARDSIGMOID) //HARDSIGMOID
                        .dropOut(dropoutRatio)
                        .updater(Updater.RMSPROP) //-
                        .build())
                .layer(layer++, new DenseLayer.Builder()
                        .nIn(lstmLayer2Size)
                        .nOut(denseLayerSize)
                        .activation(Activation.RELU) //RELU
                        .updater(Updater.RMSPROP) //-
                        .build())
                .layer(layer++, new RnnOutputLayer.Builder()
                        .nIn(denseLayerSize)
                        .nOut(iterator.totalOutcomes())
                        .activation(Activation.IDENTITY) //IDENTITY
                        .lossFunction(LossFunctions.LossFunction.MSE) //MSE
                        .updater(Updater.RMSPROP) //-
                        .build())
                .pretrain(false)
                .backprop(true)
                .build();

        MultiLayerNetwork net = new MultiLayerNetwork(conf);
        net.init();
        net.setListeners(new ScoreIterationListener(1));

        int epochs = 10_000; // training epochs
        for (int epoch = 0; epoch < epochs; epoch++) {
            while (iterator.hasNext()) {
                // fit model using mini-batch data
                net.fit(iterator.next());
            }
            // reset iterator
            iterator.reset();
            net.rnnClearPreviousState();
        }

        return net;
    }
}
