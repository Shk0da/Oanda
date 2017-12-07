package com.oanda.bot.actor;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Iterables;
import com.oanda.bot.domain.*;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.repository.InstrumentRepository;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.util.DateTimeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Scope("prototype")
@Component("TradeActor")
public class TradeActor extends UntypedAbstractActor {

    private enum OrderType {BUY, SELL}

    private enum Signal {UP, DOWN, NONE}

    protected final Instrument instrument;
    protected final Step step;

    private ActorRef learnActor;
    private ActorRef collectorActor;

    @Setter
    @Getter
    private boolean workTime = true;

    @Setter
    @Getter
    private Candle currentRate;

    @Setter
    private Order currentOrder;

    @Value("${oandabot.takeprofit}")
    private Double takeProfit;

    @Value("${oandabot.stoploss}")
    private Double stopLoss;

    @Value("${oandabot.spread.max}")
    private Double spreadMax;

    @Value("${oandabot.balance.limit}")
    private Double balanceLimit;

    @Value("${oandabot.balance.risk}")
    private Double balanceRisk;

    @Value("${oandabot.trailingstop.enable}")
    private Boolean trailingStopEnable;

    @Value("${oandabot.trailingstop.onlyprofit}")
    private Boolean trailingStopOnlyProfit;

    @Value("${oandabot.trailingstop.val}")
    private Double trailingStopVal;

    @Value("${oandabot.trailingstop.step}")
    private Double trailingStopStep;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private InstrumentRepository instrumentRepository;

    @Autowired
    private AccountService accountService;

    public TradeActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
    }

    @Override
    public void preStart() {
        collectorActor = getContext().actorOf(
                Props.create(SpringDIActor.class, CollectorActor.class, instrument, step), "CollectorActor_" + instrument.getInstrument() + "_" + step.name()
        );
        log.info("TradeActor make CollectorActor_" + instrument.getInstrument() + "_" + step.name());
        learnActor = getContext().actorOf(
                Props.create(SpringDIActor.class, LearnActor.class, instrument, step), "LearnActor_" + instrument.getInstrument() + "_" + step.name()
        );
        log.info("TradeActor make LearnActor_" + instrument.getInstrument() + "_" + step.name());
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof Messages.WorkTime) {
            setWorkTime(((Messages.WorkTime) message).is);
            log.info("Now workTime is {}", isWorkTime());
        }

        if (!isActive()) return;

        if (message instanceof Candle) {
            setCurrentRate(((Candle) message));
            log.info("CurrentRate: {}", getCurrentRate());
            learnActor.tell(message, self());
            trailingPositions();
        }

        if (message instanceof Messages.Predict) {
            log.info("Current {}: {}", instrument.getDisplayName(), getCurrentRate().getMid().getC());
            log.info("Predict {}: {}", instrument.getDisplayName(), ((Messages.Predict) message).getPrice());
            trade((Messages.Predict) message);
        }

        unhandled(message);
    }

    private void trade(final Messages.Predict predict) {
        Signal signal = signal(predict);
        if (Signal.NONE.equals(signal)) return;

        log.info("We have new signal: {}", signal);

        Price price = accountService.getPrice(instrument);

        Order order = getCurrentOrder();
        if (order.getId() != null) {
            double profit = getProfit();
            double satisfactorilyTP = ((price.getBid() + price.getAsk()) / 2 - takeProfit) * instrument.getPip();
            boolean trendChanged = (Signal.UP.equals(signal) && order.getUnits() < 0) || (Signal.DOWN.equals(signal) && order.getUnits() > 0);

            if (profit > satisfactorilyTP || trendChanged && profit >= 0) {
                accountService.closeOrdersAndTrades(instrument);
                log.info("{}: Close orders and trades", instrument.getDisplayName());
                order = new Order();
            }
        }

        List<Trade> trades = getTrades();
        if (!trades.isEmpty()) {
            log.info("We have active trades: {}. Waiting...", trades.size());
            return;
        }

        if (order.getId() == null && price.getSpread() <= spreadMax) {
            if (Signal.UP.equals(signal)) {
                order.setTakeProfitOnFill(new Order.Details(getTakeProfit(OrderType.BUY)));
                order.setStopLossOnFill(new Order.Details(getStopLoss(OrderType.BUY)));
                order.setPrice(getOrderPrice(OrderType.BUY));
                order.setUnits(getMaxUnits(OrderType.BUY));
            }

            if (Signal.DOWN.equals(signal)) {
                order.setTakeProfitOnFill(new Order.Details(getTakeProfit(OrderType.SELL)));
                order.setStopLossOnFill(new Order.Details(getStopLoss(OrderType.SELL)));
                order.setPrice(getOrderPrice(OrderType.SELL));
                order.setUnits(getMaxUnits(OrderType.SELL));
            }

            sendOrder(order);
        }
    }

    private void sendOrder(Order order) {
        //GTC	The Order is “Good unTil Cancelled”
        //GTD	The Order is “Good unTil Date” and will be cancelled at the provided time
        //GFD	The Order is “Good For Day” and will be cancelled at 5pm New York time
        //FOK	The Order must be immediately “Filled Or Killed”
        //IOC	The Order must be “Immediatedly paritally filled Or Cancelled”
        order.setTimeInForce(Order.TimeInForce.IOC);
        order.setCancelledTime(DateTimeUtil.rfc3339(DateTime.now(DateTimeZone.getDefault()).plusDays(1)));
        order.setGtdTime(DateTimeUtil.rfc3339(DateTime.now(DateTimeZone.getDefault()).plusDays(1)));
        order.setInstrument(instrument.toString());
        order.setPositionFill(Order.OrderPositionFill.DEFAULT);
        order.setType(Order.OrderType.MARKET);

        if (order.getId() != null) {
            order = accountService.updateOrder(order);
        } else {
            order = accountService.createOrder(order);
        }

        if (order == null) {
            log.info("Resetting state: closing trades and orders ...");
            currentRate = null;
            setCurrentOrder(null);
            accountService.closeOrdersAndTrades(instrument);
        } else {
            setCurrentOrder(order);
            log.info("Created: {}", order);
        }
    }

    private Order getCurrentOrder() {
        if (currentOrder == null) {
            Trade.Trades trades = accountService.getTrades(instrument);
            if (trades.getTrades().isEmpty()) return new Order();

            Trade lastTrade = Iterables.getLast(trades.getTrades());
            Order orderFromTrade = new Order();
            orderFromTrade.setId(lastTrade.getId());
            orderFromTrade.setInstrument(lastTrade.getInstrument());
            orderFromTrade.setPrice(lastTrade.getPrice());
            orderFromTrade.setUnits(lastTrade.getCurrentUnits());
            currentOrder = orderFromTrade;
        }

        return currentOrder;
    }

    private List<Trade> getTrades() {
        return accountService.getTrades(instrument).getTrades();
    }

    private double getOrderPrice(OrderType type) {
        if (currentRate == null) {
            currentRate = candleRepository.getLastCandle(instrument, step);
        }

        return OrderType.BUY.equals(type) ? currentRate.getBid().getH() : currentRate.getAsk().getL();
    }

    private Double getProfit() {
        double profit = 0;
        Trade.Trades trades = accountService.getTrades(instrument);
        for (Trade trade : trades.getTrades()) {
            profit += trade.getUnrealizedPL();
        }

        log.info("Profit {}: {}", instrument.getDisplayName(), profit);
        return profit;
    }

    private Integer getMaxUnits(OrderType type) {
        double balance = accountService.getAccountDetails().getBalance();
        int units = (int) (balance / 100 * (1000 * balanceRisk));
        return OrderType.BUY.equals(type) ? Math.abs(units) : Math.abs(units) * (-1);
    }

    private Double getTakeProfit(OrderType type) {
        double takeProfit = 0;
        Price price = accountService.getPrice(instrument);
        double ask = price.getAsk();
        double bid = price.getBid();
        double spread = price.getSpread();

        if (OrderType.BUY.equals(type)) {
            takeProfit = ask + spread + this.takeProfit * instrument.getPip();
        }

        if (OrderType.SELL.equals(type)) {
            takeProfit = bid - spread - this.takeProfit * instrument.getPip();
        }

        return takeProfit;
    }

    private Double getStopLoss(OrderType type) {
        double stopLoss = 0;
        Price price = accountService.getPrice(instrument);
        double ask = price.getAsk();
        double bid = price.getBid();

        if (OrderType.BUY.equals(type)) {
            stopLoss = ask - this.stopLoss * instrument.getPip();
        }

        if (OrderType.SELL.equals(type)) {
            stopLoss = bid + this.stopLoss * instrument.getPip();
        }

        return stopLoss;
    }

    private Signal signal(Messages.Predict predict) {
        Price price = accountService.getPrice(instrument);
        double ask = price.getAsk();
        double bid = price.getBid();
        double spread = ask - bid;
        double predictPrice = predict.getPrice();

        if (predictPrice > 0 && predictPrice > ask + spread) return Signal.UP;
        if (predictPrice > 0 && predictPrice < bid - spread) return Signal.DOWN;

        return Signal.NONE;
    }

    private Boolean isActive() {
        double accountBalance = accountService.getAccountDetails().getBalance();
        log.info("Balance: {}", accountBalance);
        return isWorkTime() && (accountBalance > balanceLimit);
    }

    private void trailingPositions() {
        if (currentRate == null || getCurrentOrder().getId() == null) return;

        Order.Orders orders = accountService.getOrders(instrument);
        orders.getOrders().forEach(order -> {
            if (Order.OrderType.STOP_LOSS.equals(order.getType())) {
                trailingSL(order);
            }

            if (Order.OrderType.TAKE_PROFIT.equals(order.getType())) {
                trailingTP(order);
            }
        });
    }

    private void trailingSL(final Order order) {
        if (!trailingStopEnable) return;

        OrderType type = getCurrentOrder().getUnits() > 0 ? OrderType.BUY : OrderType.SELL;
        double currentStopLoss = order.getPrice();

        if (OrderType.BUY.equals(type) && (currentRate.getBid().getC() - getCurrentOrder().getPrice()) > trailingStopVal * instrument.getPip()) {
            if (!trailingStopOnlyProfit
                    || currentStopLoss < currentRate.getBid().getC() - (trailingStopVal + trailingStopStep - 1) * instrument.getPip()) {
                double newStopLoss = currentRate.getBid().getC() - trailingStopVal * instrument.getPip();
                order.setPrice(newStopLoss);
                accountService.updateOrder(order);
                log.info("{}, change sl: {} -> {}", order.getId(), currentStopLoss, newStopLoss);
            }
        }

        if (OrderType.SELL.equals(type) && (getCurrentOrder().getPrice() - currentRate.getAsk().getC()) > trailingStopVal * instrument.getPip()) {
            if (!trailingStopOnlyProfit
                    || currentStopLoss > currentRate.getAsk().getC() + (trailingStopVal + trailingStopStep - 1) * instrument.getPip()) {
                double newStopLoss = currentRate.getAsk().getC() + trailingStopVal * instrument.getPip();
                order.setPrice(newStopLoss);
                accountService.updateOrder(order);
                log.info("{}, change sl: {} -> {}", order.getId(), currentStopLoss, newStopLoss);
            }
        }
    }

    private void trailingTP(final Order order) {
        if (!trailingStopEnable) return;

        OrderType type = order.getUnits() > 0 ? OrderType.BUY : OrderType.SELL;
        double currentTakeProfit = order.getPrice();
        double newTakeProfit = currentTakeProfit + takeProfit * instrument.getPip() * (OrderType.BUY.equals(type) ? 1 : -1);
        double mid = (currentRate.getAsk().getC() + currentRate.getBid().getC()) / 2;

        if (currentTakeProfit != newTakeProfit
                && (OrderType.SELL.equals(type) && getCurrentOrder().getPrice() > (mid + takeProfit * instrument.getPip())
                || OrderType.BUY.equals(type) && getCurrentOrder().getPrice() < (mid - takeProfit * instrument.getPip()))
                ) {
            order.setPrice(newTakeProfit);
            accountService.updateOrder(order);
            log.info("{}, change tp: {} -> {}", order.getId(), currentTakeProfit, newTakeProfit);
        }
    }
}
