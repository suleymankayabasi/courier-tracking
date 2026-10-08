package com.migrosone.couriertracking.support;

import com.migrosone.couriertracking.config.LocationProperties;
import com.migrosone.couriertracking.config.StoreEntranceProperties;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import java.time.Duration;
import java.time.Instant;

public final class TestFixtures {
	public static final Instant START_TIME = Instant.parse("2026-01-01T10:00:00Z");
	public static final Store ATASEHIR = new Store("Ataşehir MMM Migros", new GeoPoint(40.9923307, 29.1244229));
	public static final Store NOVADA = new Store("Novada MMM Migros", new GeoPoint(40.986106, 29.1161293));
	public static final StoreEntranceProperties STORE_ENTRANCE_PROPERTIES =
			new StoreEntranceProperties(100, Duration.ofMinutes(1));
	public static final LocationProperties LOCATION_PROPERTIES = new LocationProperties(Duration.ofMinutes(1));

	private static final double METERS_PER_DEGREE_OF_LATITUDE = 111_194.93;

	private TestFixtures() {
	}

	public static GeoPoint metersNorthOf(Store store, double meters) {
		GeoPoint storeLocation = store.location();
		return new GeoPoint(storeLocation.lat() + meters / METERS_PER_DEGREE_OF_LATITUDE, storeLocation.lng());
	}

	public static Instant secondsAfterStart(long seconds) {
		return START_TIME.plusSeconds(seconds);
	}
}
