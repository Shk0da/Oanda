package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Iterables;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.*;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.util.DateTimeUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Precision;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.oanda.bot.actor.Messages.Predict.Signal;

@Slf4j
@Scope("prototype")
@Component("TradeActor")
public class TradeActor extends UntypedAbstractActor {

    private enum OrderType {
        BUY, SELL
    }

    protected final Instrument instrument;
    protected final Step step;

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

    @Value("${oandabot.takeprofit.enable}")
    private Boolean takeProfitEnable;

    @Value("${oandabot.stoploss.enable}")
    private Boolean stopLossEnable;

    @Value("${oandabot.satisfactorilyprofit}")
    private Double satisfactorilyProfit;

    @Value("${oandabot.spread.max}")
    private Double spreadMax;

    @Value("${oandabot.balance.limit}")
    private Double balanceLimit;

    @Value("${oandabot.balance.risk}")
    private Double balanceRisk;

    @Value("${oandabot.pricebound.enable}")
    private Boolean priceboundEnable;

    @Value("${oandabot.lossorders.close}")
    private Boolean lossOrdersClose;

    @Value("${oandabot.trailingstop.enable}")
    private Boolean trailingStopEnable;

    @Value("${oandabot.trailingstop.distance}")
    private Double trailingStopDistance;

    @Getter
    @Setter
    private Double lastProfit = 0D;

    @Autowired
    private CandleRepository candleRepository;

    @Autowired
    private AccountService accountService;

    public TradeActor(Instrument instrument, Step step) {
        this.instrument = instrument;
        this.step = step;
    }

    @Override
    public void preStart() {
        balanceInfo();
    }

    @Override
    public void onReceive(Object message) {
        try {
            if (message instanceof Messages.WorkTime) {
                setWorkTime(((Messages.WorkTime) message).getIs());
                log.warn("Now workTime is {}", isWorkTime());
            }

            if (!isActive())
                return;

            if (Messages.WORK.equals(message)) {
                checkProfit();
            }

            if (message instanceof Candle) {
                setCurrentRate(((Candle) message));
                log.info("CurrentRate {}: {}", instrument, getCurrentRate().getCloseMid());
                getContext().actorSelection(
                        ActorConfig.ACTOR_PATH_HEAD + "LearnActor_" + instrument.getInstrument() + "_" + step.name())
                        .tell(Messages.PREDICT, self());
            }

            if (message instanceof Messages.Predict) {
                log.info("Predict {}: {}", instrument.getDisplayName(), ((Messages.Predict) message).getTrend());
                trade((Messages.Predict) message);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }

        unhandled(message);
    }

    private void balanceInfo() {
        log.warn("Balance: {}", accountService.getAccountDetails().getBalance());
    }

    @Async
    protected void checkProfit() {
        double profit = Precision.round(getProfit(), 5);
        if (profit >= getSatisfactorilyProfit()) {
            toTakeProfit(profit);
            return;
        }

        Order current = getCurrentOrder();
        if (current == null || current.getPrice() == null)
            return;

        Price price = accountService.getPrice(instrument);
        double currentTP = current.getTakeProfit();
        if (currentTP == 0) {
            if (current.getUnits() > 0) {
                currentTP = price.getAsk() + price.getSpread() + takeProfit * instrument.getPip();
            }

            if (current.getUnits() < 0) {
                currentTP = price.getBid() - price.getSpread() - takeProfit * instrument.getPip();
            }
        }

        double satisfactorilyTP = (currentTP + current.getPrice()) / 2;
        double midPrice = (price.getBid() + price.getAsk()) / 2;
        if (profit > 0 && midPrice > satisfactorilyTP) {
            toTakeProfit(profit);
        }
    }

    private void toTakeProfit(double profit) {
        lastProfit = profit;
        log.warn("Profit {}: {}", instrument.getDisplayName(), profit);
        accountService.closeOrdersAndTrades(instrument);
        setCurrentOrder(null);
        log.info("{}: Close orders and trades", instrument.getDisplayName());
        balanceInfo();
    }

    private void trade(final Messages.Predict predict) {
        Signal signal = predict.getTrend();
        if (Signal.NONE.equals(signal))
            return;

        log.info("We have new {} signal: {}", instrument.getDisplayName(), signal);

        Order order = getCurrentOrder();
        if (order.getId() != null) {
            boolean trendChanged = (Signal.UP.equals(signal) && order.getUnits() < 0)
                    || (Signal.DOWN.equals(signal) && order.getUnits() > 0);
            if (trendChanged) {
                log.warn("The {} trend has changed: {} -> {}", instrument.getDisplayName(),
                        Signal.UP.equals(signal) ? Signal.DOWN : Signal.UP, signal);
                double profit = getProfit();
                log.warn("Profit {}: {}", instrument.getDisplayName(), profit);
                log.warn("The closure of unprofitable orders is {}", lossOrdersClose);
                if (profit >= 0 || lossOrdersClose) {
                    lastProfit = profit;
                    accountService.closeOrdersAndTrades(instrument);
                    setCurrentOrder(null);
                    log.info("{}: Close orders and trades", instrument.getDisplayName());
                    balanceInfo();
                }
            }
        }

        List<Trade> trades = getTrades();
        if (!trades.isEmpty()) {
            log.info("We have {} {} active trades:", trades.size(), instrument.getDisplayName());
            trades.forEach(trade -> log.info("Trade {}: [price: {}, units(c): {}, units(i): {}]", trade.getId(),
                    trade.getPrice(), trade.getCurrentUnits(), trade.getInitialUnits()));
            log.info("Profit {}: {}", instrument.getDisplayName(), getProfit());
            log.info("Waiting...");
            return;
        }

        double currentSpread = Precision.round(accountService.getPrice(instrument).getSpread(), 5);
        double maxSpread = Precision.round(spreadMax * instrument.getPip(), 5);
        log.info("Spread {}: {}. Max spread: {}", instrument.getDisplayName(), currentSpread, maxSpread);
        if (currentSpread > maxSpread)
            return;

        Order newOrder = getCurrentOrder();
        if (Signal.UP.equals(signal)) {
            if (takeProfitEnable) {
                newOrder.setTakeProfitOnFill(new Order.Details(getTakeProfit(OrderType.BUY)));
            }

            if (stopLossEnable) {
                newOrder.setStopLossOnFill(new Order.Details(getStopLoss(OrderType.BUY)));
            }

            if (priceboundEnable) {
                newOrder.setPrice(getOrderPrice(OrderType.BUY));
                newOrder.setPriceBound(getOrderPrice(OrderType.BUY));
            }

            newOrder.setUnits(getMaxUnits(OrderType.BUY));
        }

        if (Signal.DOWN.equals(signal)) {
            if (takeProfitEnable) {
                newOrder.setTakeProfitOnFill(new Order.Details(getTakeProfit(OrderType.SELL)));
            }

            if (stopLossEnable) {
                newOrder.setStopLossOnFill(new Order.Details(getStopLoss(OrderType.SELL)));
            }

            if (priceboundEnable) {
                newOrder.setPrice(getOrderPrice(OrderType.SELL));
                newOrder.setPriceBound(getOrderPrice(OrderType.SELL));
            }

            newOrder.setUnits(getMaxUnits(OrderType.SELL));
        }

        sendOrder(newOrder);
    }

    private void sendOrder(Order order) {
        // GTC The Order is “Good unTil Cancelled”
        // GTD The Order is “Good unTil Date” and will be cancelled at the provided
        // time
        // GFD The Order is “Good For Day” and will be cancelled at 5pm New York
        // time
        // FOK The Order must be immediately “Filled Or Killed”
        // IOC The Order must be “Immediatedly paritally filled Or Cancelled”
        order.setTimeInForce(Order.TimeInForce.IOC);
        order.setCancelledTime(DateTimeUtil.rfc3339(DateTime.now(DateTimeZone.getDefault()).plusDays(1)));
        order.setGtdTime(DateTimeUtil.rfc3339(DateTime.now(DateTimeZone.getDefault()).plusDays(1)));
        order.setInstrument(instrument.toString());
        order.setPositionFill(Order.OrderPositionFill.DEFAULT);
        order.setType(Order.OrderType.MARKET);

        if (trailingStopEnable) {
            order.setTrailingStopLossOnFill(
                    new Order.TrailingStopLossDetails(trailingStopDistance * instrument.getPip()));
        }

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
            if (trades.getTrades().isEmpty())
                return new Order();

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

        return OrderType.BUY.equals(type) ? currentRate.getAsk().getL() : currentRate.getBid().getH();
    }

    private Double getProfit() {
        double profit = 0;
        Trade.Trades trades = accountService.getTrades(instrument);
        for (Trade trade : trades.getTrades()) {
            profit += trade.getUnrealizedPL();
        }

        return profit;
    }

    private Double getSatisfactorilyProfit() {
        return accountService.getAccountDetails().getBalance() / 100 * satisfactorilyProfit;
    }

    private Integer getMaxUnits(OrderType type) {
        double balance = accountService.getAccountDetails().getBalance();
        int units = (int) (balance / getOrderPrice(type) * (balanceRisk * 0.01));

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

    private Boolean isActive() {
        if (!isWorkTime())
            return false;
        double accountBalance = accountService.getAccountDetails().getBalance();
        return accountBalance > balanceLimit;
    }
}
