package org.pminin.tb.actor.strategy;

import java.text.ParseException;
import java.util.Date;

import org.joda.time.DateTime;
import org.pminin.tb.StrategySteps;
import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.constants.Event.CurrentRate;
import org.pminin.tb.constants.Event.FractalBroken;
import org.pminin.tb.constants.Event.FractalConfirmed;
import org.pminin.tb.constants.Event.TradeOpened;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Order;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.model.Price;
import org.pminin.tb.model.State;
import org.pminin.tb.model.StateChange;
import org.pminin.tb.model.Trade;
import org.pminin.tb.model.TradingState;
import org.pminin.tb.service.AccountService;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("StrategyActor")
@Scope("prototype")
public class StrategyActor extends AbstractInstrumentActor implements TradingState {

	@Autowired
	private AccountService accountService;

	@Autowired
	private MainDao mainDao;

	@Autowired
	private StrategySteps steps;

	private Order order = new Order();

	private State state = State.INACTIVE;

	private Candle broken30M;

	private Candle brokenOpp5M;

	private Candle confirmed5M;

	private Candle confirmedOpp5M;

	private Candle vLine;

	private Candle rate;

	private boolean heartbeat;

	private Trade openedTrade;

	public StrategyActor(Instrument instrument) {
		super(instrument);
	}

	public Candle getCurrentRate() {
		return rate;
	}

	public Candle getLastBrokenFractal30M() {
		return broken30M;
	}

	public Candle getLastBrokenFractal5MOpp() {
		return brokenOpp5M;
	}

	public Candle getLastConfirmedFractal5M() {
		return confirmed5M;
	}

	public Candle getLastConfirmedFractal5MOpp() {
		return confirmedOpp5M;
	}

	public int getMarketDirection() {
		return broken30M != null ? broken30M.getDirection() : 0;
	}

	private double getOrderPrice() {
		int direction = getMarketDirection();
		Candle confirmed5M = mainDao.getLastFractal(steps.tradingStep(), instrument, getMarketDirection());
		if (direction == DIRECTION_UP) {
			return confirmed5M.getHighMid();
		} else if (direction == DIRECTION_DOWN) {
			return confirmed5M.getLowMid();
		} else {
			return 0;
		}
	}

	private double getOrderStopLoss() {
		double stopLoss = 0;
		int direction = getMarketDirection();
		Candle confirmedOpp5M = mainDao.getLastFractal(steps.tradingStep(), instrument, -getMarketDirection());
		if (direction == DIRECTION_UP) {
			stopLoss = confirmedOpp5M.getLowMid() + instrument.getPip();
		} else if (direction == DIRECTION_DOWN) {
			stopLoss = confirmedOpp5M.getHighMid() - instrument.getPip();
		}
		return stopLoss;
	}

	public State getState() {
		return state;
	}

	private void initOrder() {
		log.info("Initializing new order...");
		log.info("Current rate is " + rate.getCloseMid());
		order = new Order();
		order.setInstrument(instrument.toString());
		order.setSide(getMarketDirection() == DIRECTION_UP ? BUY : SELL);
		log.info("Order side is set to " + order.getSide());
		double balance = accountService.getAccountDetails().getBalance();
		order.setUnits((int) (balance / 100 * 1000));
		log.info("Order units is set to " + order.getUnits());
		updateOrder();
		order = accountService.createOrder(order);
		if (order != null) {
			state = State.ORDER_POSTED;
		} else {
			resetState(true);
		}
	}

	private boolean isValidFractalTime(Candle fractal) {
		return vLine != null && fractal != null && vLine.getTime().isBefore(fractal.getTime());
	}

	private void lookForTrend() {
		Candle lastBrokenFractal = mainDao.getLastBrokenFractal(steps.trendStep(), instrument);
		if (lastBrokenFractal != null) {
			processBrokenFractal(new FractalBroken(steps.trendStep(), lastBrokenFractal));
		}
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		if (msg instanceof Event) {
			Event event = (Event) msg;
			processEvent(event);
		} else if (State.INACTIVE.equals(state)) {
			return;
		} else if (msg instanceof FractalBroken) {
			processBrokenFractal((FractalBroken) msg);
		} else if (msg instanceof FractalConfirmed) {
			processConfirmedFractal((FractalConfirmed) msg);
		} else if (msg instanceof CurrentRate) {
			setCurrentRate(((CurrentRate) msg).getCandle());
		} else if (msg instanceof TradeOpened) {
			processTradeOpened(((TradeOpened) msg).getTrade());
		}
	}

	@Override
	public void preStart() throws Exception {
		super.preStart();
		heartbeat = config.hasPath("heartbeat") && config.getBoolean("heartbeat");
	}

	private void processBrokenFractal(FractalBroken brokenFractal) {
		Candle fractal = brokenFractal.getBreakingCandle();
		if (fractal != null) {
			try {
				CronExpression expression = new CronExpression(config.getString("scheduler.end-week.cron"));
				Date prevEndDate = expression.getNextValidTimeAfter(DateTime.now().minusWeeks(1).toDate());
				if (fractal.getTime().isBefore(prevEndDate.getTime())) {
					log.info("New broken fractal is from the previous week so it will be ignored");
					return;
				}
			} catch (ParseException e) {
				log.info("A problem occored whiule checking fractal time");
			}
		}
		if (fractal == null && steps.trendStep().equals(brokenFractal.getStep())) {
			setBroken30M(fractal);
		} else if (State.WAITING_FOR_TREND.equals(state) || fractal.getDirection() != getMarketDirection()) {
			// check if directions are opposite ( -1 + 1 )
			if (steps.trendStep().equals(brokenFractal.getStep())) {
				setBroken30M(fractal);
			} else if (steps.tradingStep().equals(brokenFractal.getStep()) && isValidFractalTime(fractal)) {
				setBrokenOpp5M(fractal);
			} else {
				log.info("Broken fractal is outside the vertical line");
			}
		}
	}

	private void processConfirmedFractal(FractalConfirmed fractalConfirmed) {
		Candle fractal = fractalConfirmed.getCandle();
		if (steps.tradingStep().equals(fractalConfirmed.getStep())) {
			if (isValidFractalTime(fractal)) {
				if (fractal.getDirection() == getMarketDirection()) {
					setConfirmed5M(fractal);
				} else {
					setConfirmedOpp5M(fractal);
				}
			} else {
				log.info("Confirmed fractal is outside the vertical line");
			}
		}
	}

	private void processEvent(Event event) {
		switch (event) {
		case WORK:
			if (state == State.INACTIVE) {
				state = State.WAITING_FOR_TREND;
				if (config.hasPath("forcetrendlookup") && config.getBoolean("forcetrendlookup")) {
					log.info("Current trend lokup is forced. Looking for last broken 30M fractal...");
					lookForTrend();
				} else {
					log.info("Started work. Waiting for trend...");
				}
			}
			break;
		case TRADE_CLOSED:
			log.info("The trade has been closed. Resetting state...");
			resetState(true);
			break;
		case ORDER_CLOSED:
			log.info("The pending order has been closed. Resetting state...");
			resetState(true);
			break;
		case KILL_EM_ALL:
		case TGI_FRIDAY:
		case NEWS_IN_5:
		case TREND_IS_HOT:
			resetState(false);
			break;
		default:
			unhandled(event);
		}
	}

	private void processTradeOpened(Trade trade) {
		log.info("The trade has opened. ID : " + trade.getId());
		openedTrade = trade;
		state = State.TRADE_OPENED;
	}

	private void reportState() {
		log.info(String.format("Current state: %s; Trend: %s", state.toString(),
				getMarketDirection() == DIRECTION_UP ? "up" : "down"));
	}

	private void resetOrder() {
		brokenOpp5M = null;
		confirmed5M = null;
		confirmedOpp5M = null;
		vLine = null;
		rate = null;
		order = new Order();
		accountService.closeOrdersAndTrades(instrument);
	}

	private void resetState(boolean lookForNewTrend) {
		if (!State.INACTIVE.equals(state)) {
			log.info("Resetting state: closing trades and orders and looking for the last broken 30M fractal...");
			if (State.TRADE_OPENED.equals(state) || State.ORDER_POSTED.equals(state)) {
				resetOrder();
			}
			state = State.WAITING_FOR_TREND;
			if (lookForNewTrend) {
				lookForTrend();
			} else {
				processBrokenFractal(new FractalBroken(steps.trendStep(), null));
			}
		}
	}

	private void setBroken30M(Candle broken30M) {
		switch (state) {
		case INACTIVE:
			log.info("Skipping new broken 30M fractal as the state is incorrect");
			break;

		case WAITING_FOR_VERTICAL_LINE:
		case WAITING_FOR_FRACTALS:
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
			if (broken30M != null && getMarketDirection() + broken30M.getDirection() != 0) {
				break;
			}
		case WAITING_FOR_TREND:
			this.broken30M = broken30M;
			resetOrder();
			state = broken30M == null ? State.INACTIVE : State.WAITING_FOR_VERTICAL_LINE;
			if (State.WAITING_FOR_VERTICAL_LINE.equals(state)) {
				log.info("Got new market trend (" + (broken30M.getDirection() == DIRECTION_UP ? "up" : "down")
						+ "). Waiting for vertical line...");
			} else {
				log.info("No trend defined. Closing the trading.");
				break;
			}
			reportState();
			setCurrentRate(broken30M);
			Candle lastConfirmedTrendFractal = mainDao.getLastFractal(steps.tradingStep(), instrument,
					getMarketDirection());
			if (lastConfirmedTrendFractal != null) {
				processConfirmedFractal(new FractalConfirmed(steps.tradingStep(), lastConfirmedTrendFractal));
			}
			updateTraderState(StateChange.TREND_CHANGED, broken30M);
		default:
			break;
		}
	}

	private void setBrokenOpp5M(Candle brokenOpp5M) {
		switch (state) {
		case INACTIVE:
		case WAITING_FOR_TREND:
		case WAITING_FOR_VERTICAL_LINE:
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
			log.debug("Skipping new broken 5M fractal as the state is incorrect");
			break;
		case WAITING_FOR_FRACTALS:
			if (confirmedOpp5M == null || confirmedOpp5M.getTime().isBefore(brokenOpp5M.getTime())) {
				this.confirmedOpp5M = brokenOpp5M;
			}
			log.info("Opposite 5M fractal has broken.");
			state = confirmed5M != null && brokenOpp5M != null && confirmedOpp5M != null ? State.TRADING : state;
			this.brokenOpp5M = brokenOpp5M;
			reportState();
			updateTraderState(StateChange.OPP_FRACTAL_BROKEN, brokenOpp5M);
		default:
			break;
		}
	}

	private void setConfirmed5M(Candle confirmed5M) {
		switch (state) {
		case INACTIVE:
		case WAITING_FOR_TREND:
		case WAITING_FOR_VERTICAL_LINE:
			log.debug("Skipping new confirmed 5M fractal as the state is incorrect");
			break;
		case WAITING_FOR_FRACTALS:
			log.debug("5M trend factal has confirmed");
			state = confirmed5M != null && brokenOpp5M != null && confirmedOpp5M != null ? State.TRADING : state;
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
			this.confirmed5M = confirmed5M;
			if (confirmed5M.getTime().isBefore(vLine.getTime())) {
				vLine = confirmed5M;
				Candle lastFractal = mainDao.getLastFractal(steps.tradingStep(), instrument,
						-confirmed5M.getDirection());
				processConfirmedFractal(new FractalConfirmed(steps.tradingStep(), lastFractal));
			}
			reportState();
			updateTraderState(StateChange.FRACTAL_CONFIRMED, confirmed5M);
		default:
			break;
		}
	}

	private void setConfirmedOpp5M(Candle confirmedOpp5M) {
		switch (state) {
		case INACTIVE:
		case WAITING_FOR_TREND:
		case WAITING_FOR_VERTICAL_LINE:
			log.debug("Skipping new confirmed 5M fractal with opp direction as the state is incorrect");
			break;
		case WAITING_FOR_FRACTALS:
			log.debug("Opposite 5M factal has confirmed");
			state = confirmed5M != null && brokenOpp5M != null && confirmedOpp5M != null ? State.TRADING : state;
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
			this.confirmedOpp5M = confirmedOpp5M;
			if (confirmedOpp5M.isBroken()) {
				processBrokenFractal(new FractalBroken(steps.tradingStep(), confirmedOpp5M));
			}
			reportState();
			updateTraderState(StateChange.OPP_FRACTAL_CONFIRMED, confirmedOpp5M);
		default:
			break;
		}
	}

	private void setCurrentRate(Candle rate) {
		if (heartbeat) {
			log.info(String.format("price: %.5f", rate.getCloseMid()));
		}
		switch (state) {
		case INACTIVE:
		case WAITING_FOR_TREND:
			log.debug("Skipping curernt state as the state is incorrect");
			break;
		case WAITING_FOR_VERTICAL_LINE:
			vLine = rate;
			state = State.WAITING_FOR_FRACTALS;
			log.info(String.format("Got new vertical line %s. Waiting for fractals...", vLine.getTime()));
			reportState();
		case WAITING_FOR_FRACTALS:
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
		default:
			this.rate = rate;
			updateTraderState(StateChange.CURRENT_RATE, rate);
		}
	}

	private void updateOrder() {
		int direction = getMarketDirection();
		double newStopLoss = getOrderStopLoss();
		double newPrice = getOrderPrice();
		double oldStopLoss = order.getStopLoss();
		if (State.TRADE_OPENED.equals(state)) {
			boolean needUpdateStopLoss = openedTrade.getStopLoss() == 0
					|| newStopLoss * direction > openedTrade.getStopLoss() * direction;
			if (needUpdateStopLoss) {
				openedTrade.setStopLoss(newStopLoss);
				log.info(String.format("Order stop loss is set to %.5f", newStopLoss));
				if (accountService.updateTrade(openedTrade) == null) {
					log.info("A problem occurred during updating the trade. Resetting state...");
					resetState(true);
				}
			} else {
				log.info(String.format("Stop loss is %.5f. New stop loss %.5f will not be set",
						openedTrade.getStopLoss(), newStopLoss));
			}
		} else {
			boolean changed = false;
			if (order.getPrice() != newPrice) {
				order.setPrice(newPrice);
				changed = true;
				log.info(String.format("Order new price is set to %.5f", newPrice));
				Pivot lastPivot = mainDao.getLastPivot(instrument);
				Price price = accountService.getPrice(instrument);
				log.info(String.format("Current spread is %.5f", price.getSpread()));
				double takeProfit = lastPivot.getNearestWithMiddle(newPrice, price.getSpread(), getMarketDirection());
				if (order.getTakeProfit() != takeProfit) {
					order.setTakeProfit(takeProfit);
					log.info(String.format("Order new take profit is set to %.5f", takeProfit));
				}
			}
			boolean newSLisOK = newStopLoss * direction < newPrice * direction;
			boolean oldSLisNotOK = newPrice * direction < oldStopLoss * direction || oldStopLoss == 0;
			boolean newSLisWorse = newStopLoss * direction > oldStopLoss * direction && oldStopLoss != 0;
			if (oldSLisNotOK || newSLisOK && !newSLisWorse) {
				if (!newSLisOK) {
					newStopLoss = newPrice + instrument.getPip() * -direction * 3;
				}
				changed = true;
			}
			if (changed) {
				order.setStopLoss(newStopLoss);
				log.info("New order stop loss is set to " + newStopLoss);
			} else {
				log.info(String.format("Order price is %.5f and stop loss is %.5f. New stop loss %.5f will not be set",
						order.getPrice(), order.getStopLoss(), newStopLoss));
			}
			if (changed && State.ORDER_POSTED.equals(state)) {
				if (accountService.updateOrder(order) == null) {
					log.info("A problem occurred during updating the trade. Resetting state...");
					resetState(true);
				}
			}
		}
	}

	private void updateTraderState(StateChange change, Candle candle) {

		boolean hasOpenTrade = State.TRADE_OPENED.equals(state);
		boolean orderPosted = State.ORDER_POSTED.equals(state);
		boolean trading = State.TRADING.equals(state) || orderPosted || hasOpenTrade;
		if (trading) {
			if (!orderPosted && !hasOpenTrade) {
				initOrder();
			}
			switch (change) {
			case FRACTAL_CONFIRMED:
				log.info("New fractal is confirmed. Updating pending order price...");
				updateOrder();
				break;
			case OPP_FRACTAL_CONFIRMED:
				log.info("New opposite fractal is confirmed. Updating order stop loss value...");
				updateOrder();
				break;
			case TREND_CHANGED:
				resetState(true);
				break;
			default:
				break;

			}
		}
	}

}
