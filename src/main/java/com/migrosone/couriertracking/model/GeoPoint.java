package com.migrosone.couriertracking.model;

public record GeoPoint(double lat, double lng) {
	private static final double MAX_LATITUDE = 90;
	private static final double MAX_LONGITUDE = 180;

	public GeoPoint {
		if (Math.abs(lat) > MAX_LATITUDE) {
			throw new IllegalArgumentException("latitude must be between -90 and 90 but was " + lat);
		}
		if (Math.abs(lng) > MAX_LONGITUDE) {
			throw new IllegalArgumentException("longitude must be between -180 and 180 but was " + lng);
		}
	}
}
