package com.oanda.bot.strategies.rules;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.trading.rules.AbstractRule;

import java.time.LocalTime;

public class ForceCloseRule extends AbstractRule {

	private final TimeSeries series;
	private final LocalTime exitTime;

	public ForceCloseRule(TimeSeries series, LocalTime exitTime) {
		this.series = series;
		this.exitTime = exitTime;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean forceClosePosition = false;

		Tick tick = series.getTick(index);
		if (exitTime != null)
			forceClosePosition = tick.getEndTime().getSecondOfDay() >= exitTime.toSecondOfDay();

		return forceClosePosition;
	}

}
