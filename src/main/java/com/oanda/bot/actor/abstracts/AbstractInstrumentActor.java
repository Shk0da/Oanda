package com.oanda.bot.actor.abstracts;

import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.model.Instrument;

public abstract class AbstractInstrumentActor extends UntypedActor implements Constants {

	protected static final Config config = ConfigFactory.load().getConfig("main");
	protected final Instrument instrument;
	protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public AbstractInstrumentActor(Instrument instrument) {
		super();
		this.instrument = instrument;
	}

}