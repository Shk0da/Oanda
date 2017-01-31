package org.pminin.tb.model;

import org.joda.time.DateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostOrderResponse {
	public boolean active = false;
	private String instrument;
	private double price;
	@JsonDeserialize(using = UnixTimestampDeserializer.class)
	private DateTime time;
	private Order orderOpened = null;;

	public void activate() {
		active = true;
	}

	public void deactivate() {
		active = false;
		orderOpened = new Order();
	}

	public boolean hasOpenTrade() {
		return orderOpened != null && orderOpened.getId() != null && !orderOpened.getId().isEmpty();
	}
}
