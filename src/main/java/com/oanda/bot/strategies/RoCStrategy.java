package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;

/**
 * http://es.wikipedia.org/wiki/Momentum_%28an%C3%A1lisis_t%C3%A9cnico%29
 */
public class RoCStrategy {

    public static Strategy buildStrategy(TimeSeries series) {

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        ROCIndicator rocIndicator = new ROCIndicator(closePrice, 12);

        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(rocIndicator, Decimal.valueOf(0));

        // Exit rule
        Rule exitRule = new CrossedDownIndicatorRule(rocIndicator, Decimal.valueOf(0));

        return new Strategy(entryRule, exitRule);
    }
}