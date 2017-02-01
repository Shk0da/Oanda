package org.pminin.tb.actor;

import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.actor.analyzer.AnalyzerActor;
import org.pminin.tb.actor.collector.CollectorActor;
import org.pminin.tb.actor.collector.NewsCheckActor;
import org.pminin.tb.actor.strategy.StrategyActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.Props;

@Component("InstrumentActor")
@Scope("prototype")
public class InstrumentActor extends AbstractInstrumentActor {

	@Autowired
	MainDao candlesDao;
	private ActorRef analyzer;
	private ActorRef collector;
	private ActorRef strategy;
//	private ActorRef newsActor;

	public InstrumentActor(Instrument instrument) {
		super(instrument);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (Event.WORK.equals(message)) {
			collector.tell(Event.COLLECT_PIVOT, self());
			analyzer.tell(Event.PROCESS_FRACTALS, sender());
			strategy.tell(Event.WORK, self());
		} else {
			unhandled(message);
		}
	}

	@Override
	public void preStart() throws Exception {
		candlesDao.createTables(instrument);
		strategy = getContext().actorOf(Props.create(SpringDIActor.class, StrategyActor.class, instrument), STRATEGY);
		collector = getContext().actorOf(Props.create(SpringDIActor.class, CollectorActor.class, instrument),
				COLLECTOR);
		analyzer = getContext().actorOf(Props.create(SpringDIActor.class, AnalyzerActor.class, instrument), ANALYZER);
//		newsActor = 
		getContext().actorOf(Props.create(SpringDIActor.class, NewsCheckActor.class, instrument), NEWSCHECK);
	}

}
