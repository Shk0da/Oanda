package com.oanda.bot.actor;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.provider.ApplicationContextProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Spring DI для Акторов
 */
@Slf4j
public class SpringDIActor implements IndirectActorProducer {

    private Actor actorInstance;
    private Class<? extends Actor> type;
    private Instrument instrument;
    private Step step;

    public SpringDIActor(Class<? extends Actor> type, Instrument instrument, Step step) {
        this.type = type;
        this.instrument = instrument;
        this.step = step;
    }

    /**
     * Определяем тип созданного актора из [[Props]]
     */
    @Override
    public Class<? extends Actor> actorClass() {
        return type;
    }

    /**
     * Фабричный метод для создания свежих акторов
     */
    @Override
    public Actor produce() {
        Actor newActor = actorInstance;
        actorInstance = null;
        if (newActor == null) {
            try {
                newActor = type.getConstructor(Instrument.class, Step.class).newInstance(instrument, step);
            } catch (Exception e) {
                log.error("Unable to create actor of type: {}. {}. {}", type, instrument, step, e);
            }
        }

        ApplicationContextProvider.getApplicationContext().getAutowireCapableBeanFactory().autowireBean(newActor);
        return newActor;
    }
}