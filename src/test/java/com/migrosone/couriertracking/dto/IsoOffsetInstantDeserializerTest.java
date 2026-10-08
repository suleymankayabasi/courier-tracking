package com.migrosone.couriertracking.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

class IsoOffsetInstantDeserializerTest {
	private final JsonMapper mapper = JsonMapper.builder().build();

	@Test
	void offsetTimeIsConvertedToTheSameInstant() {
		assertThat(read("\"2026-01-01T13:00:00+03:00\"")).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
	}

	@Test
	void numericTimeIsRejected() {
		assertThatThrownBy(() -> read("1767261600")).isInstanceOf(DatabindException.class);
	}

	@Test
	void timeWithoutOffsetIsRejected() {
		assertThatThrownBy(() -> read("\"2026-01-01T10:00:00\"")).isInstanceOf(DatabindException.class)
				.hasMessageContaining("ISO-8601 date-time with offset");
	}

	private Instant read(String timeJson) {
		return mapper.readValue("{\"time\": " + timeJson + "}", CourierLocationRequest.class).time();
	}
}
