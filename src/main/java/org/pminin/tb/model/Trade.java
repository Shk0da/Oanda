package org.pminin.tb.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Trade {
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Trades {
		List<Trade> trades = new ArrayList<>();
	}

	private String id;
	private int units;
	private String side;
	private Instrument instrument;
	private DateTime time;
	private double price;
	private double takeProfit;
	private double stopLoss;
	private double trailingStop;
	private double trailingAmount;
	private double profit;
}
