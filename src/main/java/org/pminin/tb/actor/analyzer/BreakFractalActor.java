package org.pminin.tb.actor.analyzer;

import org.pminin.tb.actor.abstracts.StepActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.constants.Event.FractalBroken;
import org.pminin.tb.constants.Step;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Instrument;
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
			Candle high = mainDao.getLastFractal(step, instrument, DIRECTION_UP);
			if (high != null && !high.isBroken() && current.getHighAsk() > high.getHighAsk()) {
				high.setBroken(true);
				high.setDirection(DIRECTION_UP);
				high.setBrokenTime(current.getTime());
				int count = mainDao.breakFractal(instrument, step, high);
				if (count > 0) {
					log.info("Fractal is broken (up) " + high.getBrokenTime());
					FractalBroken brokenFractal = new FractalBroken(step, high);
					getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY)
							.tell(brokenFractal, self());
				}
			}
			Candle low = mainDao.getLastFractal(step, instrument, DIRECTION_DOWN);
			if (low != null && !low.isBroken() && current.getLowBid() < low.getLowBid()) {
				low.setBroken(true);
				low.setDirection(DIRECTION_DOWN);
				low.setBrokenTime(current.getTime());
				int count = mainDao.breakFractal(instrument, step, low);
				if (count > 0) {
					log.info("Fractal is broken (down) " + low.getBrokenTime());
					FractalBroken brokenFractal = new FractalBroken(step, low);
					getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY)
							.tell(brokenFractal, self());
				}
			}
		} else if (Event.PROCESS_FRACTALS.equals(msg)) {
			int count = mainDao.findBrokenFractals(step, instrument);
			if (count > 0) {
				log.info("" + count + " fractals were broken since last sync");
			}
			Candle lastBrokenFractal = mainDao.getLastBrokenFractal(step, instrument);
			getContext().actorSelection(ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + STRATEGY)
					.tell(new FractalBroken(step, lastBrokenFractal), self());
		}
	}

}
