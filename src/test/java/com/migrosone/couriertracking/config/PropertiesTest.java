package com.migrosone.couriertracking.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PropertiesTest {
	@ParameterizedTest
	@ValueSource(doubles = {0, -1})
	void rejectsNonPositiveRadius(double radiusMeters) {
		assertThatThrownBy(() -> new StoreEntranceProperties(radiusMeters, Duration.ofMinutes(1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("radius-meters");
	}

	@Test
	void rejectsMissingOrNegativeReentryThreshold() {
		assertThatThrownBy(() -> new StoreEntranceProperties(100, null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new StoreEntranceProperties(100, Duration.ofSeconds(-1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("reentry-threshold");
	}

	@Test
	void rejectsMissingOrNegativeClockSkew() {
		assertThatThrownBy(() -> new LocationProperties(null)).isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> new LocationProperties(Duration.ofSeconds(-1)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("allowed-clock-skew");
	}
}
