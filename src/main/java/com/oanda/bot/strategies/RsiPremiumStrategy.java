package com.oanda.bot.strategies;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.oanda.bot.strategies.indicators.CCI;
import com.oanda.bot.strategies.indicators.RSI;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollinger.BollingerBandsLowerIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollinger.BollingerBandsMiddleIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollinger.BollingerBandsUpperIndicator;
import lombok.Data;
import lombok.NonNull;

@Data
public class RsiPremiumStrategy {

    @NonNull
    private RSI rsi;
    @NonNull
    private CCI cci;
    @NonNull
    private BollingerBandsUpperIndicator bbUpper;
    @NonNull
    private BollingerBandsLowerIndicator bbLower;
    private BollingerBandsMiddleIndicator bbMiddle;
    private MACDIndicator macd;

    public Map<String, List<Object>> createIndicatorValuesMap(int predictionTimeMinutes) {
        Map<String, List<Object>> indicatorsValuesMap = new LinkedHashMap<>();
        List<Object> priceCloseValue = new LinkedList<>();
        List<Object> rsiValues = new LinkedList<>();
        List<Object> cciValues = new LinkedList<>();
        List<Object> bbuValues = new LinkedList<>();
        List<Object> bbmValues = new LinkedList<>();
        List<Object> bblValues = new LinkedList<>();
        List<Object> macdValues = new LinkedList<>();
        List<Object> predictionVal = new LinkedList<>();

        TimeSeries ts = rsi.getTimeSeries();
        for (int i = 0; i < ts.getEnd() - predictionTimeMinutes; i++) {
            priceCloseValue.add(ts.getTick(i).getClosePrice());
            rsiValues.add(rsi.getValue(i));
            cciValues.add(cci.getValue(i));
            bbuValues.add(bbUpper.getValue(i));
            bbmValues.add(bbMiddle.getValue(i));
            bblValues.add(bbLower.getValue(i));
            macdValues.add(macd.getValue(i));
            if (ts.getTick(i + predictionTimeMinutes).getClosePrice().isGreaterThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("up");
            } else if (ts.getTick(i + predictionTimeMinutes).getClosePrice()
                    .isLessThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("down");
            } else {
                predictionVal.add("no");
            }
        }

        indicatorsValuesMap.put("CloseValue", priceCloseValue);
        indicatorsValuesMap.put(rsi.getClass().getSimpleName(), rsiValues);
        indicatorsValuesMap.put(cci.getClass().getSimpleName(), cciValues);
        indicatorsValuesMap.put(bbUpper.getClass().getSimpleName(), bbuValues);
        indicatorsValuesMap.put(bbMiddle.getClass().getSimpleName(), bbmValues);
        indicatorsValuesMap.put(bbLower.getClass().getSimpleName(), bblValues);
        indicatorsValuesMap.put(macd.getClass().getSimpleName(), macdValues);
        indicatorsValuesMap.put("Prediction", predictionVal);
        return indicatorsValuesMap;
    }

    public Map<String, List<Object>> createMixedindicatorsMap(int predictionTimeMinutes) {
        Map<String, List<Object>> indicatorsValuesMap = new LinkedHashMap<>();
        List<Object> priceCloseValue = new LinkedList<>();
        List<Object> rsiValues = new LinkedList<>();
        List<Object> rsiOverBoughtOrSoldSignalList = new LinkedList<>();
        List<Object> cciValues = new LinkedList<>();
        List<Object> cciChannelBreakSignalList = new LinkedList<>();
        List<Object> bbuValues = new LinkedList<>();
        List<Object> bbmValues = new LinkedList<>();
        List<Object> bblValues = new LinkedList<>();
        List<Object> bbBreakSignalList = new LinkedList<>();
        List<Object> macdValues = new LinkedList<>();
        List<Object> predictionVal = new LinkedList<>();

        TimeSeries ts = rsi.getTimeSeries();
        for (int i = 0; i < ts.getEnd() - predictionTimeMinutes; i++) {
            priceCloseValue.add(ts.getTick(i).getClosePrice());
            rsiValues.add(rsi.getValue(i));
            cciValues.add(cci.getValue(i));
            bbuValues.add(bbUpper.getValue(i));
            bbmValues.add(bbMiddle.getValue(i));
            bblValues.add(bbLower.getValue(i));
            macdValues.add(macd.getValue(i));
            if (ts.getTick(i + predictionTimeMinutes).getClosePrice().isGreaterThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("up");
            } else if (ts.getTick(i + predictionTimeMinutes).getClosePrice()
                    .isLessThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("down");
            } else {
                predictionVal.add("no");
            }

            rsiOverBoughtOrSoldSignalList.add(makeRsiSignal(rsi.getValue(i)).toString());
            cciChannelBreakSignalList.add(makeCCiSignal(cci.getValue(i)).toString());
            bbBreakSignalList.add(
                    makeBbSignal(bbUpper.getValue(i), bbLower.getValue(i), ts.getTick(i).getClosePrice()).toString());
        }

        indicatorsValuesMap.put("CloseValue", priceCloseValue);
        indicatorsValuesMap.put(rsi.getClass().getSimpleName(), rsiValues);
        indicatorsValuesMap.put(cci.getClass().getSimpleName(), cciValues);
        indicatorsValuesMap.put(bbUpper.getClass().getSimpleName(), bbuValues);
        indicatorsValuesMap.put(bbMiddle.getClass().getSimpleName(), bbmValues);
        indicatorsValuesMap.put(bbLower.getClass().getSimpleName(), bblValues);
        indicatorsValuesMap.put("RSI OVERBOUGHT OR SOLD", rsiOverBoughtOrSoldSignalList);
        indicatorsValuesMap.put("CCI CHANNEL BREAK", cciChannelBreakSignalList);
        indicatorsValuesMap.put("BB BREAK", bbBreakSignalList);
        indicatorsValuesMap.put(macd.getClass().getSimpleName(), macdValues);
        indicatorsValuesMap.put("Prediction", predictionVal);
        return indicatorsValuesMap;
    }

    public Map<String, List<Object>> createIndicatorsSignalsMap(int predictionTimeMinutes) {
        Map<String, List<Object>> indicatorsValuesMap = new LinkedHashMap<>();
        List<Object> priceCloseValue = new LinkedList<>();
        List<Object> rsiValues = new LinkedList<>();
        List<Object> cciValues = new LinkedList<>();
        List<Object> bbValues = new LinkedList<>();
        List<Object> macdValues = new LinkedList<>();
        List<Object> predictionVal = new LinkedList<>();

        TimeSeries ts = rsi.getTimeSeries();
        for (int i = 0; i < ts.getEnd() - predictionTimeMinutes; i++) {
            priceCloseValue.add(ts.getTick(i).getClosePrice().toString());
            rsiValues.add(makeRsiSignal(rsi.getValue(i)).toString());
            cciValues.add(makeCCiSignal(cci.getValue(i)).toString());
            bbValues.add(
                    makeBbSignal(bbUpper.getValue(i), bbLower.getValue(i), ts.getTick(i).getClosePrice()).toString());
            macdValues.add(macdTrendSignal(macd.getValue(i)));
            if (ts.getTick(i + predictionTimeMinutes).getClosePrice().isGreaterThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("up");
            } else if (ts.getTick(i + predictionTimeMinutes).getClosePrice()
                    .isLessThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("down");
            } else {
                predictionVal.add("no");
            }
        }

        indicatorsValuesMap.put("CloseValue", priceCloseValue);
        indicatorsValuesMap.put(rsi.getClass().getSimpleName(), rsiValues);
        indicatorsValuesMap.put(cci.getClass().getSimpleName(), cciValues);
        indicatorsValuesMap.put(bbUpper.getClass().getSimpleName(), bbValues);
        indicatorsValuesMap.put(macd.getClass().getSimpleName(), macdValues);
        indicatorsValuesMap.put("Prediction", predictionVal);
        return indicatorsValuesMap;
    }

    public Map<String, List<Object>> createIndicatorsScaledSignalsMap(int predictionTimeMinutes) {
        Map<String, List<Object>> indicatorsValuesMap = new LinkedHashMap<>();
        List<Object> priceCloseValue = new LinkedList<>();
        List<Object> rsiValues = new LinkedList<>();
        List<Object> cciValues = new LinkedList<>();
        List<Object> bbValues = new LinkedList<>();
        List<Object> macdValues = new LinkedList<>();
        List<Object> predictionVal = new LinkedList<>();

        TimeSeries ts = rsi.getTimeSeries();
        for (int i = 0; i < ts.getEnd() - predictionTimeMinutes; i++) {
            priceCloseValue.add(ts.getTick(i).getClosePrice().toString());
            rsiValues.add(scaleRsiValue(rsi.getValue(i)).toString());
            cciValues.add(scaleCCiValue(cci.getValue(i)).toString());
            bbValues.add(
                    scaleBBSignal(bbUpper.getValue(i), bbLower.getValue(i), ts.getTick(i).getClosePrice()).toString());
            macdValues.add(scaleMacdValues(macd.getValue(i)));
            if (ts.getTick(i + predictionTimeMinutes).getClosePrice().isGreaterThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("up");
            } else if (ts.getTick(i + predictionTimeMinutes).getClosePrice()
                    .isLessThan(ts.getTick(i).getClosePrice())) {
                predictionVal.add("down");
            } else {
                predictionVal.add("no");
            }
        }

        indicatorsValuesMap.put("CloseValue", priceCloseValue);
        indicatorsValuesMap.put(rsi.getClass().getSimpleName(), rsiValues);
        indicatorsValuesMap.put(cci.getClass().getSimpleName(), cciValues);
        indicatorsValuesMap.put(bbUpper.getClass().getSimpleName(), bbValues);
        indicatorsValuesMap.put(macd.getClass().getSimpleName(), macdValues);
        indicatorsValuesMap.put("Prediction", predictionVal);
        return indicatorsValuesMap;
    }

    public Map<String, List<Object>> createEqualDecisionNumberList(int predictionTimeMinutes) {
        Map<String, List<Object>> indicatorsValuesMap = createIndicatorsSignalsMap(predictionTimeMinutes);
        for (int i = 0; i <= indicatorsValuesMap.get("CloseValue").size() - 1; i++) {
            // if(indicatorsValuesMap.get)
        }

        return indicatorsValuesMap;
    }

    private Integer makeRsiSignal(Decimal rsiValue) {
        if (rsi.getOverBoughtLevel().isGreaterThanOrEqual(rsiValue)) {
            return 1;
        } else if (rsi.getOverSoldLevel().isLessThanOrEqual(rsiValue)) {
            return -1;
        } else {
            return 0;
        }
    }

    private Integer makeCCiSignal(Decimal rsiValue) {
        if (cci.getHighMeasuredAverageLevel().isGreaterThanOrEqual(rsiValue)) {
            return 1;
        } else if (cci.getLowMeasuredAverageLevel().isLessThanOrEqual(rsiValue)) {
            return -1;
        } else {
            return 0;
        }
    }

    private Integer makeBbSignal(Decimal bbuValue, Decimal bblValue, Decimal priceValue) {
        if (priceValue.isGreaterThanOrEqual(bbuValue)) {
            return 1;
        } else if (priceValue.isLessThanOrEqual(bblValue)) {
            return -1;
        } else {
            return 0;
        }
    }

    private Integer macdTrendSignal(Decimal macdVal) {
        if (macdVal.isGreaterThan(Decimal.valueOf(new Double(0.00005)))) {
            return 1;
        } else if (macdVal.isLessThan(Decimal.valueOf(new Double(-0.00005)))) {
            return -1;
        } else {
            return 0;
        }
    }

    private Integer scaleRsiValue(Decimal rsiValue) {
        if (rsiValue.isLessThanOrEqual(Decimal.valueOf("20"))) {
            return 1;
        } else if (rsiValue.isLessThanOrEqual(Decimal.valueOf("40")) && rsiValue.isGreaterThan(Decimal.valueOf("20"))) {
            return 2;
        } else if (rsiValue.isLessThanOrEqual(Decimal.valueOf("60")) && rsiValue.isGreaterThan(Decimal.valueOf("40"))) {
            return 3;
        } else if (rsiValue.isLessThanOrEqual(Decimal.valueOf("80")) && rsiValue.isGreaterThan(Decimal.valueOf("60"))) {
            return 4;
        } else if (rsiValue.isLessThanOrEqual(Decimal.valueOf("100"))
                && rsiValue.isGreaterThan(Decimal.valueOf("80"))) {
            return 5;
        }
        return 0;
    }

    private Integer scaleCCiValue(Decimal rsiValue) { // 5>300 4>200 3>150 2>100
        // 1>50 50>0>-50
        if (rsiValue.isGreaterThanOrEqual(Decimal.valueOf(300))) {
            return 5;
        } else if (rsiValue.isGreaterThanOrEqual(Decimal.valueOf(200)) && rsiValue.isLessThan(Decimal.valueOf(300))) {
            return 4;
        } else if (rsiValue.isGreaterThanOrEqual(Decimal.valueOf(150)) && rsiValue.isLessThan(Decimal.valueOf(200))) {
            return 3;
        } else if (rsiValue.isGreaterThanOrEqual(Decimal.valueOf(100)) && rsiValue.isLessThan(Decimal.valueOf(150))) {
            return 2;
        } else if (rsiValue.isGreaterThanOrEqual(Decimal.valueOf(50)) && rsiValue.isLessThan(Decimal.valueOf(100))) {
            return 1;
        } else if (rsiValue.isGreaterThan(Decimal.valueOf(-100)) && rsiValue.isLessThanOrEqual(Decimal.valueOf(-50))) {
            return -1;
        } else if (rsiValue.isGreaterThan(Decimal.valueOf(-150)) && rsiValue.isLessThanOrEqual(Decimal.valueOf(-100))) {
            return -2;
        } else if (rsiValue.isGreaterThan(Decimal.valueOf(-200)) && rsiValue.isLessThanOrEqual(Decimal.valueOf(-150))) {
            return -3;
        } else if (rsiValue.isGreaterThan(Decimal.valueOf(-300)) && rsiValue.isLessThanOrEqual(Decimal.valueOf(-200))) {
            return -4;
        } else if (rsiValue.isLessThanOrEqual(Decimal.valueOf(-300))) {
            return -5;
        } else {
            return 0;
        }
    }

    private Integer scaleBBSignal(Decimal bbuValue, Decimal bblValue, Decimal priceValue) {
        if (priceValue.minus(bbuValue).isGreaterThan(Decimal.valueOf(0.0005))) {
            return 2;
        } else if (priceValue.minus(bbuValue).isGreaterThan(Decimal.valueOf(0.0001))
                && priceValue.minus(bbuValue).isLessThan(Decimal.valueOf(0.0005))) {
            return 1;
        } else if (priceValue.minus(bblValue).isLessThanOrEqual(Decimal.valueOf(-0.0001))
                && priceValue.minus(bblValue).isGreaterThan(Decimal.valueOf(-0.0005))) {
            return -1;
        } else if (priceValue.minus(bblValue).isLessThanOrEqual(Decimal.valueOf(-0.0005))) {
            return -2;
        } else {
            return 0;
        }
    }

    private Integer scaleMacdValues(Decimal macdVal) { // 3>0.0001 2>0.000075
        // 1>0.00005 0
        if (macdVal.isGreaterThanOrEqual(Decimal.valueOf(0.0001))) {
            return 3;
        } else if (macdVal.isGreaterThanOrEqual(Decimal.valueOf(0.000075))
                && macdVal.isLessThan(Decimal.valueOf(0.0001))) {
            return 2;
        } else if (macdVal.isGreaterThanOrEqual(Decimal.valueOf(0.00005))
                && macdVal.isLessThan(Decimal.valueOf(0.000075))) {
            return 1;
        } else if (macdVal.isGreaterThan(Decimal.valueOf(-0.000075))
                && macdVal.isLessThanOrEqual(Decimal.valueOf(-0.00005))) {
            return -1;
        } else if (macdVal.isGreaterThan(Decimal.valueOf(-0.0001))
                && macdVal.isLessThanOrEqual(Decimal.valueOf(-0.000075))) {
            return -2;
        } else if (macdVal.isLessThanOrEqual(Decimal.valueOf(-0.0001))) {
            return -3;
        } else {
            return 0;
        }
    }
}
