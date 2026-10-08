package com.migrosone.couriertracking.geo;

public class HaversineDistanceCalculator implements DistanceCalculator {
	@Override
	public double distanceInMeters(GeoPoint from, GeoPoint to) {
		double fromLatitude = Math.toRadians(from.latitude());
		double toLatitude = Math.toRadians(to.latitude());
		double deltaLatitude = toLatitude - fromLatitude;
		double deltaLongitude = Math.toRadians(to.longitude() - from.longitude());

		double sinHalfLatitude = Math.sin(deltaLatitude / 2);
		double sinHalfLongitude = Math.sin(deltaLongitude / 2);
		double a = sinHalfLatitude * sinHalfLatitude
				+ Math.cos(fromLatitude) * Math.cos(toLatitude) * sinHalfLongitude * sinHalfLongitude;

		return 2 * Earth.MEAN_RADIUS_METERS * Math.asin(Math.sqrt(Math.min(1.0, a)));
	}
}
