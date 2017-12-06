package com.oanda.bot.util.learning;

import com.google.common.collect.Lists;
import com.oanda.bot.domain.Candle;
import lombok.Getter;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

public class StockDataSetIterator implements DataSetIterator {

    public static final int VECTOR_SIZE = 5;

    private int miniBatchSize;
    private int exampleLength;

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

    @Getter
    private List<Candle> train;
    @Getter
    private List<Pair<INDArray, Double>> test;

    public StockDataSetIterator(List<Candle> stockDataList, int miniBatchSize, double splitRatio) {
        this.miniBatchSize = miniBatchSize;
        this.exampleLength = stockDataList.size();
        int split = (int) Math.round(stockDataList.size() * splitRatio);
        train = stockDataList.subList(0, split);

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

        test = generateTestDataSet(stockDataList.subList(split, stockDataList.size()));
        initializeOffsets();
    }

    private void initializeOffsets() {
        exampleStartOffsets.clear();
        int window = exampleLength + 1;
        for (int i = 0; i < train.size() - window; i++) {
            exampleStartOffsets.add(i);
        }
    }

    @Override
    public DataSet next(int num) {
        if (exampleStartOffsets.size() == 0) throw new NoSuchElementException();
        int actualMiniBatchSize = Math.min(num, exampleStartOffsets.size());
        INDArray input = Nd4j.create(new int[]{actualMiniBatchSize, VECTOR_SIZE, exampleLength}, 'f');
        INDArray label = Nd4j.create(new int[]{actualMiniBatchSize, 1, exampleLength}, 'f');
        for (int index = 0; index < actualMiniBatchSize; index++) {
            int startIdx = exampleStartOffsets.removeFirst();
            int endIdx = startIdx + exampleLength;
            Candle curData = train.get(startIdx);
            Candle nextData;
            for (int i = startIdx; i < endIdx; i++) {
                nextData = train.get(i + 1);
                int c = i - startIdx;
                input.putScalar(new int[]{index, 0, c}, normalize(curData.getOpenMid(), openMin, openMax));
                input.putScalar(new int[]{index, 1, c}, normalize(curData.getCloseMid(), closeMin, closeMax));
                input.putScalar(new int[]{index, 2, c}, normalize(curData.getLowMid(), lowMin, lowMax));
                input.putScalar(new int[]{index, 3, c}, normalize(curData.getHighMid(), highMin, highMax));
                input.putScalar(new int[]{index, 4, c}, normalize(curData.getVolume(), volumeMin, volumeMax));

                label.putScalar(new int[]{index, 0, c}, normalize(curData.getCloseMid(), closeMin, closeMax));
                curData = nextData;
            }
            if (exampleStartOffsets.size() == 0) break;
        }
        return new DataSet(input, label);
    }

    @Override
    public int totalExamples() {
        return train.size() - exampleLength - 1;
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
        return miniBatchSize;
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
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public List<String> getLabels() {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public boolean hasNext() {
        return exampleStartOffsets.size() > 0;
    }

    @Override
    public DataSet next() {
        return next(miniBatchSize);
    }

    public List<Pair<INDArray, Double>> generateTestDataSet(List<Candle> stockDataList) {
        List<Pair<INDArray, Double>> test = Lists.newArrayList();
        stockDataList.forEach(candle -> {
            INDArray input = Nd4j.create(new int[]{1, VECTOR_SIZE}, 'f');
            input.putScalar(new int[]{0, 0}, normalize(candle.getOpenMid(), openMin, openMax));
            input.putScalar(new int[]{0, 1}, normalize(candle.getCloseMid(), closeMin, closeMax));
            input.putScalar(new int[]{0, 2}, normalize(candle.getLowMid(), lowMin, lowMax));
            input.putScalar(new int[]{0, 3}, normalize(candle.getHighMid(), highMin, highMax));
            input.putScalar(new int[]{0, 4}, normalize(candle.getVolume(), volumeMin, volumeMax));

            test.add(new Pair<>(input, normalize(candle.getOpenMid(), closeMin, closeMax)));
        });

        return test;
    }

    public static double normalize(double input, double min, double max) {
        return (input - min) / (max - min) * 0.8 + 0.1;
    }

    public static double deNormalize(double input, double min, double max) {
        return min + (input - 0.1) * (max - min) / 0.8;
    }
}
