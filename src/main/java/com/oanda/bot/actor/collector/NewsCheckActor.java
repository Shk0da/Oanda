package com.oanda.bot.actor.collector;

import java.util.List;

import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.constants.Event;
import com.oanda.bot.model.CalendarEvent;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component("NewsCheckActor")
@Scope("prototype")
public class NewsCheckActor extends AbstractInstrumentActor {

	@Autowired
	private TaskScheduler scheduler;
	@Autowired
	private AccountService accountService;

	public NewsCheckActor(Instrument instrument) {
		super(instrument);
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
		List<CalendarEvent> events = accountService.getCalendarEvents(instrument, 1);
		events.stream().forEach(event -> {
			scheduler.schedule(() -> {
				if (event.getImpact() > 1) {
					log.info("News will be published in 5 minutes: {} (impact: {})", event.getTitle(),
							event.getImpact());
					getContext().actorSelection(Constants.ACTOR_PATH_HEAD + "*/" + Constants.STRATEGY)
							.tell(Event.NEWS_IN_5, self());
				}
			}, event.getDateTime().minusMinutes(5).toDate());
		});
	}

}
