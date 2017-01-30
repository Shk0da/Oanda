package org.pminin.tb.model;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Order {
	private String id;
	private String instrument;
	private int units;
	private String side;
	private String type;
	private double price;
	private double takeProfit;
	private double stopLoss;
	@JsonDeserialize(using = UnixTimestampDeserializer.class)
	private DateTime expiry;
	private double upperBound;
	private double lowerBound;
	private double trailingStop;

	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	public static class Orders {
		List<Order> orders = new ArrayList<>();
	}
}