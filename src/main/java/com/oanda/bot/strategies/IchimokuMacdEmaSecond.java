package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ichimoku.*;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

/**
 * from Step.M15
 */
public class IchimokuMacdEmaSecond {

    public static Strategy buildStrategy(TimeSeries series) {

        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        /*MACD + EMA*/
        MACDIndicator macd = new MACDIndicator(closePrice, 9, 26);
        EMAIndicator shortEma = new EMAIndicator(closePrice, 9);
        EMAIndicator longEma = new EMAIndicator(closePrice, 26);
        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);
        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

        /*Ichimoku Cloud*/
        IchimokuTenkanSenIndicator tenkanSenIndicator = new IchimokuTenkanSenIndicator(series);
        IchimokuKijunSenIndicator kijunSenIndicator = new IchimokuKijunSenIndicator(series);
        IchimokuSenkouSpanAIndicator senkouSpanAIndicator = new IchimokuSenkouSpanAIndicator(series, tenkanSenIndicator, kijunSenIndicator);
        IchimokuSenkouSpanBIndicator senkouSpanBIndicator = new IchimokuSenkouSpanBIndicator(series);
        IchimokuChikouSpanIndicator chikouSpanIndicator = new IchimokuChikouSpanIndicator(series);

        Rule entryRule = ((new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillK, Decimal.valueOf(20))) // Signal 1
                .and(new OverIndicatorRule(macd, emaMacd)))
                .and(new CrossedDownIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                        .or(new CrossedDownIndicatorRule(closePrice, kijunSenIndicator))
                        .or(new CrossedDownIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                        .or(new UnderIndicatorRule(chikouSpanIndicator, closePrice))));

        Rule exitRule = ((new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillK, Decimal.valueOf(80))) // Signal 1
                .and(new UnderIndicatorRule(macd, emaMacd)))
                .and(new CrossedDownIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                        .or(new CrossedDownIndicatorRule(closePrice, kijunSenIndicator))
                        .or(new CrossedDownIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                        .or(new UnderIndicatorRule(chikouSpanIndicator, closePrice))));

        return new Strategy(entryRule, exitRule);
    }
}
