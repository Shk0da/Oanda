package com.oanda.bot.config;

import akka.actor.ActorSystem;
import akka.actor.InvalidActorNameException;
import akka.actor.Props;
import com.oanda.bot.actor.SpringDIActor;
import com.oanda.bot.actor.TradeActor;
import com.oanda.bot.domain.Instrument;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.InstrumentRepository;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ActorConfig {

    public static final String ACTOR_PATH_HEAD = "akka://TradingSystem/user/";

    private static final Config config = ConfigFactory.load();

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Bean(name = "actorSystem")
    public ActorSystem actorSystem() {
        ActorSystem system = ActorSystem.create("TradingSystem", config.getConfig("main").withFallback(config));
        Config pairsCfg = config.getConfig("pairs");

        for (String key : pairsCfg.root().keySet()) {
            Config cfg = pairsCfg.getConfig(key);
            if (!cfg.getBoolean("on")) {
                continue;
            }

            String left = cfg.getString("left");
            String right = cfg.getString("right");
            Instrument instrument = instrumentRepository.getInstrument(left + "_" + right);

            if (instrument != null) {
                try {
                    Step step = Step.valueOf(cfg.getString("step"));
                    system.actorOf(Props.create(SpringDIActor.class, TradeActor.class, instrument, step), "TradeActor_" + instrument.getInstrument() + "_" + step.name());
                    log.info("Create: TradeActor_" + instrument.getInstrument() + "_" + step.name());
                } catch (InvalidActorNameException ex) {
                    log.error(ex.getMessage());
                }
            } else {
                log.error("Could not process instrument", new IllegalArgumentException("No such instrument found " + left + "/" + right));
            }
        }

        return system;
    }
}
