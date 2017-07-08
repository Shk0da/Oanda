package com.oanda.bot.actor.collector;

import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.constants.Event;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.model.Pivot;
import com.oanda.bot.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("PivotCollectorActor")
@Scope("prototype")
public class PivotCollectorActor extends AbstractInstrumentActor {

	@Autowired
	MainDao collectorDao;
	@Autowired
	AccountService accountService;

	public PivotCollectorActor(Instrument instrument) {
		super(instrument);
	}

	private String analyzerActorUrl() {
		return ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + ANALYZER + "/" + PIVOT;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		Pivot pivot = accountService.getPivot(instrument);
		if (pivot != null) {
			Pivot lastPivot = collectorDao.getLastPivot(instrument);
			if (lastPivot == null || lastPivot.getTime() == null || pivot.getTime().isAfter(lastPivot.getTime())) {
				int cnt = collectorDao.insertPivot(pivot, instrument);
				if (cnt > 0) {
					getContext().actorSelection(analyzerActorUrl()).tell(new Event.PivotChanged(pivot), self());
				}
			}
		} else {
			throw new Exception("Pivot is null");
		}
	}

}
