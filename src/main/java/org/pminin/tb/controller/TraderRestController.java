package org.pminin.tb.controller;

import jersey.repackaged.com.google.common.collect.Lists;
import org.pminin.tb.Scheduler;
import org.pminin.tb.model.Instrument;
import org.pminin.tb.model.Order;
import org.pminin.tb.service.AccountService;
import org.pminin.tb.util.DateTimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TraderRestController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private Scheduler scheduler;
    @Autowired
    private Environment env;

    @RequestMapping("help")
    public List<String> help() {
        return Lists.newArrayList("startwork",
                "state",
                "account",
                "testCreate?type=(MARKET, LIMIT, STOP, MARKET_IF_TOUCHED, TAKE_PROFIT, STOP_LOSS, TRAILING_STOP_LOSS, MARKET_ORDER, STOP_ORDER, LIMIT_ORDER)",
                "testUpdate?id=777",
                "reset"
        );
    }

    @RequestMapping("startwork")
    public String startwork() {
        scheduler.startWorkEveryDay();
        return "Starting work...";
    }

    @RequestMapping(value = "testCreate", params = {"type"})
    public Object test(@RequestParam("type") Order.OrderType type) {
        return createOrder(type);
    }

    @RequestMapping(value = "testUpdate")
    public Object testUpdate() {
        return updateOrder();
    }

    @RequestMapping("reset")
    public String reset() {
        scheduler.resetState();
        return "resetting state...";
    }

    @RequestMapping("state")
    public String state() {
        try {
            byte[] encoded = Files.readAllBytes(Paths.get(env.getProperty("logging.path") + "/spring.log"));
            String logContent = new String(encoded, "UTF-8");
            return logContent.replaceAll("\n", "</br>");
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private Object createOrder(Order.OrderType type) {
        Order order = new Order();
        order.setInstrument("USD_JPY");
        order.setType(type);
        order.setCancelledTime(DateTimeUtil.rfc3339Plus2Days());

        double balance = accountService.getAccountDetails().getBalance();
        order.setUnits((int) (balance / 100 * 1000));

        order.setPositionFill(Order.OrderPositionFill.DEFAULT);
        order.setPrice(1.072);

        Order.Details takeProfit = new Order.Details();
        takeProfit.setPrice(1.075);
        order.setTakeProfitOnFill(takeProfit);

        Order.Details stopLoss = new Order.Details();
        stopLoss.setPrice(1.070);
        order.setStopLossOnFill(stopLoss);

        order = accountService.createOrder(order);
        return order != null ? order : "Error occurred during order creation";
    }

    private Object updateOrder() {
        Order.Orders orders = accountService.getOrders(accountService.getInstrument("USD_JPY"));

        if (orders.getOrders().isEmpty()) return "Nothing update";

        Order order = orders.getOrders().get(0);
        order.setStopLossOnFill(new Order.Details(1.050));

        order = accountService.updateOrder(order);
        return order != null ? order : "Error occurred during order creation";
    }

}
