package org.pminin.tb.actor;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import org.pminin.tb.ApplicationContextProvider;
import org.pminin.tb.constants.Step;
import org.pminin.tb.model.Instrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class SpringDIActor implements IndirectActorProducer {

	private static final Logger LOG = LoggerFactory.getLogger(SpringDIActor.class);

	private Class<? extends Actor> type;
	private Instrument instrument;
	private Step step;
	private int paramsCount;

	private Actor actorInstance;

	public SpringDIActor(Class<? extends Actor> type) {
		this.type = type;
		paramsCount = 0;
	}

	public SpringDIActor(Class<? extends Actor> type, Instrument instrument) {
		this.type = type;
		this.instrument = instrument;
		paramsCount = 1;
	}

	public SpringDIActor(Class<? extends Actor> type, Instrument instrument, Step step) {
		this.type = type;
		this.instrument = instrument;
		this.step = step;
		paramsCount = 2;
	}

	/**
	 * This method is used by [[Props]] to determine the type of actor which
	 * will be created. This means that an instance of this
	 * `IndirectActorProducer` will be created in order to call this method
	 * during any call to [[Props#actorClass]]; it should be noted that such
	 * calls may performed during actor set-up before the actual actor’s
	 * instantiation, and that the instance created for calling `actorClass` is
	 * not necessarily reused later to produce the actor.
	 */
	@Override
	public Class<? extends Actor> actorClass() {
		return type;
	}

	/**
	 * This factory method must produce a fresh actor instance upon each
	 * invocation. <b>It is not permitted to return the same instance more than
	 * once.</b>
	 */
	@Override
	public Actor produce() {
		Actor newActor = actorInstance;
		actorInstance = null;
		if (newActor == null) {
			try {
				switch (paramsCount) {
				case 1:
					newActor = type.getConstructor(Instrument.class).newInstance(instrument);
					break;
				case 2:
					newActor = type.getConstructor(Instrument.class, Step.class).newInstance(instrument, step);
					break;
				default:
					newActor = type.newInstance();
				}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				LOG.error("Unable to create actor of type:{}", type, e);
			}
		}

		ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(newActor);
		return newActor;
	}
}