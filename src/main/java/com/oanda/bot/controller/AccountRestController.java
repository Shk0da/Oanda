package com.oanda.bot.controller;

import com.oanda.bot.model.Accounts;
import com.oanda.bot.service.AccountService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.oanda.bot.constants.Step;
import com.oanda.bot.model.Candle;
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

    @RequestMapping("account")
    public Accounts.Account account() {
        return accountService.getAccountDetails().getAccount();
    }

    @RequestMapping("candles")
    public Map<String, List<Candle>> candles() {
        Map<String, List<Candle>> prices = Maps.newHashMap();
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
            candles.addAll(accountService.getCandles(
                    Step.D,
                    DateTime.now(DateTimeZone.getDefault()).minusDays(365),
                    accountService.getInstrument(instrument)).getCandles());
            prices.put(instrument, candles);
        }

        return prices;
    }

}
