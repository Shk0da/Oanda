package com.oanda.bot.strategies;

import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ichimoku.*;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;

public class IchimokuCloudTradingStrategy {

    private static Rule buyRule;
    private static Rule sellRule;

    /**
     * @param series a time series
     * @return Ichimoku Cloud Trading Strategy
     */
    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        IchimokuTenkanSenIndicator tenkanSenIndicator = new IchimokuTenkanSenIndicator(series);
        IchimokuKijunSenIndicator kijunSenIndicator = new IchimokuKijunSenIndicator(series);
        IchimokuSenkouSpanAIndicator senkouSpanAIndicator = new IchimokuSenkouSpanAIndicator(series, tenkanSenIndicator, kijunSenIndicator);
        IchimokuSenkouSpanBIndicator senkouSpanBIndicator = new IchimokuSenkouSpanBIndicator(series);
        IchimokuChikouSpanIndicator chikouSpanIndicator = new IchimokuChikouSpanIndicator(series);

        buyRule = new CrossedUpIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                .or(new CrossedUpIndicatorRule(closePrice, kijunSenIndicator))
                .or(new CrossedUpIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                .or(new OverIndicatorRule(chikouSpanIndicator, closePrice));

        sellRule = new CrossedDownIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                .or(new CrossedDownIndicatorRule(closePrice, kijunSenIndicator))
                .or(new CrossedDownIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                .or(new UnderIndicatorRule(chikouSpanIndicator, closePrice));

        return new Strategy(buyRule, sellRule);
    }

    public static Rule getBuyRule() {
        return buyRule;
    }

    public static Rule getSellRule() {
        return sellRule;
    }
}