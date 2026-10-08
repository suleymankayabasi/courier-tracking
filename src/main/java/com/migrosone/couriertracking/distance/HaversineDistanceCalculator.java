package com.migrosone.couriertracking.distance;

import com.migrosone.couriertracking.model.GeoPoint;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {
	private static final double EARTH_RADIUS_METERS = 6_371_000;

	@Override
	public double distanceInMeters(GeoPoint from, GeoPoint to) {
		double fromLat = Math.toRadians(from.lat());
		double toLat = Math.toRadians(to.lat());
		double deltaLat = toLat - fromLat;
		double deltaLng = Math.toRadians(to.lng() - from.lng());

		double a = Math.pow(Math.sin(deltaLat / 2), 2)
				+ Math.cos(fromLat) * Math.cos(toLat) * Math.pow(Math.sin(deltaLng / 2), 2);
		return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(Math.min(1, a)));
	}
}
