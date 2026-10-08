package com.migrosone.couriertracking.model;

import java.util.Objects;

public record Store(String name, GeoPoint location) {
	public Store {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("store name is required");
		}
		Objects.requireNonNull(location, "store location is required");
	}
}
