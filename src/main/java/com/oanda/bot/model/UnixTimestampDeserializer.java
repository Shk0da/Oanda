package com.oanda.bot.model;

import java.io.IOException;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class UnixTimestampDeserializer extends JsonDeserializer<DateTime> {

	@Override
	public DateTime deserialize(JsonParser jsonparser, DeserializationContext deserializationcontext)
			throws IOException, JsonProcessingException {
		try {
			String date = jsonparser.getText();
			return new DateTime(Long.valueOf(date) / 1000, DateTimeZone.getDefault());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}