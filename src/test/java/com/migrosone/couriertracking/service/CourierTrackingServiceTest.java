package com.migrosone.couriertracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.distance.HaversineDistanceCalculator;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationException;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.StoreEntrance;
import com.migrosone.couriertracking.repository.CourierRepository;
import com.migrosone.couriertracking.repository.StoreRepository;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CourierTrackingServiceTest {
	private static final double METERS_PER_DEGREE_OF_LATITUDE = 111_194.93;
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final GeoPoint ATASEHIR = new GeoPoint(40.9923307, 29.1244229);
	private static final GeoPoint NOVADA = new GeoPoint(40.986106, 29.1161293);
	private static final String ATASEHIR_NAME = "Ataşehir MMM Migros";
	private static final String COURIER = "courier-1";

	private final List<Object> publishedEvents = new CopyOnWriteArrayList<>();
	private final CourierTrackingService service = new CourierTrackingService(
			new CourierRepository(),
			new StoreRepository(JsonMapper.builder().build()),
			new HaversineDistanceCalculator(),
			publishedEvents::add);

	// Distance

	@Test
	void firstLocationHasZeroDistance() {
		record(COURIER, 0, north(ATASEHIR, 500));

		assertThat(service.getTotalTravelDistance(COURIER)).isZero();
	}

	@Test
	void distanceIsTheSumOfAllLegs() {
		record(COURIER, 0, north(ATASEHIR, 500));
		record(COURIER, 10, north(ATASEHIR, 700));
		record(COURIER, 20, north(ATASEHIR, 400));

		assertThat(service.getTotalTravelDistance(COURIER)).isCloseTo(200 + 300, within(0.5));
	}

	@Test
	void outOfOrderLocationIsIgnored() {
		record(COURIER, 0, north(ATASEHIR, 500));
		record(COURIER, 60, north(ATASEHIR, 600));
		record(COURIER, 30, ATASEHIR);
		record(COURIER, 90, north(ATASEHIR, 700));

		assertThat(service.getTotalTravelDistance(COURIER)).isCloseTo(200, within(0.5));
		assertThat(service.getStoreEntrances(COURIER)).isEmpty();
	}

	@Test
	void duplicateLocationAddsNothing() {
		record(COURIER, 0, ATASEHIR);
		record(COURIER, 0, ATASEHIR);

		assertThat(service.getTotalTravelDistance(COURIER)).isZero();
		assertThat(service.getStoreEntrances(COURIER)).hasSize(1);
	}

	@Test
	void couriersAreTrackedSeparately() {
		record("a", 0, north(ATASEHIR, 500));
		record("b", 0, north(ATASEHIR, 1000));
		record("a", 10, north(ATASEHIR, 600));

		assertThat(service.getTotalTravelDistance("a")).isCloseTo(100, within(0.5));
		assertThat(service.getTotalTravelDistance("b")).isZero();
	}

	@Test
	void unknownCourierIsNotFound() {
		assertThatThrownBy(() -> service.getTotalTravelDistance("nobody")).isInstanceOf(CourierNotFoundException.class);
		assertThatThrownBy(() -> service.getStoreEntrances("nobody")).isInstanceOf(CourierNotFoundException.class);
	}

	@Test
	void futureTimeIsRejected() {
		Instant tomorrow = Instant.now().plusSeconds(24 * 60 * 60);

		assertThatThrownBy(() -> service.recordLocation(COURIER, tomorrow, ATASEHIR))
				.isInstanceOf(InvalidLocationException.class);
		assertThatThrownBy(() -> service.getTotalTravelDistance(COURIER)).isInstanceOf(CourierNotFoundException.class);
	}

	// Store entrances

	@Test
	void enteringTheRadiusIsAnEntrance() {
		record(COURIER, 0, north(ATASEHIR, 300));
		record(COURIER, 20, north(ATASEHIR, 50));

		StoreEntrance expected = new StoreEntrance(COURIER, ATASEHIR_NAME, T0.plusSeconds(20));
		assertThat(service.getStoreEntrances(COURIER)).containsExactly(expected);
		assertThat(publishedEvents).containsExactly(expected);
	}

	@Test
	void firstLocationInsideTheRadiusIsAnEntrance() {
		record(COURIER, 0, north(ATASEHIR, 50));

		assertThat(service.getStoreEntrances(COURIER)).hasSize(1);
	}

	@Test
	void radiusIsOneHundredMeters() {
		record("inside", 0, north(ATASEHIR, 99.9));
		record("outside", 0, north(ATASEHIR, 100.1));

		assertThat(service.getStoreEntrances("inside")).hasSize(1);
		assertThat(service.getStoreEntrances("outside")).isEmpty();
	}

	@Test
	void stayingInsideIsNotANewEntrance() {
		record(COURIER, 0, north(ATASEHIR, 50));
		record(COURIER, 120, north(ATASEHIR, 20));
		record(COURIER, 300, north(ATASEHIR, 60));

		assertThat(service.getStoreEntrances(COURIER)).hasSize(1);
	}

	@Test
	void reentryWithinOneMinuteIsNotCounted() {
		record(COURIER, 0, north(ATASEHIR, 50));
		record(COURIER, 20, north(ATASEHIR, 200));
		record(COURIER, 59, north(ATASEHIR, 50));

		assertThat(service.getStoreEntrances(COURIER)).extracting(StoreEntrance::time).containsExactly(T0);
	}

	@Test
	void reentryAfterOneMinuteIsCounted() {
		record(COURIER, 0, north(ATASEHIR, 50));
		record(COURIER, 30, north(ATASEHIR, 200));
		record(COURIER, 60, north(ATASEHIR, 50));

		assertThat(service.getStoreEntrances(COURIER)).extracting(StoreEntrance::time)
				.containsExactly(T0, T0.plusSeconds(60));
	}

	@Test
	void reentryRuleIsPerStore() {
		record(COURIER, 0, ATASEHIR);
		record(COURIER, 30, NOVADA);

		assertThat(service.getStoreEntrances(COURIER)).extracting(StoreEntrance::storeName)
				.containsExactly(ATASEHIR_NAME, "Novada MMM Migros");
	}

	@Test
	void reentryRuleIsPerCourier() {
		record("a", 0, ATASEHIR);
		record("b", 10, ATASEHIR);

		assertThat(service.getStoreEntrances("a")).hasSize(1);
		assertThat(service.getStoreEntrances("b")).hasSize(1);
	}

	@Test
	void concurrentLocationsOfOneCourierInsideAStoreGiveOneEntrance() throws InterruptedException {
		try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
			for (int second = 0; second < 1_000; second++) {
				int time = second;
				executor.submit(() -> record(COURIER, time, ATASEHIR));
			}
			executor.shutdown();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(service.getStoreEntrances(COURIER)).hasSize(1);
		assertThat(service.getTotalTravelDistance(COURIER)).isZero();
	}

	private void record(String courierId, int secondsAfterT0, GeoPoint location) {
		service.recordLocation(courierId, T0.plusSeconds(secondsAfterT0), location);
	}

	private static GeoPoint north(GeoPoint point, double meters) {
		return new GeoPoint(point.lat() + meters / METERS_PER_DEGREE_OF_LATITUDE, point.lng());
	}
}
