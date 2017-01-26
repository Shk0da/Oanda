package org.pminin.tb.actor.collector;

import org.pminin.tb.actor.SpringDIActor;
import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.constants.Step;
import org.pminin.tb.model.Instrument;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.Props;

@Component("CollectorActor")
@Scope("prototype")
public class CollectorActor extends AbstractInstrumentActor {

	private ActorRef pivotCollector;
	private ActorRef m30Collector;
	private ActorRef m5Collector;
	private ActorRef tradeChecker;

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
				Props.create(SpringDIActor.class, StepCollectorActor.class, instrument, Step.M5), Step.M5.toString());
		m30Collector = getContext().actorOf(
				Props.create(SpringDIActor.class, StepCollectorActor.class, instrument, Step.M30), Step.M30.toString());
		pivotCollector = getContext().actorOf(Props.create(SpringDIActor.class, PivotCollectorActor.class, instrument),
				PIVOT);
		tradeChecker = getContext().actorOf(Props.create(SpringDIActor.class, TradeCheckActor.class, instrument),
				TRADE_CHECK);
	}

}
