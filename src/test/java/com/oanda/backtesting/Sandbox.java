package com.oanda.backtesting;

import com.oanda.backtesting.strategies.*;
import com.oanda.bot.TradingBotApplication;
import com.oanda.bot.constants.Step;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Candle;
import com.oanda.bot.model.Instrument;
import com.oanda.bot.service.AccountService;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.analysis.criteria.*;
import jersey.repackaged.com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TradingBotApplication.class, loader = SpringApplicationContextLoader.class)
public class Sandbox {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

    @Autowired
    private AccountService accountService;

    @Before
    public void init() {
        //init
    }

    @Test
    public void profit() throws Exception {
        TimeSeries series = new TimeSeries("eur_usd", getTicks("EUR_USD", Step.W, 365));

        // Running the strategy
        Strategy strategy = GlobalExtremaStrategy.buildStrategy(series);
        TradingRecord tradingRecord = series.run(strategy);

        /*Analysis*/
        TotalProfitCriterion totalProfit = new TotalProfitCriterion();
        double profit = totalProfit.calculate(series, tradingRecord);
        System.out.println(ANSI_GREEN);
        //Result
        System.out.println("Your 1000$ would turn into: " + String.format("%.2f", 1000 * profit) + "$!");
        // Total profit
        System.out.println("Total profit: " + profit);
        // Number of ticks
        System.out.println("Number of ticks: " + new NumberOfTicksCriterion().calculate(series, tradingRecord));
        // Average profit (per tick)
        System.out.println("Average profit (per tick): " + new AverageProfitCriterion().calculate(series, tradingRecord));
        // Number of trades
        System.out.println("Number of trades: " + new NumberOfTradesCriterion().calculate(series, tradingRecord));
        // Profitable trades ratio
        System.out.println("Profitable trades ratio: " + new AverageProfitableTradesCriterion().calculate(series, tradingRecord));
        // Maximum drawdown
        System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));
        // Reward-risk ratio
        System.out.println("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(series, tradingRecord));
        // Total transaction cost
        System.out.println("Total transaction cost (from $1000): " + new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));
        // Buy-and-hold
        System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));
        // Total profit vs buy-and-hold
        System.out.println("Custom strategy profit vs buy-and-hold strategy profit: " + new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));
        System.out.println(ANSI_RESET);
    }

    @Test
    public void alternativeTest() throws Exception {
        TimeSeries series = new TimeSeries("eur_usd", getTicks("EUR_USD", Step.W, 365));

        // Running the strategy
        Strategy strategy = GlobalExtremaStrategy.buildStrategy(series);
        TradingRecord tradingRecord = new TradingRecord();

        int countTicks = series.getTickCount();
        for (int endIndex = 0; endIndex < countTicks; endIndex++) {
            Tick newTick = series.getTick(endIndex);
            if (strategy.shouldEnter(endIndex)) {
                // Our strategy should enter
                System.out.println("Strategy should ENTER on " + endIndex);
                boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), Decimal.TEN);
                if (entered) {
                    Order entry = tradingRecord.getLastEntry();
                    System.out.println("Entered on " + entry.getIndex()
                            + " (price=" + entry.getPrice().toDouble()
                            + ", amount=" + entry.getAmount().toDouble() + ")");
                }
            } else if (strategy.shouldExit(endIndex)) {
                // Our strategy should exit
                System.out.println("Strategy should EXIT on " + endIndex);
                boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), Decimal.TEN);
                if (exited) {
                    Order exit = tradingRecord.getLastExit();
                    System.out.println("Exited on " + exit.getIndex()
                            + " (price=" + exit.getPrice().toDouble()
                            + ", amount=" + exit.getAmount().toDouble() + ")");
                }
            }
        }
    }

    @Test
    public void walkForward() throws Exception {
        TimeSeries series = new TimeSeries("eur_usd", getTicks("EUR_USD", Step.M15, 90));
        List<TimeSeries> subseries = series.split(Period.days(6), Period.days(7));

        // Building the map of strategies
        HashMap<Strategy, String> strategies = new HashMap<>();
        strategies.put(CCICorrectionStrategy.buildStrategy(series), "CCI Correction");
        strategies.put(GlobalExtremaStrategy.buildStrategy(series), "Global Extrema");
        strategies.put(MovingMomentumStrategy.buildStrategy(series), "Moving Momentum");
        strategies.put(RSI2Strategy.buildStrategy(series), "RSI-2");

        // The analysis criterion
        AnalysisCriterion profitCriterion = new TotalProfitCriterion();

        for (TimeSeries slice : subseries) {
            // For each sub-series...
            System.out.println(ANSI_GREEN + "Sub-series: " + slice.getSeriesPeriodDescription() + ANSI_RESET);
            System.out.println(ANSI_WHITE_BACKGROUND + ANSI_BLACK);
            for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
                Strategy strategy = entry.getKey();
                String name = entry.getValue();
                // For each strategy...
                TradingRecord tradingRecord = slice.run(strategy);
                double profit = profitCriterion.calculate(slice, tradingRecord);
                System.out.println("\tProfit for " + name + ": " + profit);
            }
            System.out.println(ANSI_RESET);

            Strategy bestStrategy = profitCriterion.chooseBest(slice, new ArrayList<>(strategies.keySet()));
            System.out.println(ANSI_RED + "\tBest strategy: " + strategies.get(bestStrategy) + "\n" + ANSI_RESET);
        }
    }

    private List<Tick> getTicks(String pair, Step step, int daysBack) throws Exception {
        Instrument instrument = accountService.getInstrument(pair);
        Candle.Candles candles = accountService.getCandles(
                step,
                DateTime.now(DateTimeZone.getDefault()).minusDays(daysBack),
                instrument);

        List<Tick> ticks = Lists.newArrayList();
        candles.getCandles().forEach(candle ->
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
