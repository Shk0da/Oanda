package com.oanda.bot.actor.abstracts;

import com.oanda.bot.constants.Step;
import com.oanda.bot.model.Instrument;

public abstract class StepActor extends AbstractInstrumentActor {

	protected final Step step;

	public StepActor(Instrument instrument, Step step) {
		super(instrument);
		this.step = step;
	}

}