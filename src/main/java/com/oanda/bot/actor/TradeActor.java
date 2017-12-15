package com.oanda.bot.actor;

import akka.actor.UntypedAbstractActor;
import com.google.common.collect.Iterables;
import com.oanda.bot.config.ActorConfig;
import com.oanda.bot.domain.*;
import com.oanda.bot.repository.CandleRepository;
import com.oanda.bot.service.AccountService;
import com.oanda.bot.util.DateTimeUtil;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
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

    private final Core talib = new Core();

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

    @Value("${oandabot.spread.max}")
    private Double spreadMax;

    @Value("${oandabot.balance.limit}")
    private Double balanceLimit;

    @Value("${oandabot.balance.risk}")
    private Double balanceRisk;

    @Value("${oandabot.lossorders.close}")
    private Boolean lossOrdersClose;

    @Value("${oandabot.trailingstop.enable}")
    private Boolean trailingStopEnable;

    @Value("${oandabot.trailingstop.distance}")
    private Double trailingStopDistance;

    @Value("${oandabot.martingale.enable}")
    private Boolean martingaleEnable;

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
        if (message instanceof Messages.WorkTime) {
            setWorkTime(((Messages.WorkTime) message).getIs());
            log.info("Now workTime is {}", isWorkTime());
        }

        if (!isActive()) return;

        if (Messages.WORK.equals(message)) {
            checkProfit();
        }

        if (message instanceof Candle) {
            setCurrentRate(((Candle) message));
            log.info("CurrentRate: {}", getCurrentRate());
            getContext()
                    .actorSelection(ActorConfig.ACTOR_PATH_HEAD + "LearnActor_" + instrument.getInstrument() + "_" + step.name())
                    .tell(message, self());
        }

        if (message instanceof Messages.Predict) {
            log.info("Current {}: {}", instrument.getDisplayName(), getCurrentRate().getMid().getC());
            log.info("Predict {}: {}", instrument.getDisplayName(), ((Messages.Predict) message).getPrice());
            trade((Messages.Predict) message);
        }

        unhandled(message);
    }

    private void balanceInfo() {
        log.warn("Balance: {}", accountService.getAccountDetails().getBalance());
    }

    private void checkProfit() {
        Order current = getCurrentOrder();
        if (current == null || current.getPrice() == null) return;

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
        double profit = getProfit();

        if (profit > 0 && midPrice > satisfactorilyTP) {
            lastProfit = profit;
            log.warn("Profit {}: {}", instrument.getDisplayName(), profit);
            accountService.closeOrdersAndTrades(instrument);
            setCurrentOrder(null);
            log.info("{}: Close orders and trades", instrument.getDisplayName());
            balanceInfo();
        }
    }

    private void trade(final Messages.Predict predict) {
        Signal signal = signal(predict);
        if (Signal.NONE.equals(signal)) return;

        log.info("We have new {} signal: {}", instrument.getDisplayName(), signal);

        Order order = getCurrentOrder();
        if (order.getId() != null) {
            boolean trendChanged = (Signal.UP.equals(signal) && order.getUnits() < 0)
                    || (Signal.DOWN.equals(signal) && order.getUnits() > 0);
            if (trendChanged) {
                log.warn("The {} trend has changed: {} -> {}",
                        instrument.getDisplayName(), Signal.UP.equals(signal) ? Signal.DOWN : Signal.UP, signal);
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
            trades.forEach(trade ->
                    log.info("Trade {}: [price: {}, units(c): {}, units(i): {}]",
                            trade.getId(), trade.getPrice(), trade.getCurrentUnits(), trade.getInitialUnits())
            );
            log.info("Profit {}: {}", instrument.getDisplayName(), getProfit());
            log.info("Waiting...");
            return;
        }

        Price price = accountService.getPrice(instrument);
        if (price.getSpread() <= spreadMax) {
            Order newOrder = getCurrentOrder();
            if (Signal.UP.equals(signal)) {
                newOrder.setTakeProfitOnFill(new Order.Details(getTakeProfit(OrderType.BUY)));
                newOrder.setStopLossOnFill(new Order.Details(getStopLoss(OrderType.BUY)));
                newOrder.setPrice(getOrderPrice(OrderType.BUY));
                newOrder.setPriceBound(getOrderPrice(OrderType.BUY));
                newOrder.setUnits(getMaxUnits(OrderType.BUY));
            }

            if (Signal.DOWN.equals(signal)) {
                newOrder.setTakeProfitOnFill(new Order.Details(getTakeProfit(OrderType.SELL)));
                newOrder.setStopLossOnFill(new Order.Details(getStopLoss(OrderType.SELL)));
                newOrder.setPrice(getOrderPrice(OrderType.SELL));
                newOrder.setPriceBound(getOrderPrice(OrderType.SELL));
                newOrder.setUnits(getMaxUnits(OrderType.SELL));
            }

            sendOrder(newOrder);
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

        if (trailingStopEnable) {
            order.setTrailingStopLossOnFill(new Order.TrailingStopLossDetails(trailingStopDistance * instrument.getPip()));
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
            log.warn("Created: {}", order);
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

    private Integer getMaxUnits(OrderType type) {
        double balance = accountService.getAccountDetails().getBalance();
        int units = (int) (balance / 100 * (1000 * balanceRisk)) / 10;
        if (martingaleEnable && lastProfit < 0) {
            units = units * 2;
        }

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
        List<Candle> dayCandles = candleRepository.getLastCandles(instrument, step, 127);
        double[] inHigh = new double[dayCandles.size()];
        double[] inLow = new double[dayCandles.size()];
        double[] inClose = new double[dayCandles.size()];
        for (int i = 0; i < dayCandles.size(); i++) {
            Candle candle = dayCandles.get(i);
            inHigh[i] = candle.getHighMid();
            inLow[i] = candle.getLowMid();
            inClose[i] = candle.getCloseMid();
        }

        double predictPrice = predict.getPrice();
        double rsi = rsi(inClose);
        double adx = adx(inClose, inLow, inHigh);
        double white = movingAverageWhite(inClose);
        double black = movingAverageBlack(inClose);
        double imalow = movingAverageLowHigh(inLow);
        double imahigh = movingAverageLowHigh(inHigh);
        boolean tdwn = rsi < 39 && adx > 19;
        boolean tup = rsi > 59 && adx > 25;

        Signal signal = Signal.NONE;
        if (predictPrice <= imalow && tdwn && predictPrice <= white && predictPrice >= black && black < white) {
            signal = Signal.DOWN;
        }

        if (predictPrice > imahigh && tup && predictPrice > white && predictPrice < black && black > white) {
            signal = Signal.UP;
        }

        return signal;
    }

    private double adx(double[] inClose, double[] inLow, double[] inHigh) {
        double[] outADX = new double[inClose.length];
        MInteger beginADX = new MInteger();
        MInteger lengthADX = new MInteger();
        RetCode retCodeADX = talib.adx(
                0, inClose.length - 1,
                inHigh, inLow, inClose, 14,
                beginADX, lengthADX, outADX
        );

        double adx = 0;
        if (RetCode.Success.equals(retCodeADX)) {
            adx = outADX[lengthADX.value - 1];
        }

        log.info("ADX {}: {}", instrument.getDisplayName(), adx);
        return adx;
    }

    private double rsi(double[] in) {
        int period = 14;

        double[] out = new double[in.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        RetCode retCode = talib.rsi(
                0, in.length - 1, in, period, begin, length, out
        );
        double rsi = RetCode.Success.equals(retCode) ? out[length.value - 1] : 0;

        log.info("RSI {}: {}", instrument.getDisplayName(), rsi);
        return rsi;
    }

    private double movingAverageLowHigh(double[] in) {
        int period = 3;

        double[] out = new double[in.length];
        MInteger begin = new MInteger();
        MInteger length = new MInteger();
        RetCode retCode = talib.movingAverage(
                0, in.length - 1, in, period, MAType.Wma, begin, length, out
        );
        double ma = RetCode.Success.equals(retCode) ? out[length.value - 1] : 0;

        log.info("MovingAverageLowHigh {}: {}", instrument.getDisplayName(), ma);
        return ma;
    }

    private double movingAverageBlack(double[] inClose) {
        int blackPeriod = 63;

        double[] outBlackMA = new double[inClose.length];
        MInteger beginBlackMA = new MInteger();
        MInteger lengthBlackMA = new MInteger();
        RetCode retCodeBlackMA = talib.trima(
                0, inClose.length - 1, inClose, blackPeriod, beginBlackMA, lengthBlackMA, outBlackMA
        );
        double blackMA = RetCode.Success.equals(retCodeBlackMA) ? outBlackMA[lengthBlackMA.value - 1] : 0;

        log.info("MovingAverageBlack {}: {}", instrument.getDisplayName(), blackMA);
        return blackMA;
    }

    private double movingAverageWhite(double[] inClose) {
        int whitePeriod = 7;

        double[] outWhiteMA = new double[inClose.length];
        MInteger beginWhiteMA = new MInteger();
        MInteger lengthWhiteMA = new MInteger();
        RetCode retCodeWhiteMA = talib.trima(
                0, inClose.length - 1, inClose, whitePeriod, beginWhiteMA, lengthWhiteMA, outWhiteMA
        );
        double whiteMA = RetCode.Success.equals(retCodeWhiteMA) ? outWhiteMA[lengthWhiteMA.value - 1] : 0;

        log.info("MovingAverageWhite {}: {}", instrument.getDisplayName(), whiteMA);
        return whiteMA;
    }

    private Boolean isActive() {
        double accountBalance = accountService.getAccountDetails().getBalance();
        return isWorkTime() && (accountBalance > balanceLimit);
    }
}
