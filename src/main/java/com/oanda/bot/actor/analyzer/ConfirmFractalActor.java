package com.oanda.bot.actor.analyzer;

import com.oanda.bot.actor.abstracts.StepActor;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Event.FractalConfirmed;
import com.oanda.bot.constants.Step;
import com.oanda.bot.model.Candle;
import com.oanda.bot.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("ConfirmFractalActor")
@Scope("prototype")
public class ConfirmFractalActor extends StepActor {

	@Autowired
    MainDao mainDao;

	public ConfirmFractalActor(Instrument instrument, Step step) {
		super(instrument, step);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (Event.PROCESS_FRACTALS.equals(msg) || msg instanceof Event.CandlesCollected) {
			int count = mainDao.findFractals(step, instrument);
			if (count > 0) {
				Candle lastFractal = mainDao.getLastFractal(step, instrument);
				log.debug(String.format("New confirmed fractal direction: %s",
						lastFractal.getDirection() == DIRECTION_UP ? "up" : "down"));
				getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY)
						.tell(new FractalConfirmed(step, lastFractal), self());
			}
		}
	}

}
