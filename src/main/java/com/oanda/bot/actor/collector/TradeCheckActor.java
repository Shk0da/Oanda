package com.oanda.bot.actor.collector;

import com.oanda.bot.model.Order;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Event.TradeOpened;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.model.Trade.Trades;
import org.springframework.beans.factory.annotation.Autowired;

public class TradeCheckActor extends AbstractInstrumentActor {

	private int openTradesCount = 0;
	private int openOrdersCount = 0;

	@Autowired
	private AccountService accountService;

	public TradeCheckActor(Instrument instrument) {
		super(instrument);
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
		Object tradeEvent = null;
		Object orderEvent = null;
		Order.Orders orders = accountService.getOrders(instrument);
		if (orders.getOrders().isEmpty() != (openOrdersCount == 0)) {
			if (openOrdersCount == 0) {
				openOrdersCount = 1;
			} else {
				openOrdersCount = 0;
				orderEvent = Event.ORDER_CLOSED;
			}
		}
		Trades trades = accountService.getTrades(instrument);
		if (trades.getTrades().isEmpty() != (openTradesCount == 0)) {
			if (openTradesCount == 0) {
				openTradesCount = 1;
				tradeEvent = new TradeOpened(trades.getTrades().stream().findFirst().orElse(null));
			} else {
				openTradesCount = 0;
				tradeEvent = Event.TRADE_CLOSED;
			}
		}
		if (tradeEvent != null) {
			getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY).tell(tradeEvent,
					self());
		} else if (orderEvent != null) {
			getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY).tell(orderEvent,
					self());
		}
	}

}
