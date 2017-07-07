package com.oanda.bot.strategies.indicators;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.CCIIndicator;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CCI extends CCIIndicator {

    private Decimal highMeasuredAverageLevel;
    private Decimal lowMeasuredAverageLevel;

    public CCI(TimeSeries series, int timeFrame, Decimal highMeasuredAverageLevel, Decimal lowMeasuredAverageLevel) {
        super(series, timeFrame);
        this.highMeasuredAverageLevel = highMeasuredAverageLevel;
        this.lowMeasuredAverageLevel = lowMeasuredAverageLevel;
    }

    public Decimal getChangeValue(Integer indexStart, Integer indexEnd) {
        if (indexEnd == 0 || indexEnd > indexStart) {
            return Decimal.valueOf(0);
        }
        return this.getValue(indexStart).min(this.getValue(indexEnd)).abs();
    }
}