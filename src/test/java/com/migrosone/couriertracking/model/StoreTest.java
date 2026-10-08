package com.migrosone.couriertracking.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StoreTest {
	@Test
	void boundaryCoordinatesAreValid() {
		assertThat(new Store("edge", -90, 180).location()).isEqualTo(new GeoPoint(-90, 180));
		assertThat(new Store("edge", 90, -180).location()).isEqualTo(new GeoPoint(90, -180));
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "  "})
	void nameIsRequired(String name) {
		assertThatThrownBy(() -> new Store(name, 41, 29)).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest
	@CsvSource({"-90.1, 29", "90.1, 29", "41, -180.1", "41, 180.1"})
	void coordinatesMustBeOnEarth(double lat, double lng) {
		assertThatThrownBy(() -> new Store("store", lat, lng)).isInstanceOf(IllegalArgumentException.class);
	}
}
