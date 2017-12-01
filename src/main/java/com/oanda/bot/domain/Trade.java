package com.oanda.bot.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Trade {

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class Details {
		private double price;
		private Order.TimeInForce timeInForce = Order.TimeInForce.GTC;

		public Details(double price) {
			this.price = price;
		}
	}

    private String id;
	private int currentUnits;
	private double financing;
	private int initialUnits;
	private String instrument;
	private String openTime;
	private double price;
	private double realizedPL;
	private State state;
	private double unrealizedPL;

	private double stopLoss;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trades {
        List<Trade> trades = new ArrayList<>();
    }

}
