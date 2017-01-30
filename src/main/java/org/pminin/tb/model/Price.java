package org.pminin.tb.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Price {

	private String intrument;
	private DateTime time;
	private double bid;
	private double ask;
	private String halted;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class Prices {
		private List<Price> prices = new ArrayList<>();
	}
	
	public double getSpread() {
		return Math.abs(ask - bid);
	}
}
