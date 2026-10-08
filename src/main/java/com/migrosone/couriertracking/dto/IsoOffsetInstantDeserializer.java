package com.migrosone.couriertracking.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

class IsoOffsetInstantDeserializer extends ValueDeserializer<Instant> {
	@Override
	public Instant deserialize(JsonParser parser, DeserializationContext context) {
		if (!parser.hasToken(JsonToken.VALUE_STRING)) {
			return (Instant) context.handleUnexpectedToken(Instant.class, parser);
		}
		String text = parser.getString();
		try {
			return OffsetDateTime.parse(text).toInstant();
		}
		catch (DateTimeParseException e) {
			return (Instant) context.handleWeirdStringValue(
					Instant.class, text, "expected an ISO-8601 date-time with offset");
		}
	}
}
