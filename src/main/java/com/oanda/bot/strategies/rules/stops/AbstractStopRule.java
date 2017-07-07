package com.oanda.bot.strategies.rules.stops;

import com.oanda.bot.strategies.enums.StopType;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.AbstractRule;

public abstract class AbstractStopRule extends AbstractRule {

	protected final Decimal resultLimit;
	protected final StopType stopType;

	private ClosePriceIndicator closePrice;

	public AbstractStopRule(ClosePriceIndicator closePrice, Decimal resultLimit, StopType stopType) {
		this.closePrice = closePrice;
		this.resultLimit = resultLimit;
		this.stopType = stopType;
	}

	protected abstract Decimal getResult(Order entry, Tick tick);

	protected abstract Decimal getExitPrice(Order entry);

	@Override
	public boolean isSatisfied(int index, TradingRecord tradingRecord) {
		boolean satisfied = false;

		Order entry = getEntryOrder(tradingRecord);
		if (entry != null) {
			Tick tick = closePrice.getTimeSeries().getTick(index);
			Decimal result = this.getResult(entry, tick);

			satisfied = result.isGreaterThanOrEqual(this.resultLimit);
		}

		return satisfied;
	}

	public Decimal getExitPrice(TradingRecord tradingRecord) {
		Order entry = this.getEntryOrder(tradingRecord);
		if (entry == null) {
			throw new IllegalArgumentException("Trading record must hold a opened trade");
		}

		return this.getExitPrice(entry);
	}

	private Order getEntryOrder(TradingRecord tradingRecord) {
		if (tradingRecord != null) {
			Trade currentTrade = tradingRecord.getCurrentTrade();

			if (currentTrade.isOpened()) {
				return currentTrade.getEntry();
			}
		}

		return null;
	}
}
