package com.migrosone.couriertracking.geo;

public record GeoPoint(double latitude, double longitude) {
	public static final double MIN_LATITUDE = -90.0;
	public static final double MAX_LATITUDE = 90.0;
	public static final double MIN_LONGITUDE = -180.0;
	public static final double MAX_LONGITUDE = 180.0;

	public GeoPoint {
		if (!(MIN_LATITUDE <= latitude && latitude <= MAX_LATITUDE)) {
			throw new IllegalArgumentException(
					"latitude must be between %s and %s but was %s".formatted(MIN_LATITUDE, MAX_LATITUDE, latitude));
		}
		if (!(MIN_LONGITUDE <= longitude && longitude <= MAX_LONGITUDE)) {
			throw new IllegalArgumentException(
					"longitude must be between %s and %s but was %s".formatted(MIN_LONGITUDE, MAX_LONGITUDE, longitude));
		}
	}
}
