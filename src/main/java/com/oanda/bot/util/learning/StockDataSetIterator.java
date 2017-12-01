package com.oanda.bot.util.learning;

import com.oanda.bot.domain.Candle;
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

    private int miniBatchSize;
    private int exampleLength;

    private double[] minNum = new double[VECTOR_SIZE];
    private double[] maxNum = new double[VECTOR_SIZE];

    private LinkedList<Integer> exampleStartOffsets = new LinkedList<>();

    private List<Candle> train;
    private List<Pair<INDArray, Double>> test;

    public StockDataSetIterator(List<Candle> stockDataList, int miniBatchSize, double splitRatio) {
        this.miniBatchSize = miniBatchSize;
        this.exampleLength = stockDataList.size();
        int split = (int) Math.round(stockDataList.size() * splitRatio);
        train = stockDataList.subList(0, split);
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

    public List<Pair<INDArray, Double>> getTestDataSet() {
        return test;
    }

    public double[] getMaxNum() {
        return maxNum;
    }

    public double[] getMinNum() {
        return minNum;
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
                input.putScalar(new int[]{index, 0, c}, (curData.getOpenMid() - minNum[0]) / (maxNum[0] - minNum[0]));
                input.putScalar(new int[]{index, 1, c}, (curData.getCloseMid() - minNum[1]) / (maxNum[1] - minNum[1]));
                input.putScalar(new int[]{index, 2, c}, (curData.getLowMid() - minNum[2]) / (maxNum[2] - minNum[2]));
                input.putScalar(new int[]{index, 3, c}, (curData.getHighMid() - minNum[3]) / (maxNum[3] - minNum[3]));
                input.putScalar(new int[]{index, 4, c}, (curData.getVolume() - minNum[4]) / (maxNum[4] - minNum[4]));
                label.putScalar(new int[]{index, 0, c}, (nextData.getCloseMid() - minNum[1]) / (maxNum[1] - minNum[1]));
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
        int window = exampleLength + 1;
        List<Pair<INDArray, Double>> test = new ArrayList<>();
        for (int i = 0; i < stockDataList.size() - window; i++) {
            INDArray input = Nd4j.create(new int[]{exampleLength, VECTOR_SIZE}, 'f');
            for (int j = i; j < i + exampleLength; j++) {
                Candle stock = stockDataList.get(j);
                input.putScalar(new int[]{j - i, 0}, (stock.getOpenMid() - minNum[0]) / (maxNum[0] - minNum[0]));
                input.putScalar(new int[]{j - i, 1}, (stock.getCloseMid() - minNum[1]) / (maxNum[1] - minNum[1]));
                input.putScalar(new int[]{j - i, 2}, (stock.getLowMid() - minNum[2]) / (maxNum[2] - minNum[2]));
                input.putScalar(new int[]{j - i, 3}, (stock.getHighMid() - minNum[3]) / (maxNum[3] - minNum[3]));
                input.putScalar(new int[]{j - i, 4}, (stock.getVolume() - minNum[4]) / (maxNum[4] - minNum[4]));
            }
            double label = stockDataList.get(i + exampleLength).getCloseMid();
            test.add(new Pair<>(input, label));
        }
        return test;
    }
}
