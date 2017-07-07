package com.oanda.bot.strategies.rules.stops;

import com.oanda.bot.strategies.enums.StopType;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;

public class TrailingStopRule extends AbstractStopRule {

	private final Decimal trigger;

	private Decimal maxGainPrice;

	public TrailingStopRule(ClosePriceIndicator closePrice, Decimal trigger, Decimal distance, StopType stopType) {
		super(closePrice, distance, stopType);
		this.trigger = trigger;
		this.maxGainPrice = Decimal.NaN;
	}

	private Decimal getCurrentGain(Order entry, Decimal currentPrice) {
		switch (entry.getType()) {
		case BUY:
			return currentPrice.minus(entry.getPrice());
		case SELL:
			return entry.getPrice().minus(currentPrice);
		default:
			throw new IllegalArgumentException("Entry order must be either a buying order or selling order.");
		}
	}

	@Override
	protected Decimal getResult(Order entry, Tick tick) {
		Decimal currentPrice = tick.getClosePrice();
		Decimal currentGain = this.getCurrentGain(entry, currentPrice);
		if (this.stopType == StopType.PERCENTAGE) {
			currentGain = currentGain.multipliedBy(Decimal.HUNDRED).dividedBy(entry.getPrice());
		}

		if (maxGainPrice.isNaN() && currentGain.isGreaterThanOrEqual(trigger)) {
			maxGainPrice = currentPrice;
		} else if (maxGainPrice.isNaN()) {
			return Decimal.ZERO;
		}

		Decimal loss;
		switch (entry.getType()) {
		case BUY:
			maxGainPrice = maxGainPrice.max(currentPrice);
			loss = maxGainPrice.minus(currentPrice);
			break;
		case SELL:
			maxGainPrice = maxGainPrice.min(currentPrice);
			loss = currentPrice.minus(maxGainPrice);
			break;
		default:
			throw new IllegalArgumentException("Entry order must be either a buying order or selling order.");
		}

		if (this.stopType == StopType.PERCENTAGE) {
			loss = loss.multipliedBy(Decimal.HUNDRED).dividedBy(maxGainPrice);
		}

		return loss;
	}

	@Override
	protected Decimal getExitPrice(Order entry) {
		Decimal loss = this.resultLimit;
		if (this.stopType == StopType.PERCENTAGE) {
			loss = loss.multipliedBy(maxGainPrice).dividedBy(Decimal.HUNDRED);
		}

		switch (entry.getType()) {
		case BUY:
			return maxGainPrice.minus(loss);
		case SELL:
			return maxGainPrice.plus(loss);
		default:
			throw new IllegalArgumentException("Entry order must be either a buying order or selling order.");
		}
	}

	public void startNewTrade() {
		this.maxGainPrice = Decimal.NaN;
	}

}
