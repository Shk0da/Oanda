package com.oanda.bot;

import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@EnableScheduling
@Component(value = "scheduler")
public class Scheduler {

	@Autowired
	private ActorSystem actorSystem;

	@Scheduled(cron = "${main.scheduler.candle-collect.cron}")
	public void fireCollectCandles() {
		if (actorSystem == null) return;
	}

	@Scheduled(cron = "${main.scheduler.start-work.cron}")
	public void startWorkEveryDay() {
		if (actorSystem == null) return;
	}
}
