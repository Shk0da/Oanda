package org.pminin.tb.actor.strategy;

import org.pminin.tb.actor.abstracts.AbstractInstrumentActor;
import org.pminin.tb.constants.Event;
import org.pminin.tb.constants.Event.CurrentRate;
import org.pminin.tb.constants.Event.FractalBroken;
import org.pminin.tb.constants.Event.FractalConfirmed;
import org.pminin.tb.constants.Event.TradeOpened;
import org.pminin.tb.constants.Step;
import org.pminin.tb.dao.MainDao;
import org.pminin.tb.model.Candle;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Order;
import org.pminin.tb.model.Pivot;
import org.pminin.tb.model.State;
import org.pminin.tb.model.StateChange;
import org.pminin.tb.model.Trade;
import org.pminin.tb.model.TradingState;
import org.pminin.tb.service.AccountService;
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

	public State getState() {
		return state;
	}

	private boolean isValidFractalTime(Candle fractal) {
		return vLine != null && fractal != null && vLine.getTime().isBefore(fractal.getTime());
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

	private void processBrokenFractal(FractalBroken brokenFractal) {
		Candle fractal = brokenFractal.getBreakingCandle();
		// check if directions are opposite ( -1 + 1 )
		if (fractal.getDirection() != getMarketDirection()) {
			switch (brokenFractal.getStep()) {
			case M30:
				setBroke30M(fractal);
				break;
			case M5:
				// check if it is more recent than vertical line
				if (isValidFractalTime(fractal)) {
					setBrokenOpp5M(fractal);
				} else {
					log.info("Broken fractal is outside the vertical line");
				}
			default:
				break;
			}
		}
	}

	private void processConfirmedFractal(FractalConfirmed fractalConfirmed) {
		Candle fractal = fractalConfirmed.getCandle();
		if (Step.M5.equals(fractalConfirmed.getStep())) {
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

	private void lookForTrend() {
		Candle lastBrokenFractal = mainDao.getLastBrokenFractal(Step.M30, instrument);
		if (lastBrokenFractal != null) {
			processBrokenFractal(new FractalBroken(Step.M30, lastBrokenFractal));
		}
	}

	private void setBroke30M(Candle broken30M) {
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
			brokenOpp5M = null;
			confirmed5M = null;
			confirmedOpp5M = null;
			vLine = null;
			rate = null;
			order = new Order();
			state = broken30M == null ? State.INACTIVE : State.WAITING_FOR_VERTICAL_LINE;
			if (broken30M != null) {
				log.info("Got new market trend (" + (broken30M.getDirection() == DIRECTION_UP ? "up" : "down")
						+ "). Waiting for vertical line...");
			} else {
				log.info("No trend defined. Closing the trading.");
				resetState(false);
				break;
			}
			reportState();
			setCurrentRate(broken30M);
			Candle lastConfirmedTrendFractal = mainDao.getLastFractal(Step.M5, instrument, getMarketDirection());
			if (lastConfirmedTrendFractal != null) {
				processConfirmedFractal(new FractalConfirmed(Step.M5, lastConfirmedTrendFractal));
			}
			updateTraderState(StateChange.TREND_CHANGED, broken30M);
		default:
			break;
		}
	}

	private void setCurrentRate(Candle rate) {
		if (heartbeat) {
			log.info(String.format("prices: %.5f ask; %.5f bid", rate.getCloseAsk(), rate.getCloseBid()));
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
			log.info("Skipping new confirmed 5M fractal as the state is incorrect");
			break;
		case WAITING_FOR_FRACTALS:
			log.info("5M trend factal has confirmed");
			state = confirmed5M != null && brokenOpp5M != null && confirmedOpp5M != null ? State.TRADING : state;
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
			this.confirmed5M = confirmed5M;
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
			log.info("Skipping new confirmed 5M fractal with opp direction as the state is incorrect");
			break;
		case WAITING_FOR_FRACTALS:
			log.info("Opposite 5M factal has confirmed");
			state = confirmed5M != null && brokenOpp5M != null && confirmedOpp5M != null ? State.TRADING : state;
		case TRADING:
		case ORDER_POSTED:
		case TRADE_OPENED:
			this.confirmedOpp5M = confirmedOpp5M;
			reportState();
			updateTraderState(StateChange.OPP_FRACTAL_CONFIRMED, confirmedOpp5M);
		default:
			break;
		}
	}

	private void updateTraderState(StateChange change, Candle candle) {

		boolean hasOpenTrade = State.TRADE_OPENED.equals(state);
		boolean orderPosted = State.ORDER_POSTED.equals(state);
		boolean trading = State.TRADING.equals(state) || orderPosted || hasOpenTrade;
		if (trading) {
			if (!orderPosted) {
				initOrder();
			}
			switch (change) {
			case FRACTAL_CONFIRMED:
				if (!hasOpenTrade) {
					log.info("New fractal is confirmed. Updating pending order price...");
					updateOrderPrice();
				}
				break;
			case OPP_FRACTAL_CONFIRMED:
				log.info("New opposite fractal is confirmed. Updating order stop loss value...");
				updateStopLoss();
				break;
			case TREND_CHANGED:
				resetState(true);
				break;
			default:
				break;

			}
		}
	}

	private void resetState(boolean lookForNewTrend) {
		if (!State.INACTIVE.equals(state)) {
			log.info("Resetting state: closing trades and orders and looking for the last broken 30M fractal...");
			if (State.TRADE_OPENED.equals(state) || State.ORDER_POSTED.equals(state)) {
				accountService.closeOrdersAndTrades(instrument);
			}
			state = State.WAITING_FOR_TREND;
			if (lookForNewTrend) {
				lookForTrend();
			} else {
				setBroke30M(null);
			}
		}
	}

	private void initOrder() {
		log.info("Initializing new order...");
		log.info("Current rate is " + rate.getCloseBid());
		order = new Order();
		order.setInstrument(instrument);
		order.setSide(getMarketDirection() == DIRECTION_UP ? BUY : SELL);
		log.info("Order side is set to " + order.getSide());
		double balance = accountService.getAccountDetails().getBalance();
		order.setUnits((int) (balance / 100 * 1000));
		log.info("Order units is set to " + order.getUnits());
		updateOrderPrice();
		updateStopLoss();
		System.out.println();
		order = accountService.createOrder(order);
		if (order != null) {
			state = State.ORDER_POSTED;
		} else {
			resetState(true);
		}
	}

	private void updateStopLoss() {
		double stopLoss = 0;
		int direction = getMarketDirection();
		Candle confirmedOpp5M = mainDao.getLastFractal(Step.M5, instrument, -getMarketDirection());
		if (direction == DIRECTION_UP) {
			stopLoss = confirmedOpp5M.getLowAsk();
		} else if (direction == DIRECTION_DOWN) {
			stopLoss = confirmedOpp5M.getHighAsk();
		} else {
			return;
		}
		stopLoss += 0.00030 * (-direction);
		if (State.TRADE_OPENED.equals(state)) {
			if (openedTrade.getStopLoss() == 0 || stopLoss * direction > openedTrade.getStopLoss() * direction) {
				openedTrade.setStopLoss(stopLoss);
				log.info("Order stop loss is set to " + stopLoss);
			} else {
				log.info(String.format("Stop loss is %.5f. New stop loss %.5f will not be set",
						openedTrade.getStopLoss(), stopLoss));
			}
			if (accountService.updateTrade(openedTrade) == null) {
				log.info("A problem occurred during updating the trade. Resetting state...");
				resetState(true);
			}
		} else {
			if (order.getStopLoss() == 0 || stopLoss * direction > order.getStopLoss() * direction) {
				order.setStopLoss(stopLoss);
				log.info("Order stop loss is set to " + stopLoss);
			} else {
				log.info(String.format("Stop loss is %.5f. New stop loss %.5f will not be set", order.getStopLoss(),
						stopLoss));
			}
			if (State.ORDER_POSTED.equals(state)) {
				if (accountService.updateOrder(order) == null) {
					log.info("A problem occurred during updating the trade. Resetting state...");
					resetState(true);
				}
			}
		}
	}

	private void updateOrderPrice() {
		double price = getOrderPrice();
		order.setPrice(price);
		log.info("Order new price is set to " + price);
		Pivot lastPivot = mainDao.getLastPivot(instrument);
		double takeProfit = lastPivot.getNearestNoMiddle(price, getMarketDirection());
		order.setTakeProfit(takeProfit);
		log.info("Order new take profit is set to " + takeProfit);
		if (State.ORDER_POSTED.equals(state)) {
			if (accountService.updateOrder(order) == null) {
				log.info("A problem occurred during updating the trade. Resetting state...");
				resetState(true);
			}
		}
	}

	private double getOrderPrice() {
		int direction = getMarketDirection();
		Candle confirmed5M = mainDao.getLastFractal(Step.M5, instrument, getMarketDirection());
		if (direction == DIRECTION_UP) {
			return confirmed5M.getHighBid();
		} else if (direction == DIRECTION_DOWN) {
			return confirmed5M.getLowBid();
		} else {
			return 0;
		}
	}

	@Override
	public void preStart() throws Exception {
		super.preStart();
		heartbeat = config.hasPath("heartbeat") && config.getBoolean("heartbeat");
	}

	private void reportState() {
		log.info(String.format(
				"Current state: %s\n" + "Trend: %s\n" + "Fractals:\n" + "    opp confirmed: %b\n"
						+ "    opp broken: %b\n" + "    trend-confirmed: %b;\n",
				state.toString(), getMarketDirection() == DIRECTION_UP ? "up" : "down", confirmedOpp5M != null,
				brokenOpp5M != null, confirmed5M != null));
	}

}
