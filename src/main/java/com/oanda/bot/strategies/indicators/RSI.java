package com.oanda.bot.strategies.indicators;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import lombok.Data;

@Data
public class RSI extends RSIIndicator {
    private Decimal overSoldLevel;
    private Decimal overBoughtLevel;

    public RSI(Decimal overSoldLevel, Decimal overBoughtLevel, Indicator<Decimal> indicator, int timeFrame) {
        super(indicator, timeFrame);
        this.overSoldLevel = overSoldLevel;
        this.overBoughtLevel = overBoughtLevel;
    }

    public Decimal getChangeValue(Integer indexStart, Integer indexEnd) {
        if (indexEnd == 0 || indexEnd > indexStart) {
            return Decimal.valueOf(0);
        }
        return this.getValue(indexStart).min(this.getValue(indexEnd)).abs();
    }
}