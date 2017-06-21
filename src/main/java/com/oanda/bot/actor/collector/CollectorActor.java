package com.oanda.bot.actor.collector;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.oanda.bot.StrategySteps;
import com.oanda.bot.actor.SpringDIActor;
import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.constants.Event;
import com.oanda.bot.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("CollectorActor")
@Scope("prototype")
public class CollectorActor extends AbstractInstrumentActor {

	private ActorRef pivotCollector;
	private ActorRef m30Collector;
	private ActorRef m5Collector;
	private ActorRef tradeChecker;

	@Autowired
	private StrategySteps steps;

	public CollectorActor(Instrument instrument) {
		super(instrument);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.debug("tick " + msg);
		if (Event.COLLECT_CANDLES.equals(msg)) {
			m5Collector.tell(msg, getContext().sender());
			m30Collector.tell(msg, getContext().sender());
		} else if (Event.CHECK_TRADES_ORDERS.equals(msg)) {
			tradeChecker.tell(msg, getContext().sender());
		} else if (Event.COLLECT_PIVOT.equals(msg)) {
			pivotCollector.tell(msg, getContext().sender());
		}
	}

	@Override
	public void preStart() throws Exception {
		m5Collector = getContext().actorOf(
				Props.create(SpringDIActor.class, StepCollectorActor.class, instrument, steps.tradingStep()),
				steps.tradingStep().toString());
		m30Collector = getContext().actorOf(
				Props.create(SpringDIActor.class, StepCollectorActor.class, instrument, steps.trendStep()),
				steps.trendStep().toString());
		pivotCollector = getContext().actorOf(Props.create(SpringDIActor.class, PivotCollectorActor.class, instrument),
				Constants.PIVOT);
		tradeChecker = getContext().actorOf(Props.create(SpringDIActor.class, TradeCheckActor.class, instrument),
				Constants.TRADE_CHECK);
	}

}
