package com.migrosone.couriertracking.distance;

import com.migrosone.couriertracking.model.GeoPoint;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {
	private static final double EARTH_MEAN_RADIUS_METERS = 6_371_000;
	private static final double MAX_HAVERSINE_VALUE = 1;

	@Override
	public double distanceInMeters(GeoPoint from, GeoPoint to) {
		double fromLatRadians = Math.toRadians(from.lat());
		double toLatRadians = Math.toRadians(to.lat());
		double deltaLatRadians = toLatRadians - fromLatRadians;
		double deltaLngRadians = Math.toRadians(to.lng() - from.lng());

		double haversineOfCentralAngle = haversine(deltaLatRadians)
				+ Math.cos(fromLatRadians) * Math.cos(toLatRadians) * haversine(deltaLngRadians);
		return EARTH_MEAN_RADIUS_METERS * inverseHaversine(haversineOfCentralAngle);
	}

	private static double haversine(double angleRadians) {
		double sinOfHalfAngle = Math.sin(angleRadians / 2);
		return sinOfHalfAngle * sinOfHalfAngle;
	}

	private static double inverseHaversine(double value) {
		double valueWithoutRoundingOverflow = Math.min(MAX_HAVERSINE_VALUE, value);
		return 2 * Math.asin(Math.sqrt(valueWithoutRoundingOverflow));
	}
}
