package com.oanda.bot.actor.collector;

import com.oanda.bot.actor.abstracts.StepActor;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Step;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Candle;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.util.ModelUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

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
		return Constants.ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + Constants.ANALYZER + "/" + step.toString();
	}

	@Override
	public void onReceive(Object arg0) throws Exception {
		DateTime start = lastCandle != null ? lastCandle.getTime()
				: DateTime.now(DateTimeZone.getDefault()).minusHours(36);
		Candle.Candles candles = accountService.getCandles(step, start, instrument);
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
