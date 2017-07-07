package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorDIndicator;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;

/**
 * http://ciberconta.unizar.es/leccion/fin005/570.HTM
 */
public class StochasticStrategy2 {

    public static Strategy buildStrategy(TimeSeries series) {

        StochasticOscillatorKIndicator sof = new StochasticOscillatorKIndicator(series, 14);
        StochasticOscillatorDIndicator sos = new StochasticOscillatorDIndicator(sof);

        // Entry rule
        Rule entryRule = new CrossedUpIndicatorRule(sof, sos);

        // Exit rule
        Rule exitRule = new CrossedDownIndicatorRule(sof, sos);

        return new Strategy(entryRule, exitRule);
    }
}