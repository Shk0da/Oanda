package com.oanda.bot;

import akka.actor.ActorSystem;
import com.oanda.bot.actor.Messages;
import com.oanda.bot.config.ActorConfig;
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

    @Scheduled(cron = "${oandabot.scheduler.trade-check.cron}")
    public void fireTrade() {
        if (actorSystem == null) return;

        actorSystem.actorSelection(ActorConfig.ACTOR_PATH_HEAD + "*/*").tell(Messages.WORK, actorSystem.guardian());
    }

    @Scheduled(cron = "${oandabot.scheduler.start-work.cron}")
    public void startWork() {
        if (actorSystem == null) return;

        actorSystem.actorSelection(ActorConfig.ACTOR_PATH_HEAD + "*/*").tell(new Messages.WorkTime(true), actorSystem.guardian());
    }

    @Scheduled(cron = "${oandabot.scheduler.end-work.cron}")
    public void tgiFriday() {
        if (actorSystem == null) return;

        actorSystem.actorSelection(ActorConfig.ACTOR_PATH_HEAD + "*/*").tell(new Messages.WorkTime(false), actorSystem.guardian());
    }
}
