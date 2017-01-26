package org.pminin.tb.actor.collector;

import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.service.AccountService;
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

	@Override
	public void onReceive(Object msg) throws Exception {
		Pivot pivot = accountService.getPivot(instrument);
		if (pivot != null) {
			Pivot lastPivot = collectorDao.getLastPivot(instrument);
			if (lastPivot == null || pivot.getTime().isAfter(lastPivot.getTime())) {
				int cnt = collectorDao.insertPivot(pivot, instrument);
				if (cnt > 0) {
					getContext().actorSelection(analyzerActorUrl()).tell(new Event.PivotChanged(pivot), self());
				}
			}
		} else {
			throw new Exception("Pivot is null");
		}
	}

	private String analyzerActorUrl() {
		return ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + ANALYZER + "/" + PIVOT;
	}

}
