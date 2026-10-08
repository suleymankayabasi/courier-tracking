package com.migrosone.couriertracking.geo;

public class EquirectangularDistanceCalculator implements DistanceCalculator {
	private static final double HALF_TURN_DEGREES = 180.0;
	private static final double FULL_TURN_DEGREES = 360.0;

	@Override
	public double distanceInMeters(GeoPoint from, GeoPoint to) {
		double fromLatitude = Math.toRadians(from.latitude());
		double toLatitude = Math.toRadians(to.latitude());
		double deltaLongitude = Math.toRadians(shortestLongitudeDelta(from.longitude(), to.longitude()));

		double x = deltaLongitude * Math.cos((fromLatitude + toLatitude) / 2);
		double y = toLatitude - fromLatitude;
		return Earth.MEAN_RADIUS_METERS * Math.sqrt(x * x + y * y);
	}

	private static double shortestLongitudeDelta(double fromLongitude, double toLongitude) {
		double delta = toLongitude - fromLongitude;
		if (delta > HALF_TURN_DEGREES) {
			return delta - FULL_TURN_DEGREES;
		}
		if (delta < -HALF_TURN_DEGREES) {
			return delta + FULL_TURN_DEGREES;
		}
		return delta;
	}
}
