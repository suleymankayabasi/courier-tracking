package com.migrosone.couriertracking.distance;

import com.migrosone.couriertracking.model.GeoPoint;

/** Strategy for measuring the distance between two points; used for both the store radius and the travel distance. */
public interface DistanceCalculator {
	double distanceInMeters(GeoPoint from, GeoPoint to);
}
