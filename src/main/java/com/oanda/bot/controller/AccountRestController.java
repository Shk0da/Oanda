package com.oanda.bot.controller;

import com.oanda.bot.InstrumentStorage;
import com.oanda.bot.constants.Step;
import com.oanda.bot.dao.MainDao;
import com.oanda.bot.model.Accounts;
import com.oanda.bot.model.Candle;
import com.oanda.bot.service.AccountService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

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

            prices.put(instrument, candles);
        }

        return prices;
    }

}
