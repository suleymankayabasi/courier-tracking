package com.migrosone.couriertracking.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class StoreTest {
	private static final GeoPoint LOCATION = new GeoPoint(41, 29);

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "  "})
	void requiresName(String name) {
		assertThatThrownBy(() -> new Store(name, LOCATION)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void requiresLocation() {
		assertThatThrownBy(() -> new Store("store", null)).isInstanceOf(NullPointerException.class);
	}
}
