package com.oanda.bot.actor.analyzer;

import com.oanda.bot.actor.abstracts.StepActor;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Step;
import com.oanda.bot.model.Candle;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.model.Pivot;
import com.oanda.bot.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("PriceAnalyzerActor")
@Scope("prototype")
public class PriceAnalyzerActor extends StepActor {

	@Autowired
	private AccountService accountService;
	
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
				log.info("The trend seems to be very hot. Closing trades for a few hours.");
				getContext().actorSelection(Constants.ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + Constants.STRATEGY)
						.tell(Event.TREND_IS_HOT, self());
			}

		} else if (Event.PROCESS_FRACTALS.equals(msg)) {
			unhandled(msg);
		}
	}

}
