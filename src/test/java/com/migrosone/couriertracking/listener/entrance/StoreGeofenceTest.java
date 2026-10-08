package com.migrosone.couriertracking.listener.entrance;

import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.support.TestProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class StoreGeofenceTest {
	private static final GeoPoint STORE = new GeoPoint(41.0, 29.0);
	private static final GeoPoint COURIER = new GeoPoint(41.001, 29.0);

	@Test
	void pointExactlyOnTheRadiusIsInside() {
		assertThat(geofenceMeasuring(100.0).contains(STORE, COURIER)).isTrue();
	}

	@Test
	void pointJustBeyondTheRadiusIsOutside() {
		assertThat(geofenceMeasuring(Math.nextUp(100.0)).contains(STORE, COURIER)).isFalse();
	}

	@Test
	void pointWellInsideTheRadiusIsInside() {
		assertThat(geofenceMeasuring(0.0).contains(STORE, COURIER)).isTrue();
	}

	@Test
	void pointWellOutsideTheRadiusIsOutside() {
		assertThat(geofenceMeasuring(150.0).contains(STORE, COURIER)).isFalse();
	}

	@Test
	void radiusComesFromConfiguration() {
		StoreGeofence geofence = new StoreGeofence(
				(from, to) -> 150.0, TestProperties.withEntrance(200.0, Duration.ofSeconds(60)));

		assertThat(geofence.contains(STORE, COURIER)).isTrue();
	}

	private static StoreGeofence geofenceMeasuring(double meters) {
		return new StoreGeofence((from, to) -> meters, TestProperties.defaults());
	}
}
