package com.oanda.bot.strategies.rules;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.trading.rules.AbstractRule;

import java.time.LocalTime;

public class AllowOpenRule extends AbstractRule {

	private final TimeSeries series;
	private final LocalTime initialEntryTime;
	private final LocalTime finalEntryTime;

	public AllowOpenRule(TimeSeries series, LocalTime initialEntryTime, LocalTime finalEntryTime) {
		this.series = series;
		this.initialEntryTime = initialEntryTime;
		this.finalEntryTime = finalEntryTime;
	}

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean validInitialTime = true;
		boolean validFinalTime = true;

		Tick tick = series.getTick(index);
		if (initialEntryTime != null) {
			validInitialTime = tick.getEndTime().getSecondOfDay() >= initialEntryTime.toSecondOfDay();
		}
		if (finalEntryTime != null) {
			validFinalTime = tick.getEndTime().getSecondOfDay() < finalEntryTime.toSecondOfDay();
		}

		return validInitialTime && validFinalTime;
	}

}
