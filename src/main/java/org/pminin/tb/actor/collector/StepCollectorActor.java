package org.pminin.tb.actor.collector;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.pminin.tb.actor.abstracts.StepActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.constants.Step;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Candle.Candles;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.service.AccountService;
import org.pminin.tb.util.ModelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("StepCollectorActor")
@Scope("prototype")
public class StepCollectorActor extends StepActor {

	@Autowired
	MainDao collectorDao;
	@Autowired
	AccountService accountService;

	private Candle lastCandle;

	public StepCollectorActor(Instrument instrument, Step step) {
		super(instrument, step);
	}

	private String analyzerActorUrl() {
		return ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + ANALYZER + "/" + step.toString();
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
		DateTime start = lastCandle != null ? lastCandle.getTime()
				: DateTime.now(DateTimeZone.getDefault()).minusHours(36);
		Candles candles = accountService.getCandles(step, start, instrument);
		Candle cndl = collectorDao.insertCandles(candles);
		if (cndl != null) {
			lastCandle = cndl;
			getContext().actorSelection(analyzerActorUrl()).tell(new Event.CandlesCollected(), self());
		}
		Candle incomplete = null;
		List<Candle> incompleteList = ModelUtil.getNotCompletedCandles(candles);
		if (incompleteList != null && !incompleteList.isEmpty()) {
			// We get only one imcomplete candle
			incomplete = incompleteList.iterator().next();
		}
		if (incomplete != null) {
			getContext().actorSelection(analyzerActorUrl()).tell(incomplete, self());
		}
	}

	@Override
	public void preStart() throws Exception {
		lastCandle = collectorDao.getLastCandle(step, instrument);
	}

}
