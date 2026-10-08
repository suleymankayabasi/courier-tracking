package com.migrosone.couriertracking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.distance.HaversineDistanceCalculator;
import com.migrosone.couriertracking.model.Courier;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoreEntranceDetectorTest {
	private static final double METERS_PER_DEGREE_OF_LATITUDE = 111_194.93;
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

	private final Store atasehir = new Store("Ataşehir MMM Migros", 40.9923307, 29.1244229);
	private final Store novada = new Store("Novada MMM Migros", 40.986106, 29.1161293);
	private final StoreEntranceDetector detector =
			new StoreEntranceDetector(() -> List.of(atasehir, novada), new HaversineDistanceCalculator());
	private final Courier courier = new Courier("courier-1");

	@Test
	void movingFromOutsideToInsideIsAnEntrance() {
		courier.moveTo(north(atasehir, 300), T0, 0);

		assertThat(detector.enteredStores(courier, north(atasehir, 50), T0.plusSeconds(20))).containsExactly(atasehir);
	}

	@Test
	void firstLocationInsideIsAnEntrance() {
		assertThat(detector.enteredStores(courier, north(atasehir, 50), T0)).containsExactly(atasehir);
	}

	@Test
	void locationOutsideEveryStoreIsNoEntrance() {
		assertThat(detector.enteredStores(courier, north(atasehir, 300), T0)).isEmpty();
	}

	@Test
	void radiusIsOneHundredMetersInclusive() {
		assertThat(detector.enteredStores(courier, north(atasehir, 99.9), T0)).containsExactly(atasehir);
		assertThat(detector.enteredStores(courier, north(atasehir, 100.1), T0)).isEmpty();
	}

	@Test
	void stayingInsideIsNoNewEntrance() {
		courier.enter(atasehir, T0);
		courier.moveTo(north(atasehir, 50), T0, 0);

		assertThat(detector.enteredStores(courier, north(atasehir, 20), T0.plusSeconds(300))).isEmpty();
	}

	@Test
	void reentryWithinOneMinuteIsNotCounted() {
		courier.enter(atasehir, T0);
		courier.moveTo(north(atasehir, 200), T0.plusSeconds(20), 0);

		assertThat(detector.enteredStores(courier, north(atasehir, 50), T0.plusSeconds(59))).isEmpty();
	}

	@Test
	void reentryAfterExactlyOneMinuteIsCounted() {
		courier.enter(atasehir, T0);
		courier.moveTo(north(atasehir, 200), T0.plusSeconds(30), 0);

		assertThat(detector.enteredStores(courier, north(atasehir, 50), T0.plusSeconds(60))).containsExactly(atasehir);
	}

	@Test
	void reentryRuleIsPerStore() {
		courier.enter(atasehir, T0);
		courier.moveTo(atasehir.location(), T0, 0);

		List<Store> entered = detector.enteredStores(courier, novada.location(), T0.plusSeconds(30));

		assertThat(entered).containsExactly(novada);
	}

	private static GeoPoint north(Store store, double meters) {
		return new GeoPoint(store.lat() + meters / METERS_PER_DEGREE_OF_LATITUDE, store.lng());
	}
}
