package com.oanda.bot.strategies;

import com.oanda.bot.strategies.enums.MovingAverageType;
import com.oanda.bot.strategies.indicators.EMAIndicator;
import com.oanda.bot.strategies.indicators.WilderRSIIndicator;
import com.oanda.bot.strategies.parameters.RobotParameters;
import com.oanda.bot.strategies.parameters.daytrade.DayTradeParameters;
import com.oanda.bot.strategies.parameters.entry.BollingerBandsParameters;
import com.oanda.bot.strategies.parameters.entry.EntryParameters;
import com.oanda.bot.strategies.parameters.entry.MovingAverageParameters;
import com.oanda.bot.strategies.parameters.entry.RSIParameters;
import com.oanda.bot.strategies.parameters.exit.ExitParameters;
import com.oanda.bot.strategies.parameters.exit.FixedStopGainParameters;
import com.oanda.bot.strategies.parameters.exit.FixedStopLossParameters;
import com.oanda.bot.strategies.parameters.exit.TrailingStopGainParameters;
import com.oanda.bot.strategies.rules.AllowOpenRule;
import com.oanda.bot.strategies.rules.ForceCloseRule;
import com.oanda.bot.strategies.rules.NoExitRule;
import com.oanda.bot.strategies.rules.stops.FixedStopGainRule;
import com.oanda.bot.strategies.rules.stops.FixedStopLossRule;
import com.oanda.bot.strategies.rules.stops.TrailingStopRule;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.statistics.StandardDeviationIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollinger.BollingerBandsLowerIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollinger.BollingerBandsMiddleIndicator;
import eu.verdelhan.ta4j.indicators.trackers.bollinger.BollingerBandsUpperIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ichimoku.*;
import eu.verdelhan.ta4j.trading.rules.CrossedDownIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.CrossedUpIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;
import lombok.Data;

import java.time.LocalTime;
import java.util.Map;

@Data
public class SolyankaStrategy {

    private final ClosePriceIndicator prices;
    private final RobotParameters parameters;

    private final Strategy buyStrategy;
    private final Strategy sellStrategy;

    private Rule buyEntryRule;
    private Rule buyExitRule;
    private Rule sellEntryRule;
    private Rule sellExitRule;

    private FixedStopLossRule stopLossRule;
    private FixedStopGainRule stopGainRule;
    private TrailingStopRule trailingStopRule;

    public static Strategy buildStrategy(TimeSeries series, Map<String, Integer> params) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
        SolyankaStrategy rules = new SolyankaStrategy(closePrices, new RobotParameters(
                new EntryParameters(
                        new MovingAverageParameters(MovingAverageType.EXPONENTIAL, MovingAverageType.SIMPLE,
                                params.get("shortPeriodMA"), params.get("longPeriodMA")),
                        new RSIParameters(params.get("RSIPeriods"), params.get("RSILowerValue"), params.get("RSIUpperValue")),
                        new BollingerBandsParameters(params.get("bollingerBandsPeriod"), Decimal.valueOf(params.get("bollingerBandsFactor")))
                ),
                new ExitParameters(),
                null
        ));

        Rule exitRule = rules.getBuyExitRule()
                .or(rules.getSellExitRule())
                .or(getIchimokuCloudTradingStrategySellRules(series));

        Rule entryRule = rules.getBuyEntryRule()
                .or(rules.getSellEntryRule())
                .or(getIchimokuCloudTradingStrategyBuyRules(series));

        return new Strategy(entryRule, exitRule);
    }

    public static Strategy buildStrategy(TimeSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrices = new ClosePriceIndicator(series);
        SolyankaStrategy rules = new SolyankaStrategy(closePrices, new RobotParameters(
                new EntryParameters(
                        new MovingAverageParameters(MovingAverageType.EXPONENTIAL, MovingAverageType.SIMPLE, 5, 14),
                        new RSIParameters(6, 24, 33),
                        new BollingerBandsParameters(5, Decimal.valueOf(1))
                ),
                new ExitParameters(),
                null
        ));

        IchimokuCloudTradingStrategy.buildStrategy(series);

        Rule exitRule = rules.getBuyExitRule()
                .or(rules.getSellExitRule())
                .or(getIchimokuCloudTradingStrategySellRules(series));

        Rule entryRule = rules.getBuyEntryRule()
                .or(rules.getSellEntryRule())
                .or(getIchimokuCloudTradingStrategyBuyRules(series));

        return new Strategy(entryRule, exitRule);
    }

    public SolyankaStrategy(ClosePriceIndicator prices, RobotParameters parameters) {
        this.prices = prices;
        this.parameters = parameters;

        this.setRules();

        buyStrategy = new Strategy(buyEntryRule, buyExitRule);
        sellStrategy = new Strategy(sellEntryRule, sellExitRule);
    }

    private void setRules() {
        this.setEntryRules();
        this.setExitRules();
        this.setDayTradeRules();
    }

    private void setEntryRules() {
        this.setMovingAverageRules();
        this.setRSIRules();
        this.setBBRules();
    }

    private void setExitRules() {
        this.setFixedStopLoss();
        this.setFixedStopGain();
        this.setTrailingStop();
    }

    private static Rule getIchimokuCloudTradingStrategyBuyRules(TimeSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        IchimokuTenkanSenIndicator tenkanSenIndicator = new IchimokuTenkanSenIndicator(series);
        IchimokuKijunSenIndicator kijunSenIndicator = new IchimokuKijunSenIndicator(series);
        IchimokuSenkouSpanAIndicator senkouSpanAIndicator = new IchimokuSenkouSpanAIndicator(series, tenkanSenIndicator, kijunSenIndicator);
        IchimokuSenkouSpanBIndicator senkouSpanBIndicator = new IchimokuSenkouSpanBIndicator(series);
        IchimokuChikouSpanIndicator chikouSpanIndicator = new IchimokuChikouSpanIndicator(series);

        return new CrossedUpIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                .or(new CrossedUpIndicatorRule(closePrice, kijunSenIndicator))
                .or(new CrossedUpIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                .or(new OverIndicatorRule(chikouSpanIndicator, closePrice));
    }

    private static Rule getIchimokuCloudTradingStrategySellRules(TimeSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        IchimokuTenkanSenIndicator tenkanSenIndicator = new IchimokuTenkanSenIndicator(series);
        IchimokuKijunSenIndicator kijunSenIndicator = new IchimokuKijunSenIndicator(series);
        IchimokuSenkouSpanAIndicator senkouSpanAIndicator = new IchimokuSenkouSpanAIndicator(series, tenkanSenIndicator, kijunSenIndicator);
        IchimokuSenkouSpanBIndicator senkouSpanBIndicator = new IchimokuSenkouSpanBIndicator(series);
        IchimokuChikouSpanIndicator chikouSpanIndicator = new IchimokuChikouSpanIndicator(series);

        return new CrossedDownIndicatorRule(tenkanSenIndicator, kijunSenIndicator)
                .or(new CrossedDownIndicatorRule(closePrice, kijunSenIndicator))
                .or(new CrossedDownIndicatorRule(senkouSpanAIndicator, senkouSpanBIndicator))
                .or(new UnderIndicatorRule(chikouSpanIndicator, closePrice));
    }

    private void setDayTradeRules() {
        DayTradeParameters dayTradeParam = parameters.getDayTradeParameters();
        if (dayTradeParam == null) {
            return;
        }

        LocalTime initialEntry = dayTradeParam.getInitialEntryTimeLimit();
        LocalTime finalEntry = dayTradeParam.getFinalEntryTimeLimit();
        LocalTime exit = dayTradeParam.getExitTimeLimit();

        Rule allowOpenRule = new AllowOpenRule(prices.getTimeSeries(), initialEntry, finalEntry);
        Rule forceCloseRule = new ForceCloseRule(prices.getTimeSeries(), exit);

        buyEntryRule = buyEntryRule == null ? allowOpenRule : buyEntryRule.and(allowOpenRule);
        setBuyExitRule(forceCloseRule);

        sellEntryRule = sellEntryRule == null ? allowOpenRule : sellEntryRule.and(allowOpenRule);
        setSellExitRule(forceCloseRule);
    }

    private void setMovingAverageRules() {
        MovingAverageParameters param = parameters.getEntryParameters().getMovingAverageParameters();
        if (param == null) {
            return;
        }

        Indicator<Decimal> shortMovingAverage;
        Indicator<Decimal> longMovingAverage;

        switch (param.getShortType()) {
            case SIMPLE:
                shortMovingAverage = new SMAIndicator(prices, param.getShortPeriods());
                break;

            case EXPONENTIAL:
                shortMovingAverage = new EMAIndicator(prices, param.getShortPeriods());
                break;

            default:
                throw new IllegalArgumentException("Invalid moving average short type");
        }

        switch (param.getLongType()) {
            case SIMPLE:
                longMovingAverage = new SMAIndicator(prices, param.getLongPeriods());
                break;

            case EXPONENTIAL:
                longMovingAverage = new EMAIndicator(prices, param.getLongPeriods());
                break;

            default:
                throw new IllegalArgumentException("Invalid moving average long type");
        }

        Rule underRule = new UnderIndicatorRule(shortMovingAverage, longMovingAverage);
        Rule overRule = new OverIndicatorRule(shortMovingAverage, longMovingAverage);

        buyEntryRule = buyEntryRule == null ? underRule : buyEntryRule.and(underRule);
        setBuyExitRule(overRule);

        sellEntryRule = sellEntryRule == null ? overRule : sellEntryRule.and(overRule);
        setSellExitRule(underRule);
    }

    private void setRSIRules() {
        RSIParameters param = parameters.getEntryParameters().getRsiParameters();
        if (param == null) {
            return;
        }

        WilderRSIIndicator rsiIndicator = new WilderRSIIndicator(prices, param.getPeriods());

        Decimal lowerLimit = Decimal.valueOf(param.getLowerValue());
        Decimal upperLimit = Decimal.valueOf(param.getUpperValue());

        Rule underRule = new UnderIndicatorRule(rsiIndicator, lowerLimit);
        Rule overRule = new OverIndicatorRule(rsiIndicator, upperLimit);

        buyEntryRule = buyEntryRule == null ? underRule : buyEntryRule.and(underRule);
        setBuyExitRule(overRule);

        sellEntryRule = sellEntryRule == null ? overRule : sellEntryRule.and(overRule);
        setSellExitRule(underRule);
    }

    private void setBBRules() {
        BollingerBandsParameters param = parameters.getEntryParameters().getBollingerBandsParameters();
        if (param == null) return;

        SMAIndicator simpleMovingAverage = new SMAIndicator(prices, param.getPeriods());
        StandardDeviationIndicator stdDeviation = new StandardDeviationIndicator(prices, param.getPeriods());

        BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(simpleMovingAverage);
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, stdDeviation, param.getFactor());
        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, stdDeviation, param.getFactor());

        Rule underRule = new UnderIndicatorRule(prices, lower);
        Rule overRule = new OverIndicatorRule(prices, upper);

        buyEntryRule = buyEntryRule == null ? underRule : buyEntryRule.and(underRule);
        setBuyExitRule(overRule);

        sellEntryRule = sellEntryRule == null ? overRule : sellEntryRule.and(overRule);
        setSellExitRule(underRule);
    }

    private void setFixedStopLoss() {
        ExitParameters exitParam = parameters.getExitParameters();
        if (exitParam == null)
            return;

        FixedStopLossParameters fixedStopLoss = exitParam.getFixedStopLoss();
        if (fixedStopLoss == null)
            return;

        stopLossRule = new FixedStopLossRule(prices, fixedStopLoss.getValue(), fixedStopLoss.getType());
    }

    private void setFixedStopGain() {
        ExitParameters exitParam = parameters.getExitParameters();
        if (exitParam == null)
            return;

        FixedStopGainParameters fixedStopGain = exitParam.getFixedStopGain();
        if (fixedStopGain == null)
            return;

        stopGainRule = new FixedStopGainRule(prices, fixedStopGain.getValue(), fixedStopGain.getType());
    }

    private void setTrailingStop() {
        ExitParameters exitParam = parameters.getExitParameters();
        if (exitParam == null)
            return;

        TrailingStopGainParameters trailingStop = exitParam.getTrailingStopGain();
        if (trailingStop == null)
            return;

        trailingStopRule = new TrailingStopRule(prices, trailingStop.getTrigger(), trailingStop.getDistance(),
                trailingStop.getType());
    }

    public Decimal buyOperate(int index, TradingRecord tradingRecord) {
        if (stopLossRule != null && stopLossRule.isSatisfied(index, tradingRecord)) {
            return stopLossRule.getExitPrice(tradingRecord);
        }

        if (stopGainRule != null && stopGainRule.isSatisfied(index, tradingRecord)) {
            return stopGainRule.getExitPrice(tradingRecord);
        }

        if (trailingStopRule != null && trailingStopRule.isSatisfied(index, tradingRecord)) {
            return trailingStopRule.getExitPrice(tradingRecord);
        }

        if (buyStrategy.shouldOperate(index, tradingRecord)) {
            return prices.getValue(index);
        }

        return null;
    }

    public Decimal sellOperate(int index, TradingRecord tradingRecord) {
        if (stopLossRule != null && stopLossRule.isSatisfied(index, tradingRecord)) {
            return stopLossRule.getExitPrice(tradingRecord);
        }

        if (stopGainRule != null && stopGainRule.isSatisfied(index, tradingRecord)) {
            return stopGainRule.getExitPrice(tradingRecord);
        }

        if (trailingStopRule != null && trailingStopRule.isSatisfied(index, tradingRecord)) {
            return trailingStopRule.getExitPrice(tradingRecord);
        }

        if (sellStrategy.shouldOperate(index, tradingRecord)) {
            return prices.getValue(index);
        }

        return null;
    }

    private void setBuyExitRule(Rule exitRule) {
        switch (parameters.getExitParameters().getExitType()) {
            case ANY_INDICATOR:
                buyExitRule = buyExitRule == null ? exitRule : buyExitRule.or(exitRule);
                break;
            case ALL_INDICATORS:
                buyExitRule = buyExitRule == null ? exitRule : buyExitRule.and(exitRule);
                break;
            case NO_INDICATORS:
                buyExitRule = new NoExitRule();
                break;
            default:
                break;
        }

    }

    private void setSellExitRule(Rule exitRule) {
        switch (parameters.getExitParameters().getExitType()) {
            case ANY_INDICATOR:
                sellExitRule = sellExitRule == null ? exitRule : sellExitRule.or(exitRule);
                break;
            case ALL_INDICATORS:
                sellExitRule = sellExitRule == null ? exitRule : sellExitRule.and(exitRule);
                break;
            case NO_INDICATORS:
                sellExitRule = new NoExitRule();
                break;
            default:
                break;
        }
    }

    public void startNewTrade() {
        if (trailingStopRule != null) {
            this.trailingStopRule.startNewTrade();
        }
    }

}
