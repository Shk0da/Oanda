package org.pminin.tb.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.pminin.tb.Scheduler;
import org.pminin.tb.model.Order;
import org.pminin.tb.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TraderRestController {

	@Autowired
	private AccountService accountService;
	@Autowired
	private Scheduler scheduler;
	@Autowired
	private Environment env;
//	@Autowired
//	private InstrumentStorage storage;

	public String getAccountDetails() {
		return "<p>" + accountService.getAccountDetails().toString() + "</p>";
	}

	private String createOrder() {
		Order order = new Order();
		order.setInstrument("EUR_USD");

		order.setSide("buy");
		double balance = accountService.getAccountDetails().getBalance();
		order.setUnits((int) (balance / 100 * 1000));
		order.setPrice(1.073);
		order.setTakeProfit(1.075);
		order.setStopLoss(1.070);
		order = accountService.createOrder(order);
		return order != null ? order.getId() : "Error occured during order creation";
	}

	@RequestMapping("/{command}")
	public String command(@PathVariable String command) {
		switch (command) {
		case "startwork":
			scheduler.startWorkEveryDay();
			return "Starting work...";
		case "order":
			return createOrder();
		case "account":
			return getAccountDetails();
		case "reset":
			scheduler.resetState();
			return "resetting state...";
		case "state":
			try {
				byte[] encoded = Files.readAllBytes(Paths.get(env.getProperty("logging.path") + "/spring.log"));
				String logContent = new String(encoded, "UTF-8");
				return logContent.replaceAll("\n", "</br>");
			} catch (IOException e) {
				return e.getMessage();
			}
		default:
			return "Incorrect command";
		}
	}

}
