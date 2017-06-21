package com.oanda.bot.actor.analyzer;

import akka.actor.ActorRef;
import akka.actor.Props;
import com.oanda.bot.StrategySteps;
import com.oanda.bot.actor.SpringDIActor;
import com.oanda.bot.actor.abstracts.StepActor;
import com.oanda.bot.constants.Constants;
import com.oanda.bot.constants.Step;
import com.oanda.bot.model.Candle;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Event.CandlesCollected;
import com.oanda.bot.constants.Event.CurrentRate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("CandleAnalyzerActor")
@Scope("prototype")
public class CandleAnalyzerActor extends StepActor {

	private ActorRef confirmFractal;

	private ActorRef breakFractal;
	@Autowired
	private StrategySteps steps;

	public CandleAnalyzerActor(Instrument instrument, Step step) {
		super(instrument, step);
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof CandlesCollected) {
			confirmFractal.tell(msg, getContext().sender());
		} else if (msg instanceof Candle) {
			breakFractal.tell(msg, sender());
			if (steps.tradingStep().equals(step)) {
				getContext().actorSelection(Constants.ACTOR_PATH_HEAD + "/" + instrument.toString() + "/" + Constants.STRATEGY)
						.tell(new CurrentRate((Candle) msg), self());
			}
		} else if (Event.PROCESS_FRACTALS.equals(msg)) {
			confirmFractal.tell(msg, sender());
			breakFractal.tell(msg, sender());
		} else {
			unhandled(msg);
		}
	}

	@Override
	public void preStart() throws Exception {
		confirmFractal = getContext().actorOf(
				Props.create(SpringDIActor.class, ConfirmFractalActor.class, instrument, step), Constants.CONFIRM_FRACTAL);
		breakFractal = getContext()
				.actorOf(Props.create(SpringDIActor.class, BreakFractalActor.class, instrument, step), Constants.BREAK_FRACTAL);
	}

}
