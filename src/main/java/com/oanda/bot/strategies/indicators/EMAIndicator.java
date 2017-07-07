package com.oanda.bot.strategies.indicators;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.indicators.RecursiveCachedIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;

public class EMAIndicator extends RecursiveCachedIndicator<Decimal> {

	private final Indicator<Decimal> indicator;
	private final Decimal multiplier;
	private final int timeFrame;

	public EMAIndicator(Indicator<Decimal> indicator, int timeFrame) {
		super(indicator);
		this.indicator = indicator;
		this.timeFrame = timeFrame;
		this.multiplier = Decimal.valueOf(timeFrame);
	}

	@Override
	protected Decimal calculate(int index) {
		if (index < timeFrame) {
			return new SMAIndicator(indicator, timeFrame).getValue(index);
		}

		Decimal prev = getValue(index - 1);
		return prev.multipliedBy(multiplier.minus(Decimal.ONE)).plus(indicator.getValue(index)).dividedBy(multiplier);
	}
}
