package com.oanda.bot.actor.analyzer;

import com.oanda.bot.actor.abstracts.StepActor;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Candle;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Event.FractalBroken;
import com.oanda.bot.constants.Step;
import com.oanda.bot.model.Instrument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("BreakFractalActor")
@Scope("prototype")
public class BreakFractalActor extends StepActor {

	@Autowired
    MainDao mainDao;

	public BreakFractalActor(Instrument instrument, Step step) {
		super(instrument, step);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Candle) {
			Candle current = (Candle) msg;
			Candle high = mainDao.getLastFractal(step, instrument, Constants.DIRECTION_UP);
			if (high != null && !high.isBroken() && current.getHighMid() > high.getHighMid()) {
				high.setBroken(true);
				high.setDirection(Constants.DIRECTION_UP);
				high.setBrokenTime(current.getTime());
				int count = mainDao.breakFractal(instrument, step, high);
				if (count > 0) {
					log.debug("Fractal is broken (up) " + high.getBrokenTime());
					FractalBroken brokenFractal = new FractalBroken(step, high);
					getContext().actorSelection(Constants.ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + Constants.STRATEGY)
							.tell(brokenFractal, self());
				}
			}
			Candle low = mainDao.getLastFractal(step, instrument, Constants.DIRECTION_DOWN);
			if (low != null && !low.isBroken() && current.getLowMid() < low.getLowMid()) {
				low.setBroken(true);
				low.setDirection(Constants.DIRECTION_DOWN);
				low.setBrokenTime(current.getTime());
				int count = mainDao.breakFractal(instrument, step, low);
				if (count > 0) {
					log.debug("Fractal is broken (down) " + low.getBrokenTime());
					FractalBroken brokenFractal = new FractalBroken(step, low);
					getContext().actorSelection(Constants.ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + Constants.STRATEGY)
							.tell(brokenFractal, self());
				}
			}
		} else if (Event.PROCESS_FRACTALS.equals(msg)) {
			int count = mainDao.findBrokenFractals(step, instrument);
			if (count > 0) {
				log.debug("" + count + " fractals were broken since last sync");
			}
			Candle lastBrokenFractal = mainDao.getLastBrokenFractal(step, instrument);
			getContext().actorSelection(Constants.ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + Constants.STRATEGY)
					.tell(new FractalBroken(step, lastBrokenFractal), self());
		}
	}

}
