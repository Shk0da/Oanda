package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.trackers.WilliamsRIndicator;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class WilliamsStrategy {

    public static Strategy buildStrategy(TimeSeries series) {

        WilliamsRIndicator williamsRIndicator = new WilliamsRIndicator(series, 14);

        // Entry rule
        Rule entryRule = new OverIndicatorRule(williamsRIndicator, Decimal.valueOf(-80));

        // Exit rule
        Rule exitRule = new UnderIndicatorRule(williamsRIndicator, Decimal.valueOf(-20));

        return new Strategy(entryRule, exitRule);
    }
}