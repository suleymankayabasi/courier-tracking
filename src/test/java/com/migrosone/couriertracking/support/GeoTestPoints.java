package com.migrosone.couriertracking.support;

import com.migrosone.couriertracking.geo.Earth;
import com.migrosone.couriertracking.geo.GeoPoint;

public final class GeoTestPoints {
	public static final GeoPoint ATASEHIR = new GeoPoint(40.9923307, 29.1244229);
	public static final GeoPoint NOVADA = new GeoPoint(40.986106, 29.1161293);

	private GeoTestPoints() {
	}

	public static GeoPoint northOf(GeoPoint origin, double meters) {
		return new GeoPoint(origin.latitude() + Math.toDegrees(meters / Earth.MEAN_RADIUS_METERS), origin.longitude());
	}
}
