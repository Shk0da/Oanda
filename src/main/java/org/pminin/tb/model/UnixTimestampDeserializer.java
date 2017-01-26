package org.pminin.tb.model;

import java.io.IOException;

import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class UnixTimestampDeserializer extends JsonDeserializer<DateTime> {

	@Override
	public DateTime deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		String timestamp = jp.getText().trim();
		try {
			return new DateTime(Long.valueOf(timestamp));
		} catch (NumberFormatException e) {
			return null;
		}
	}
}