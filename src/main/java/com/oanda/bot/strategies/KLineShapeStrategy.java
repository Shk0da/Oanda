package com.oanda.bot.strategies;

import com.oanda.bot.strategies.indicators.candles.BottomIndicator;
import com.oanda.bot.strategies.indicators.candles.TopIndicator;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.trading.rules.BooleanIndicatorRule;

/**
 * 基于K线形态分析的交易策略
 * 
 * @author meixinbin
 * @2016-6-29
 */
public class KLineShapeStrategy {

	public static Strategy buildStrategy(TimeSeries series) {
		if (series == null) {
			throw new IllegalArgumentException("Series cannot be null");
		}
		
		//底部形态指标
		BottomIndicator bi = new BottomIndicator(series,30);
		
		TopIndicator ti = new TopIndicator(series, 30);
		//底部形态已经形成，买进
		Rule entryRule1 = new BooleanIndicatorRule(bi);
		Rule entryRule = entryRule1;
		Rule exitRule = new BooleanIndicatorRule(ti);
		return new Strategy(entryRule, exitRule);
	}
}
