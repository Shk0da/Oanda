package org.pminin.tb.actor.analyzer;

import org.pminin.tb.StrategySteps;
import org.pminin.tb.actor.SpringDIActor;
import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import akka.actor.ActorRef;
import akka.actor.Props;

@Component("AnalyzerActor")
@Scope("prototype")
public class AnalyzerActor extends AbstractInstrumentActor {

	private ActorRef fractalAnalyzerM5;
	private ActorRef fractalAnalyzerM30;

	@Autowired
	private StrategySteps steps;

	public AnalyzerActor(Instrument instrument) {
		super(instrument);
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (Event.PROCESS_FRACTALS.equals(message)) {
			fractalAnalyzerM30.tell(message, sender());
			fractalAnalyzerM5.tell(message, sender());
		} else {
			unhandled(message);
		}
	}

	@Override
	public void preStart() throws Exception {
		fractalAnalyzerM5 = getContext().actorOf(
				Props.create(SpringDIActor.class, CandleAnalyzerActor.class, instrument, steps.tradingStep()),
				steps.tradingStep().toString());
		fractalAnalyzerM30 = getContext().actorOf(
				Props.create(SpringDIActor.class, CandleAnalyzerActor.class, instrument, steps.trendStep()),
				steps.trendStep().toString());
		getContext().actorOf(Props.create(SpringDIActor.class, PivotChangeActor.class, instrument), PIVOT);
	}

}
