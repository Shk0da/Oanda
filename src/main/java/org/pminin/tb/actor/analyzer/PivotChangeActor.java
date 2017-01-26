package org.pminin.tb.actor.analyzer;

import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.model.Instrument;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("PivotChangeActor")
@Scope("prototype")
public class PivotChangeActor extends AbstractInstrumentActor {

	public PivotChangeActor(Instrument instrument) {
		super(instrument);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.info(msg.toString());
	}

}
