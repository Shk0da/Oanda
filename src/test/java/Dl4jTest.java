import com.google.common.collect.Lists;
import com.oanda.bot.domain.Candle;
import com.oanda.bot.util.CSVUtil;
import com.oanda.bot.util.LSTMNetwork;
import com.oanda.bot.util.StockDataSetIterator;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

@Slf4j
public class Dl4jTest {

    public static final String dataFileName = "Data.csv";
    public static final String networkFileName = "NeuralNetwork";

    public static void main(String[] args) {
        MultiLayerNetwork net;
        List<Candle> data = CSVUtil.getCandles(Dl4jTest.class.getResource(dataFileName).getFile(), 5000);
        StockDataSetIterator iterator = new StockDataSetIterator(data, 0.9);
        URL neuralNetworkFile = Dl4jTest.class.getResource(networkFileName);
        if (neuralNetworkFile != null && new File(neuralNetworkFile.getFile()).exists()) {
            try {
                net = ModelSerializer.restoreMultiLayerNetwork(neuralNetworkFile.getFile());
            } catch (IOException ex) {
                log.error(ex.getMessage());
                return;
            }
        } else {
            net = LSTMNetwork.buildLstmNetworks(iterator);
        }

        List<Pair<INDArray, Double>> testData = iterator.getTest();
        List<Double> predicts = Lists.newArrayList();
        List<Double> actuals = Lists.newArrayList();

        testData.forEach(indArrayDoublePair -> {
            INDArray output = net.rnnTimeStep(indArrayDoublePair.getKey());
            predicts.add(StockDataSetIterator.deNormalize(
                    output.getDouble(0), iterator.getCloseMin(), iterator.getCloseMax()
            ));
            actuals.add(indArrayDoublePair.getValue());
        });

        plote(actuals, predicts);
    }

    private static void plote(List<Double> actuals, List<Double> predicts) {
        final XYSeries series = new XYSeries("ACTUAL");
        final XYSeries series2 = new XYSeries("PREDICT");

        double max = 0;
        double min = 999;
        for (int i = 0; i < actuals.size(); i++) {
            if (actuals.get(i) > max) max = actuals.get(i);
            if (actuals.get(i) < min) min = actuals.get(i);
            if (predicts.get(i) > max) max = predicts.get(i);
            if (predicts.get(i) < min) min = predicts.get(i);

            series.add(i, actuals.get(i));
            series2.add(i, predicts.get(i));
        }

        final XYSeriesCollection data = new XYSeriesCollection();
        data.addSeries(series);
        data.addSeries(series2);

        final JFreeChart chart = ChartFactory.createXYLineChart(
                "Symbol",
                "Ticks",
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

        ApplicationFrame appFrane = new ApplicationFrame("Dl4jTest");
        appFrane.setContentPane(chartPanel);
        appFrane.pack();
        RefineryUtilities.centerFrameOnScreen(appFrane);
        appFrane.setVisible(true);
    }
}
