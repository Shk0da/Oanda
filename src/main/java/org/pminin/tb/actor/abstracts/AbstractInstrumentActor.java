package org.pminin.tb.actor.abstracts;

import org.pminin.tb.constants.Constants;
import org.pminin.tb.model.Instrument;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public abstract class AbstractInstrumentActor extends UntypedActor implements Constants {

	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	protected static final Config config = ConfigFactory.load().getConfig("main");

	protected final Instrument instrument;

	public AbstractInstrumentActor(Instrument instrument) {
		super();
		this.instrument = instrument;
	}

}