package com.oanda.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TradeStateChange {
	private StateChange change;
	private TradingState state;
	private Candle candle;
}