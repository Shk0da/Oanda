package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;

/**
 * http://es.wikipedia.org/wiki/MACD
 */
public class MACDStrategy {

    private String name = "MACDStrategy";

    private Double profit;

    public static Strategy buildStrategy(TimeSeries series) {

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
        EMAIndicator emaMacd = new EMAIndicator(macd, 9);

        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(macd, emaMacd);

        // Exit rule
        Rule exitRule = new CrossedDownIndicatorRule(macd, emaMacd);

        return new Strategy(entryRule, exitRule);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}