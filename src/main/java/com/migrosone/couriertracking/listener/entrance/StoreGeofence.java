package com.migrosone.couriertracking.listener.entrance;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.geo.DistanceCalculator;
import com.migrosone.couriertracking.geo.GeoPoint;
import org.springframework.stereotype.Component;

@Component
class StoreGeofence {
	private final DistanceCalculator distanceCalculator;
	private final double radiusMeters;

	StoreGeofence(DistanceCalculator distanceCalculator, CourierTrackingProperties properties) {
		this.distanceCalculator = distanceCalculator;
		this.radiusMeters = properties.entrance().radiusMeters();
	}

	boolean contains(GeoPoint storeLocation, GeoPoint point) {
		return distanceCalculator.distanceInMeters(point, storeLocation) <= radiusMeters;
	}
}
