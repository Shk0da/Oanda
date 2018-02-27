package com.oanda.bot.util;

import com.oanda.bot.domain.Candle;
import lombok.Getter;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class StockDataSetIterator implements DataSetIterator {

    public static final int VECTOR_SIZE = 5;
    public static final int LENGTH = 22;
    public static final int MINI_BATCH_SIZE = 32;

    @Getter
    private double openMin = Double.MAX_VALUE;
    @Getter
    private double openMax = Double.MIN_VALUE;
    @Getter
    private double lowMin = Double.MAX_VALUE;
    @Getter
    private double lowMax = Double.MIN_VALUE;
    @Getter
    private double highMin = Double.MAX_VALUE;
    @Getter
    private double highMax = Double.MIN_VALUE;
    @Getter
    private double closeMin = Double.MAX_VALUE;
    @Getter
    private double closeMax = Double.MIN_VALUE;
    @Getter
    private double volumeMin = Double.MAX_VALUE;
    @Getter
    private double volumeMax = Double.MIN_VALUE;

    private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();
    private DataSetPreProcessor dataSetPreProcessor;

    @Getter
    private List<Candle> train;
    @Getter
    private List<Pair<INDArray, Double>> test;

    public StockDataSetIterator(List<Candle> stockDataList, double splitRatio) {
        int split = (int) Math.round(stockDataList.size() * splitRatio);

        stockDataList.forEach(candle -> {
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

        train = stockDataList.subList(0, split);
        test = generateTestDataSet(stockDataList.subList(split, stockDataList.size()));

        initializeOffsets();
    }

    private void initializeOffsets() {
        exampleStartOffsets.clear();
        int window = LENGTH + 1;
        for (int i = 0; i < train.size() - window; i++) {
            exampleStartOffsets.add(i);
        }
    }

    @Override
    public DataSet next(int num) {
        if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();
        int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());

        INDArray input = Nd4j.create(new int[]{actualMiniBatchSize, VECTOR_SIZE, LENGTH}, 'f');
        INDArray label = Nd4j.create(new int[]{actualMiniBatchSize, 1, LENGTH}, 'f');
        for (int index = 0; index < actualMiniBatchSize; index++) {
            int startIdx = exampleStartOffsets.removeFirst() + VECTOR_SIZE;
            int endIdx = startIdx + LENGTH - VECTOR_SIZE;
            for (int i = startIdx; i < endIdx; i++) {
                int c = i - startIdx;
                //input
                for (int j = VECTOR_SIZE, k = 0; j > 0; j--, k++) {
                    input.putScalar(new int[]{index, k, c}, normalize(train.get(startIdx - j).getCloseMid(), closeMin, closeMax));
                }
                // label
                label.putScalar(new int[]{index, 0, c}, normalize(train.get(startIdx + 1).getCloseMid(), closeMin, closeMax));
            }
            if (exampleStartOffsets.size() == 0) break;
        }

        return new DataSet(input, label);
    }

    @Override
    public int totalExamples() {
        return train.size() - LENGTH - 1;
    }

    @Override
    public int inputColumns() {
        return VECTOR_SIZE;
    }

    @Override
    public int totalOutcomes() {
        return 1;
    }

    @Override
    public boolean resetSupported() {
        return false;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    @Override
    public void reset() {
        initializeOffsets();
    }

    @Override
    public int batch() {
        return MINI_BATCH_SIZE;
    }

    @Override
    public int cursor() {
        return totalExamples() - exampleStartOffsets.size();
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor dataSetPreProcessor) {
        this.dataSetPreProcessor = dataSetPreProcessor;
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return dataSetPreProcessor;
    }

    @Override
    public List<String> getLabels() {
        return null;
    }

    @Override
    public boolean hasNext() {
        return exampleStartOffsets.size() > 0;
    }

    @Override
    public DataSet next() {
        return next(MINI_BATCH_SIZE);
    }

    private List<Pair<INDArray, Double>> generateTestDataSet(List<Candle> stockDataList) {
        List<Pair<INDArray, Double>> test = new ArrayList<>();
        for (int i = VECTOR_SIZE; i < stockDataList.size() - 1; i++) {
            INDArray input = Nd4j.create(new int[]{1, VECTOR_SIZE}, 'f');
            for (int j = VECTOR_SIZE, k = 0; j > 0; j--, k++) {
                input.putScalar(new int[]{0, k}, normalize(stockDataList.get(i - j).getCloseMid(), closeMin, closeMax));
            }
            test.add(new Pair<>(input, stockDataList.get(i).getCloseMid()));
        }
        return test;
    }

    public static double normalize(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.0001;
    }

    public static double deNormalize(double input, double min, double max) {
        return min + (input - 0.0001) * (max - min) / 0.8;
    }
}
