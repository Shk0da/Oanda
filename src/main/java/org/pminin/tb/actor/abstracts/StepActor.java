package org.pminin.tb.actor.abstracts;

import org.pminin.tb.constants.Step;
import org.pminin.tb.model.Instrument;

public abstract class StepActor extends AbstractInstrumentActor {

	protected final Step step;

	public StepActor(Instrument instrument, Step step) {
		super(instrument);
		this.step = step;
	}

}