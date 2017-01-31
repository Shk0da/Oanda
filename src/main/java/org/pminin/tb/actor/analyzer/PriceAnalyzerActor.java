package org.pminin.tb.actor.analyzer;

import org.pminin.tb.actor.abstracts.StepActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.constants.Step;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("PriceAnalyzerActor")
@Scope("prototype")
public class PriceAnalyzerActor extends StepActor {

	@Autowired
	MainDao mainDao;

	@Autowired
	AccountService accountService;

	public PriceAnalyzerActor(Instrument instrument, Step step) {
		super(instrument, step);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Candle) {
			Candle candle = (Candle) msg;
			Pivot pivot = accountService.getPivot(instrument);
			double s3 = pivot.getS3();
			double r3 = pivot.getR3();
			if (s3 <= candle.getLowMid() || r3 <= candle.getHighMid()) {
				getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY)
						.tell(Event.TREND_IS_HOT, self());
			}

		} else if (Event.PROCESS_FRACTALS.equals(msg)) {
			unhandled(msg);
		}
	}

}
