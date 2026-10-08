package com.migrosone.couriertracking.model;

public record Store(String name, double lat, double lng) {
	public GeoPoint location() {
		return new GeoPoint(lat, lng);
	}
}
