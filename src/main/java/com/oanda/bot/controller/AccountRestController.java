package com.oanda.bot.controller;

import com.oanda.bot.InstrumentStorage;
import com.oanda.bot.constants.Step;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Accounts;
import com.oanda.bot.model.Candle;
import com.oanda.bot.service.AccountService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jersey.repackaged.com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class AccountRestController {

    private static final Config config = ConfigFactory.load();

    @Autowired
    private AccountService accountService;

    @Autowired
    private MainDao mainDao;

    @Autowired
    private InstrumentStorage storage;

    @RequestMapping("account")
    public Accounts.Account account() {
        return accountService.getAccountDetails().getAccount();
    }

    @RequestMapping("candles")
    public Map<String, List<Candle>> candles() throws Exception {
        Map<String, List<Candle>> prices = Maps.newHashMap();
        Step step = Step.D;
        int daysBack = 365;
        Config pairsCfg = config.getConfig("pairs");

        for (String key : pairsCfg.root().keySet()) {
            Config cfg = pairsCfg.getConfig(key);
            if (!cfg.getBoolean("on")) {
                continue;
            }
            String left = cfg.getString("left");
            String right = cfg.getString("right");
            String instrument = left + "_" + right;

            List<Candle> candles = mainDao.getWhereTimeCandle(
                    step,
                    storage.getInstrument(instrument),
                    DateTime.now(DateTimeZone.getDefault()).minusDays(daysBack).toDate(),
                    DateTime.now().toDate()
            );

            if (candles == null || candles.isEmpty() || (candles.size() < (daysBack * .90))) {
                candles = mainDao.updateHistoryCandles(
                        step,
                        storage.getInstrument(instrument),
                        new DateTime().minusDays(daysBack),
                        DateTime.now()
                );
            }

            Date lastCandleDate = candles.get(candles.size() - 1).getDateTime();
            int dayDiff = (int)TimeUnit.DAYS.convert((new Date().getTime()) - lastCandleDate.getTime(), TimeUnit.MILLISECONDS);

            if (dayDiff >= 1) {
                candles.addAll(mainDao.updateHistoryCandles(
                        step,
                        storage.getInstrument(instrument),
                        new DateTime().minusDays(dayDiff),
                        DateTime.now()
                ));
            }

            prices.put(instrument, candles);
        }

        return prices;
    }

}
