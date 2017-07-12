package com.oanda.backtesting;

import com.oanda.bot.InstrumentStorage;
import com.oanda.bot.TradingBotApplication;
import com.oanda.bot.constants.Step;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Candle;
import com.oanda.bot.strategies.*;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.analysis.criteria.*;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;
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
    private MainDao mainDao;

    @Autowired
    private InstrumentStorage storage;

    @Before
    public void init() {
        //init
    }

    @Test
    public void profit() throws Exception {
        TimeSeries series = new TimeSeries("EUR_USD", getTicks("EUR_USD", Step.H1, 365));

        // Running the strategy
        Strategy strategy = IchimokuCloudTradingStrategy.buildStrategy(series);
        TradingRecord tradingRecord = series.run(strategy);

        /*Analysis*/
        TotalProfitCriterion totalProfit = new TotalProfitCriterion();
        double profit = totalProfit.calculate(series, tradingRecord);
        System.out.println(ANSI_GREEN);
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
    public void walkTest() throws Exception {
        TimeSeries series = new TimeSeries("eur_usd", getTicks("EUR_USD", Step.M10, 5));
        Map<String, Double> results = Maps.newHashMap();

        /*
        * SolyankaStrategy
        *
        * H1: 48;38;3;43;38;37;6 1.0037866641130748
        *     48;38;3;48;2;37;6 1.003498480743955
        *     48;38;3;48;2;10;1 1.003498480743955
        *     10;16;3;48;2;10;1 1.0058799583204894
        *     10;16;6;24;33;10;1 1.0064696941079778
        *     10;16;6;24;33;5;1 1.0068122253829486
        *     9;23;6;24;33;5;1 1.007868495505981
        *     9;23;7;26;49;5;1 1.0079408608619682
        *     9;23;6;24;33;4;1 1.007868495505981
        *     5;14;6;24;33;5;1 1.0082664763887241 *best
        *     5;14;13;38;18;5;1 1.0082664763887241
        *     5;14;13;38;18;5;1 1.0082664763887241
        *
        * M10:6;6;6;24;33;5;1 1.0067615969238015
        *     6;6;6;48;24;5;1 1.0081935587770867
        *     6;6;6;48;24;5;1 1.0077064215304206
        *     2;3;6;48;24;5;1 1.0086920676377533
        *     2;3;6;48;46;5;1 1.0087898927433605
        *     2;3;6;48;46;8;1 1.0094104390240544
        *     5;6;6;48;46;8;1 1.009732643276619
        *     5;6;3;42;24;8;1 1.0101219227846172
        *     5;6;3;42;24;8;1 1.0097128162665794
        *     9;11;3;42;24;8;1 1.0107234252654913
        *     9;11;3;32;33;8;1 1.0114441883652268
        *
        * M1:
        * */

        int i1 = 9;
        int i2 = 11;
        int i3 = 3;
        int i4 = 32;
        int i5 = 33;
        int i6 = 8;
        int i7 = 1;

        for (i6 = 1; i6 < 50; i6++)
            for (i7 = 1; i7 < 50; i7++) /*for (i5 = 1; i5 < 50; i5++)*/ {

                Map<String, Integer> params = Maps.newHashMap();
                params.put("shortPeriodMA", i1);
                params.put("longPeriodMA", i2);
                params.put("RSIPeriods", i3);
                params.put("RSILowerValue", i4);
                params.put("RSIUpperValue", i5);
                params.put("bollingerBandsPeriod", i6);
                params.put("bollingerBandsFactor", i7);

                Strategy strategy = SolyankaStrategy.buildStrategy(series, params);
                TradingRecord tradingRecord = series.run(strategy);
                TotalProfitCriterion totalProfit = new TotalProfitCriterion();
                double profit = totalProfit.calculate(series, tradingRecord);
                results.put(String.format("%d;%d;%d;%d;%d;%d;%d", i1, i2, i3, i4, i5, i6, i7), profit);
            }

        String betterParams = results.entrySet().stream()
                .max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();

        System.out.println(ANSI_GREEN + betterParams + " " + results.get(betterParams) + ANSI_RESET);
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
        TimeSeries series = new TimeSeries("USD_CHF", getTicks("USD_CHF", Step.M1, 5));
        List<TimeSeries> subseries = series.split(Period.days(6), Period.days(7));

        // Building the map of strategies
        HashMap<Strategy, String> strategies = new HashMap<>();
        strategies.put(IchimokuMacdEma.buildStrategy(series), "IchimokuMacdEma");
        strategies.put(IchimokuMacdEmaSecond.buildStrategy(series), "IchimokuMacdEmaSecond");
        strategies.put(IchimokuCloudTradingStrategy.buildStrategy(series), "IchimokuCloudTradingStrategy");

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

    private List<Tick> getTicks(String instrument, Step step, int daysBack) throws Exception {
        List<Candle> candles = Lists.newArrayList();

        int ichiIndex = 26;
        for (int d = daysBack; d > 0; d = d - ichiIndex) {
            candles.addAll(mainDao.getWhereTimeCandle(
                    step,
                    storage.getInstrument(instrument),
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
