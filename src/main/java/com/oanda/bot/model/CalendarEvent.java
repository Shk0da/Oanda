package com.oanda.bot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.joda.time.DateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalendarEvent {

	private double market;
	private String region;
	private String currency;
	private double forecast;
	private double previous;
	private String unit;
	private long timestamp;
	private String title;
	private int impact;

	public DateTime getDateTime() {
		return new DateTime(timestamp);
	}
}
