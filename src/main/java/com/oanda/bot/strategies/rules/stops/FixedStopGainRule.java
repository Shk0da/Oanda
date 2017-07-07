package com.oanda.bot.strategies.rules.stops;

import com.oanda.bot.strategies.enums.StopType;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;

public class FixedStopGainRule extends AbstractStopRule {

	public FixedStopGainRule(ClosePriceIndicator closePrice, Decimal maxGain, StopType stopType) {
		super(closePrice, maxGain, stopType);
	}

	@Override
	protected Decimal getResult(Order entry, Tick tick) {
		Decimal entryPrice = entry.getPrice();

		Decimal gain;
		switch (entry.getType()) {
		case BUY:
			Decimal highPrice = tick.getMaxPrice();
			gain = highPrice.minus(entryPrice);
			break;
		case SELL:
			Decimal lowPrice = tick.getMinPrice();
			gain = entryPrice.minus(lowPrice);
			break;
		default:
			throw new IllegalArgumentException("Entry order must be either a buying order or selling order.");
		}

		if (this.stopType == StopType.PERCENTAGE) {
			gain = gain.multipliedBy(Decimal.HUNDRED).dividedBy(entryPrice);
		}

		return gain;
	}

	@Override
	protected Decimal getExitPrice(Order entry) {
		Decimal entryPrice = entry.getPrice();
		Decimal profit = this.resultLimit;
		if (this.stopType == StopType.PERCENTAGE) {
			profit = profit.multipliedBy(entryPrice).dividedBy(Decimal.HUNDRED);
		}

		switch (entry.getType()) {
		case BUY:
			return entryPrice.plus(profit);
		case SELL:
			return entryPrice.minus(profit);
		default:
			throw new IllegalArgumentException("Entry order must be either a buying order or selling order.");
		}
	}

}
