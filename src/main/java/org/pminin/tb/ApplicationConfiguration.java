package org.pminin.tb;

import org.pminin.tb.actor.InstrumentActor;
import org.pminin.tb.actor.SpringDIActor;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.service.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.Props;

@EnableAutoConfiguration
@Configuration
@EnableScheduling
public class ApplicationConfiguration {
	private static final Config config = ConfigFactory.load();
	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private InstrumentStorage storage;

	@Bean(name = "accountService")
	public AccountService accountService() {
		AccountService svc = (AccountService) applicationContext.getBean(AccountService.provider);
		return svc;
	}

	@Bean(name = "actorSystem")
	public ActorSystem actorSytem() {

		ApplicationContextProvider.setApplicationContext(applicationContext);
		ActorSystem system = ActorSystem.create("TradingSystem", config.getConfig("main").withFallback(config));
		Config pairsCfg = config.getConfig("pairs");

		for (String key : pairsCfg.root().keySet()) {
			Config cfg = pairsCfg.getConfig(key);
			if (!cfg.getBoolean("on")) {
				continue;
			}
			String left = cfg.getString("left");
			String right = cfg.getString("right");
			Instrument instrument = storage.getInstrument(left + "_" + right);
			if (instrument != null) {
				system.actorOf(Props.create(SpringDIActor.class, InstrumentActor.class, instrument),
						instrument.getInstrument());
			} else {
				getLogger().error("Could not process instrument",
						new IllegalArgumentException("No such instrument found " + left + "/" + right));
			}
		}
		return system;
	}

	@Bean(name = "logger")
	public Logger getLogger() {
		return LoggerFactory.getLogger("mainLog");
	}

}
