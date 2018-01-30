package com.oanda.bot;

import akka.actor.ActorSystem;
import com.oanda.bot.actor.Messages;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.Step;
import com.oanda.bot.repository.InstrumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@EnableScheduling
@Component(value = "scheduler")
public class Scheduler {

    @Autowired
    private ActorSystem actorSystem;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Scheduled(cron = "${oandabot.scheduler.candle-collect.cron}")
    public void fireCollect() {
        if (actorSystem == null) return;

        instrumentRepository.getAllInstruments().forEach(instrument ->
                Arrays.asList(Step.values()).forEach(step -> {
                    actorSystem
                            .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "CollectorActor_" + instrument + "_" + step.name())
                            .tell(Messages.WORK, actorSystem.guardian());

                    actorSystem
                            .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "LearnActor_" + instrument + "_" + step.name())
                            .tell(Messages.WORK, actorSystem.guardian());
                })
        );
    }

    @Scheduled(cron = "${oandabot.scheduler.trade-check.cron}")
    public void fireTrade() {
        if (actorSystem == null) return;

        instrumentRepository.getAllInstruments().forEach(instrument ->
                Arrays.asList(Step.values()).forEach(step ->
                        actorSystem
                                .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "TradeActor_" + instrument + "_" + step.name())
                                .tell(Messages.WORK, actorSystem.guardian())
                )
        );
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
