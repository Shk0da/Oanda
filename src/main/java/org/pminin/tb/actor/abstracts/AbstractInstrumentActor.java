package org.pminin.tb.actor.abstracts;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.pminin.tb.constants.Constants;
import org.pminin.tb.model.Instrument;

public abstract class AbstractInstrumentActor extends UntypedActor implements Constants {

	protected static final Config config = ConfigFactory.load().getConfig("main");
	protected final Instrument instrument;
	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public AbstractInstrumentActor(Instrument instrument) {
		super();
		this.instrument = instrument;
	}

}