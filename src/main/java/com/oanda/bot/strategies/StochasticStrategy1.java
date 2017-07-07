package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;

/**
 * http://es.wikipedia.org/wiki/Oscilador_estoc%C3%A1stico
 */
public class StochasticStrategy1 {

    public static Strategy buildStrategy(TimeSeries series) {

        StochasticOscillatorKIndicator sof = new StochasticOscillatorKIndicator(series, 14);
        SMAIndicator sma = new SMAIndicator(sof, 3);

        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(sof, sma);

        // Exit rule
        Rule exitRule = new CrossedDownIndicatorRule(sof, sma);

        return new Strategy(entryRule, exitRule);
    }
}