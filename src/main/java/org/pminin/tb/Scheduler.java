package org.pminin.tb;

import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.pminin.tb.constants.Constants;
import org.pminin.tb.constants.Event;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;

@Component(value = "scheduler")
public class Scheduler implements Constants {

	private ActorSystem actorSystem;
	protected static final Config config = ConfigFactory.load().getConfig("main");
	private boolean active = false;

	private static final Logger log = LoggerFactory.getLogger(Scheduler.class);

	@Autowired
	public Scheduler(ActorSystem actorSystem) {
		this.actorSystem = actorSystem;
	}

	private boolean isTrue(String path) {
		return config.hasPath(path) && config.getBoolean(path);
	}

	@PostConstruct
	public void fireInitialCollection() {
		boolean forceStartOfDay = isTrue("forcestartofday") || isTrue("forcetrendlookup");
		active = isTrue("autostart") || forceStartOfDay;
		if (active) {
			log.info("Autostart set to TRUE. Start collecting candles...");
		}
		if (forceStartOfDay) {
			log.info("Start of work day is forced. Starting trading...");
			startWorkEveryDay();
		} else {
			try {
				CronExpression expression = new CronExpression("${main.scheduler.start-work.cron}");
				log.info("Start of work day is not forced. Trading will start at "
						+ expression.getNextValidTimeAfter(new Date()));
			} catch (ParseException e) {
				log.info("Something went wrong while parsing cron expression. I will not say when trading will start");
			}
		}
	}

	@Scheduled(cron = "${main.scheduler.candle-collect.cron}")
	public void fireCollectCandles() {
		if (!active || actorSystem == null) {
			return;
		}
		actorSystem.actorSelection(Constants.ACTOR_PATH_HEAD + "*/" + Constants.COLLECTOR).tell(Event.COLLECT_CANDLES,
				actorSystem.guardian());
	}

	@Scheduled(cron = "${main.scheduler.trade-check.cron}")
	public void fireCheckTrades() {
		if (!active || actorSystem == null) {
			return;
		}
		actorSystem.actorSelection(Constants.ACTOR_PATH_HEAD + "*/" + Constants.COLLECTOR).tell(Event.CHECK_TRADES_ORDERS,
				actorSystem.guardian());
	}

	@Scheduled(cron = "${main.scheduler.pivot-collect.cron}")
	public void fireCollectPivot() {
		if (!active || actorSystem == null) {
			return;
		}
		actorSystem.actorSelection(Constants.ACTOR_PATH_HEAD + "*/" + Constants.COLLECTOR).tell(Event.COLLECT_PIVOT,
				actorSystem.guardian());
	}

	@Scheduled(cron = "${main.scheduler.start-work.cron}")
	public void startWorkEveryDay() {
		// Start work
		if (!active || actorSystem == null) {
			return;
		}
		fireCollectPivot();
		new ScheduledThreadPoolExecutor(1).schedule(() -> {
			actorSystem.actorSelection(Constants.ACTOR_PATH_HEAD + "*/" + Constants.STRATEGY).tell(Event.WORK,
					actorSystem.guardian());
		}, 15, TimeUnit.SECONDS);
	}

	@Scheduled(cron = "${main.scheduler.end-work.cron}")
	public void tgiFriday() {
		// End work
		if (!active || actorSystem == null) {
			return;
		}
		actorSystem.actorSelection(Constants.ACTOR_PATH_HEAD + "*/" + Constants.STRATEGY).tell(Event.TGI_FRIDAY,
				actorSystem.guardian());
	}

}
