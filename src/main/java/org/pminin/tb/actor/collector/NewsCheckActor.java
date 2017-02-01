package org.pminin.tb.actor.collector;

import java.util.List;

import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.constants.Constants;
import org.pminin.tb.constants.Event;
import org.pminin.tb.model.CalendarEvent;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;

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
