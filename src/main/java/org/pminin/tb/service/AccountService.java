package org.pminin.tb.service;

import java.util.List;

import org.joda.time.DateTime;
import org.pminin.tb.constants.Step;
import org.pminin.tb.model.Accounts;
import org.pminin.tb.model.CalendarEvent;
import org.pminin.tb.model.Candle.Candles;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Order;
import org.pminin.tb.model.Order.Orders;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.model.Price;
import org.pminin.tb.model.Trade;
import org.pminin.tb.model.Trade.Trades;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public interface AccountService {

	static Config config = ConfigFactory.load().getConfig("account");
	static String provider = config.getString("provider");

	void closeOrdersAndTrades(Instrument instrument);

	Order createOrder(Order order);

	Accounts getAccountDetails();

	Candles getCandles(Step step, DateTime start, Instrument instrument);

	Instrument getInstrument(String pair);

	Instrument getInstrument(String left, String right);

	Orders getOrders(Instrument instrument);

	Pivot getPivot(Instrument instrument);
	
	Price getPrice(Instrument instrument);

	Trades getTrades(Instrument instrument);

	Order updateOrder(Order order);

	Trade updateTrade(Trade trade);
	
	List<CalendarEvent> getCalendarEvents(Instrument instrument, int futureHoursCount);

}