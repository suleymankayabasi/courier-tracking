package com.migrosone.couriertracking.geo;

@FunctionalInterface
public interface DistanceCalculator {
	double distanceInMeters(GeoPoint from, GeoPoint to);
}
