package com.migrosone.couriertracking.model;

public record Store(String name, double lat, double lng) {
	public Store {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("store name is required");
		}
		if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
			throw new IllegalArgumentException("store " + name + " has invalid coordinates " + lat + ", " + lng);
		}
	}

	public GeoPoint location() {
		return new GeoPoint(lat, lng);
	}
}
