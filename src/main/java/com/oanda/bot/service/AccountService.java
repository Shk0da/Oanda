package com.oanda.bot.service;

import com.oanda.bot.constants.Step;
import com.oanda.bot.model.*;
import com.oanda.bot.model.Candle.Candles;
import com.oanda.bot.model.Trade.Trades;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.joda.time.DateTime;

import java.util.List;

public interface AccountService {

    Config config = ConfigFactory.load().getConfig("account");
    String provider = config.getString("provider");

	void closeOrdersAndTrades(Instrument instrument);

	Order createOrder(Order order);

	Accounts getAccountDetails();

	Candles getCandles(Step step, DateTime start, Instrument instrument);

	Candles getCandles(Step step, DateTime start, DateTime end, Instrument instrument);

	Instrument getInstrument(String pair);

	Order.Orders getOrders(Instrument instrument);

	Pivot getPivot(Instrument instrument);
	
	Price getPrice(Instrument instrument);

	Trades getTrades(Instrument instrument);

	Order updateOrder(Order order);

	Trade updateTrade(Trade trade);
	
	List<CalendarEvent> getCalendarEvents(Instrument instrument, int futureHoursCount);

}