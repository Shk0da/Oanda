package com.oanda.backtesting.strategies.indicators;

import com.oanda.backtesting.strategies.enums.AverageType;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.indicators.CachedIndicator;
import eu.verdelhan.ta4j.indicators.helpers.AverageGainIndicator;
import eu.verdelhan.ta4j.indicators.helpers.AverageLossIndicator;

public class WilderAverageIndicator extends CachedIndicator<Decimal> {
	private static final String IllegalTypeMessage = "Average type should be either GAIN or LOSS";

	private final Indicator<Decimal> indicator;
	private final int timeFrame;
	private final AverageType type;

	private final Decimal multiplier;
	private final Decimal divider;

	public WilderAverageIndicator(Indicator<Decimal> indicator, int timeFrame, AverageType type) {
		super(indicator);
		this.indicator = indicator;
		this.timeFrame = timeFrame;
		this.type = type;
		this.multiplier = Decimal.valueOf(timeFrame - 1);
		this.divider = Decimal.valueOf(timeFrame);
	}

	@Override
	protected Decimal calculate(int index) {
		if (index == 0) {
			return Decimal.valueOf(0);
		}

		if (index <= timeFrame) {
			if (type == AverageType.GAIN) {
				return new AverageGainIndicator(indicator, timeFrame).getValue(index);
			} else if (type == AverageType.LOSS) {
				return new AverageLossIndicator(indicator, timeFrame).getValue(index);
			} else {
				throw new IllegalArgumentException(IllegalTypeMessage);
			}
		}

		Decimal change = indicator.getValue(index).minus(indicator.getValue(index - 1));

		Decimal gainLoss;
		if (type == AverageType.GAIN) {
			gainLoss = change;
		} else if (type == AverageType.LOSS) {
			gainLoss = change.multipliedBy(Decimal.valueOf(-1));
		} else {
			throw new IllegalArgumentException(IllegalTypeMessage);
		}

		gainLoss = gainLoss.max(Decimal.ZERO);
		Decimal prevAverage = this.getValue(index - 1);
		return prevAverage.multipliedBy(multiplier).plus(gainLoss).dividedBy(divider);
	}
}
