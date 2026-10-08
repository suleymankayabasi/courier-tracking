package com.migrosone.couriertracking.distance;

import com.migrosone.couriertracking.model.GeoPoint;

public interface DistanceCalculator {
	double distanceInMeters(GeoPoint from, GeoPoint to);
}
