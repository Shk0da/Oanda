package com.oanda.bot.actor.strategy;

import com.oanda.bot.StrategySteps;
import com.oanda.bot.actor.abstracts.AbstractInstrumentActor;
import com.oanda.bot.constants.Event;
import com.oanda.bot.constants.Step;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Candle;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.model.Order;
import com.oanda.bot.model.Price;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.strategies.IchimokuCloudTradingStrategy;
import com.oanda.bot.util.DateTimeUtil;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import jersey.repackaged.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class IchimokuStrategyActor extends AbstractInstrumentActor {

    private final static int DAYS_BACK = 365;
    private final static int PIPS = 3;

    @Autowired
    private AccountService accountService;

    @Autowired
    private MainDao mainDao;

    @Autowired
    private StrategySteps steps;

    private TimeSeries series;
    private Strategy strategy;
    private TradingRecord tradingRecord;
    private Candle currentRate;
    private Order order = new Order();

    public IchimokuStrategyActor(Instrument instrument) {
        super(instrument);
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();

        series = new TimeSeries(instrument.getName(), getTicks(instrument, steps.tradingStep(), DAYS_BACK));
        strategy = IchimokuCloudTradingStrategy.buildStrategy(series);
        tradingRecord = new TradingRecord();
    }

    @Override
    public void onReceive(Object msg) throws Exception {
        if (msg instanceof Event.CurrentRate) {
            setCurrentRate(((Event.CurrentRate) msg).getCandle());
            trade();
        }
    }

    private void setCurrentRate(Candle rate) {
        this.currentRate = rate;
    }

    private void trade() throws Exception {
        Tick newTick = new Tick(
                new DateTime(currentRate.getDateTime()),
                currentRate.getOpenMid(),
                currentRate.getHighMid(),
                currentRate.getLowMid(),
                currentRate.getCloseMid(),
                currentRate.getVolume()
        );

        if (!newTick.getEndTime().isAfter(series.getLastTick().getEndTime())) {
            return;
        }

        series.addTick(newTick);
        log.info(String.format("Tick added in %s series, close price is %.5f", instrument.getDisplayName(), newTick.getClosePrice().toDouble()));
        int endIndex = series.getEnd();

        try {
            if (strategy.shouldEnter(endIndex)) {
                if (tradingRecord.enter(endIndex)) {
                    currentRate.setDirection(DIRECTION_UP);
                    initOrder();
                }
            } else if (strategy.shouldExit(endIndex)) {
                if (tradingRecord.exit(endIndex)) {
                    currentRate.setDirection(DIRECTION_DOWN);
                    initOrder();
                }
            }
        } catch (Exception ex) {
            log.info("..." + ex.getLocalizedMessage());
        }
    }

    private void initOrder() {
        log.info("Initializing new order...");
        log.info("Current rate is " + currentRate.getCloseMid());
        order = (order != null && order.getId() != null) ? order : new Order();
        //GTC	The Order is “Good unTil Cancelled”
        //GTD	The Order is “Good unTil Date” and will be cancelled at the provided time
        //GFD	The Order is “Good For Day” and will be cancelled at 5pm New York time
        //FOK	The Order must be immediately “Filled Or Killed”
        //IOC	The Order must be “Immediatedly paritally filled Or Cancelled”
        order.setTimeInForce(Order.TimeInForce.GTD);
        order.setCancelledTime(DateTimeUtil.rfc3339(DateTime.now(DateTimeZone.getDefault()).plusDays(7)));
        order.setGtdTime(DateTimeUtil.rfc3339(DateTime.now(DateTimeZone.getDefault()).plusDays(7)));
        order.setInstrument(instrument.toString());
        order.setPositionFill(Order.OrderPositionFill.DEFAULT);
        order.setType(Order.OrderType.LIMIT);
        log.info("Order side is set to " + order.getType());
        double balance = accountService.getAccountDetails().getBalance();
        int units = (int) (balance / 100 * 1000);
        order.setUnits(getMarketDirection() == DIRECTION_UP ? Math.abs(units) : Math.abs(units) * (-1));
        log.info("Order units is set to " + order.getUnits());
        updateOrder();

        if (order.getId() != null) {
            order = accountService.updateOrder(order);
        } else {
            order = accountService.createOrder(order);
        }

        if (order == null) {
            log.info("Resetting state: closing trades and orders ...");
            currentRate = null;
            order = new Order();
            accountService.closeOrdersAndTrades(instrument);
        } else {
            log.info("Created: " + order.toString());
            mainDao.insertOrder(order, instrument);
        }
    }

    private void updateOrder() {
        int direction = getMarketDirection();
        double newStopLoss = getOrderStopLoss();
        double newPrice = getOrderPrice();
        double oldStopLoss = order.getStopLossOnFill() != null ? order.getStopLossOnFill().getPrice() : 0;
        double takeProfit = getOrderTakeProfit(newPrice);
        boolean changed = false;
        if (order.getPrice() != newPrice) {
            order.setPrice(newPrice);
            if (order.getTakeProfitOnFill().getPrice() != takeProfit) {
                order.setTakeProfitOnFill(new Order.Details(takeProfit));
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
            order.setStopLossOnFill(new Order.Details(newStopLoss));
        }
    }

    private int getMarketDirection() {
        return currentRate.getDirection();
    }

    private double getOrderPrice() {
        return getMarketDirection() == DIRECTION_UP ? currentRate.getBid().getH() : currentRate.getAsk().getL();
    }

    private double getOrderStopLoss() {
        return getMarketDirection() == DIRECTION_UP
                ? getOrderPrice() - instrument.getPip()
                : getOrderPrice() + instrument.getPip();
    }

    private double getOrderTakeProfit(double newPrice) {
        Price price = accountService.getPrice(instrument);
        log.info(String.format("Current spread is %.5f", price.getSpread()));

        int pips = PIPS;
        try {
            pips = config.getInt("takeprofit");
        } catch (Exception e) {
            log.info("Failed to read takeprofit value from config. Setting default takeprofit ({} pips)", pips);
        }

        return getMarketDirection() == DIRECTION_UP
                ? newPrice + price.getSpread() + pips * instrument.getPip()
                : newPrice - price.getSpread() - pips * instrument.getPip();
    }

    private List<Tick> getTicks(Instrument instrument, Step step, int daysBack) throws Exception {
        List<Candle> candles = Lists.newArrayList();

        int ichiIndex = 26;
        for (int d = daysBack; d > 0; d = d - ichiIndex) {
            candles.addAll(mainDao.getWhereTimeCandle(
                    step,
                    instrument,
                    DateTime.now(DateTimeZone.getDefault()).minusDays(d).toDate(),
                    DateTime.now(DateTimeZone.getDefault()).minusDays((d - ichiIndex) >= 0 ? (d - ichiIndex) : 0).toDate()
            ));
        }

        List<Tick> ticks = Lists.newArrayList();
        candles.forEach(candle ->
                ticks.add(new Tick(
                        new DateTime(candle.getDateTime()),
                        candle.getOpenMid(),
                        candle.getHighMid(),
                        candle.getLowMid(),
                        candle.getCloseMid(),
                        candle.getVolume()
                ))
        );

        return ticks;
    }
}
