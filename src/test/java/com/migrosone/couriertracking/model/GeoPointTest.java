package com.migrosone.couriertracking.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GeoPointTest {
	@ParameterizedTest
	@CsvSource({"-90, -180", "90, 180", "0, 0"})
	void acceptsCoordinatesOnEarth(double lat, double lng) {
		assertThatCode(() -> new GeoPoint(lat, lng)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@CsvSource({"-90.1, 0", "90.1, 0", "0, -180.1", "0, 180.1"})
	void rejectsCoordinatesOutsideEarth(double lat, double lng) {
		assertThatThrownBy(() -> new GeoPoint(lat, lng)).isInstanceOf(IllegalArgumentException.class);
	}
}
