package com.migrosone.couriertracking.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GeoPointTest {
	@Test
	void acceptsBoundaryValues() {
		assertThat(new GeoPoint(90.0, 180.0)).isNotNull();
		assertThat(new GeoPoint(-90.0, -180.0)).isNotNull();
	}

	@ParameterizedTest
	@ValueSource(doubles = { 90.0001, -90.0001, Double.NaN, Double.POSITIVE_INFINITY })
	void rejectsInvalidLatitude(double latitude) {
		assertThatThrownBy(() -> new GeoPoint(latitude, 0.0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("latitude");
	}

	@ParameterizedTest
	@ValueSource(doubles = { 180.0001, -180.0001, Double.NaN, Double.NEGATIVE_INFINITY })
	void rejectsInvalidLongitude(double longitude) {
		assertThatThrownBy(() -> new GeoPoint(0.0, longitude))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("longitude");
	}
}
