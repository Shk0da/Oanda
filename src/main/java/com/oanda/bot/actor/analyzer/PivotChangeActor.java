package com.oanda.bot.actor.analyzer;

import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.model.Instrument;
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
