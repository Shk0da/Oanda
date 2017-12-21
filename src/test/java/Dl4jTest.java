import com.google.common.collect.Lists;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.util.LSTMNetwork;
import com.oanda.bot.util.StockDataSetIterator;
import com.opencsv.CSVReader;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.primitives.Pair;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public class Dl4jTest {

    public static void main(String[] args) {

        int batchSize = 256; // mini-batch size
        double splitRatio = 0.5;

        StockDataSetIterator iterator = new StockDataSetIterator(getData(), batchSize, splitRatio);
        MultiLayerNetwork net = LSTMNetwork.buildLstmNetworks(iterator);

        List<Pair<INDArray, Double>> testDataSet = iterator.getTest();
        int steps = testDataSet.size() - 1;
        List<Double> predicts = Lists.newArrayList();

        for (int i = 0; i < steps; i++) {
            INDArray input = testDataSet.get(i).getKey();
            INDArray output = net.rnnTimeStep(input);
            double predict = StockDataSetIterator.deNormalize(output.getDouble(0), iterator.getCloseMin(), iterator.getCloseMax());
            predicts.add(predict);
        }

        List<Double> actuals = Lists.newArrayList();
        getData().forEach(candle -> actuals.add(candle.getCloseMid()));

        plote(actuals, predicts);
    }

    private static void plote(List<Double> actuals, List<Double> predicts) {
        final XYSeries series = new XYSeries("USD/SEK");
        final XYSeries series2 = new XYSeries("USD/SEK PREDICT");
        int bar = 1;
        double max = 0;
        double min = 999;
        for (double close : actuals) {
            if (bar++ == 1) continue;
            if (close > max) max = close;
            if (close < min) min = close;
            series.add(bar, close);
        }

        int shift = 1;
        bar = actuals.size() - predicts.size() * shift + 5;
        for (double close : predicts) {
            if (close > max) max = close;
            if (close < min) min = close;
            series2.add(bar, close);
            bar = bar + shift;
        }

        final XYSeriesCollection data = new XYSeriesCollection();

        data.addSeries(series);
        data.addSeries(series2);

        final JFreeChart chart = ChartFactory.createXYLineChart(
                "USD/SEK",
                "Tick M5",
                "ClosePrice",
                data,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        NumberAxis range = (NumberAxis) ((XYPlot) chart.getPlot()).getRangeAxis();
        range.setRange(min, max);

        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1024, 600));

        ApplicationFrame appFrane = new ApplicationFrame("USD/SEK");
        appFrane.setContentPane(chartPanel);
        appFrane.pack();
        RefineryUtilities.centerFrameOnScreen(appFrane);
        appFrane.setVisible(true);
    }

    private static List<Candle> getData() {
        List<Candle> data = Lists.newArrayList();
        String csvFile = Dl4jTest.class.getResource("data.csv").getFile();
        try {
            CSVReader reader = new CSVReader(new FileReader(csvFile));
            String[] line;
            while ((line = reader.readNext()) != null) {
                Candle candle = new Candle();
                candle.setOpenMid(Double.valueOf(line[1]));
                candle.setHighMid(Double.valueOf(line[2]));
                candle.setLowMid(Double.valueOf(line[3]));
                candle.setCloseMid(Double.valueOf(line[4]));
                candle.setVolume(Double.valueOf(line[5]).intValue());
                data.add(candle);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return data.subList(0, 5000);
    }
}
