package com.oanda.bot.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.actor.analyzer.AnalyzerActor;
import com.oanda.bot.actor.collector.CollectorActor;
import com.oanda.bot.actor.collector.NewsCheckActor;
import com.oanda.bot.actor.strategy.IchimokuStrategyActor;
import com.oanda.bot.constants.Event;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("InstrumentActor")
@Scope("prototype")
public class InstrumentActor extends AbstractInstrumentActor {

    @Autowired
    MainDao candlesDao;
    private ActorRef analyzer;
    private ActorRef collector;
    private ActorRef strategy;

    public InstrumentActor(Instrument instrument) {
        super(instrument);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (Event.WORK.equals(message)) {
            collector.tell(Event.COLLECT_PIVOT, self());
            analyzer.tell(Event.PROCESS_FRACTALS, sender());
            strategy.tell(Event.WORK, self());
        } else {
            unhandled(message);
        }
    }

    @Override
    public void preStart() throws Exception {
        candlesDao.createTables(instrument);
        strategy = getContext().actorOf(Props.create(SpringDIActor.class, IchimokuStrategyActor.class, instrument), STRATEGY);
        collector = getContext().actorOf(Props.create(SpringDIActor.class, CollectorActor.class, instrument), COLLECTOR);
        analyzer = getContext().actorOf(Props.create(SpringDIActor.class, AnalyzerActor.class, instrument), ANALYZER);
        getContext().actorOf(Props.create(SpringDIActor.class, NewsCheckActor.class, instrument), NEWSCHECK);
    }

}
