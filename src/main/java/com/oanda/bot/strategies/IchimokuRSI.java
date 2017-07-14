package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.RSIIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ichimoku.*;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

/**
 * from Step.M5
 */
public class IchimokuRSI {

    public static Strategy buildStrategy(TimeSeries series) {

        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        /*Ichimoku Cloud*/
        IchimokuTenkanSenIndicator tenkanSenIndicator = new IchimokuTenkanSenIndicator(series);
        IchimokuKijunSenIndicator kijunSenIndicator = new IchimokuKijunSenIndicator(series);
        IchimokuSenkouSpanAIndicator senkouSpanAIndicator = new IchimokuSenkouSpanAIndicator(series, tenkanSenIndicator, kijunSenIndicator);
        IchimokuSenkouSpanBIndicator senkouSpanBIndicator = new IchimokuSenkouSpanBIndicator(series);
        IchimokuChikouSpanIndicator chikouSpanIndicator = new IchimokuChikouSpanIndicator(series);

        Rule entryRule = (new CrossedUpIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                .or(new CrossedUpIndicatorRule(closePrice, kijunSenIndicator))
                .or(new CrossedUpIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                .or(new OverIndicatorRule(chikouSpanIndicator, closePrice)))
                .and(new CrossedDownIndicatorRule(rsi, Decimal.valueOf(5)));

        Rule exitRule = (new CrossedDownIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                .or(new CrossedDownIndicatorRule(closePrice, kijunSenIndicator))
                .or(new CrossedDownIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                .or(new UnderIndicatorRule(chikouSpanIndicator, closePrice)))
                .and(new CrossedUpIndicatorRule(rsi, Decimal.valueOf(95)));

        return new Strategy(entryRule, exitRule);
    }
}
