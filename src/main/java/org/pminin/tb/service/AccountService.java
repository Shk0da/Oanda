package org.pminin.tb.service;

import org.joda.time.DateTime;
import org.pminin.tb.constants.Step;
import org.pminin.tb.model.AccountDetails;
import org.pminin.tb.model.Candle.Candles;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Order;
import org.pminin.tb.model.Order.Orders;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.model.Trade;
import org.pminin.tb.model.Trade.Trades;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public interface AccountService {

	static Config config = ConfigFactory.load().getConfig("account");
	static String provider = config.getString("provider");

	void closeOrdersAndTrades(Instrument instrument);

	Order createOrder(Order order);

	AccountDetails getAccountDetails();

	Candles getCandles(Step step, DateTime start, Instrument instrument);

	Instrument getInstrument(String left, String right);
	
	Instrument getInstrument(String pair);

	Orders getOrders(Instrument instrument);

	Pivot getPivot(Instrument instrument);

	Trades getTrades(Instrument instrument);

	Order updateOrder(Order order);

	Trade updateTrade(Trade trade);

}