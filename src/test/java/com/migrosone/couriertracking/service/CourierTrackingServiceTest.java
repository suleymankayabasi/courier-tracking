package com.migrosone.couriertracking.service;

import static com.migrosone.couriertracking.support.TestFixtures.ATASEHIR;
import static com.migrosone.couriertracking.support.TestFixtures.LOCATION_PROPERTIES;
import static com.migrosone.couriertracking.support.TestFixtures.START_TIME;
import static com.migrosone.couriertracking.support.TestFixtures.STORE_ENTRANCE_PROPERTIES;
import static com.migrosone.couriertracking.support.TestFixtures.metersNorthOf;
import static com.migrosone.couriertracking.support.TestFixtures.secondsAfterStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.distance.HaversineDistanceCalculator;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationException;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.StoreEntrance;
import com.migrosone.couriertracking.repository.InMemoryCourierRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CourierTrackingServiceTest {
	private static final Instant NOW = START_TIME.plusSeconds(2 * 60 * 60);
	private static final String COURIER_ID = "courier-1";

	private final List<Object> publishedEvents = new CopyOnWriteArrayList<>();
	private final HaversineDistanceCalculator distanceCalculator = new HaversineDistanceCalculator();
	private final CourierTrackingService service = new CourierTrackingService(
			new InMemoryCourierRepository(),
			new StoreEntranceDetector(() -> List.of(ATASEHIR), distanceCalculator, STORE_ENTRANCE_PROPERTIES),
			distanceCalculator,
			publishedEvents::add,
			Clock.fixed(NOW, ZoneOffset.UTC),
			LOCATION_PROPERTIES);

	@Test
	void firstLocationHasZeroDistance() {
		sendLocation(COURIER_ID, 0, metersNorthOf(ATASEHIR, 500));

		assertThat(service.getTotalTravelDistance(COURIER_ID)).isZero();
	}

	@Test
	void totalDistanceIsTheSumOfAllLegs() {
		sendLocation(COURIER_ID, 0, metersNorthOf(ATASEHIR, 500));
		sendLocation(COURIER_ID, 10, metersNorthOf(ATASEHIR, 700));
		sendLocation(COURIER_ID, 20, metersNorthOf(ATASEHIR, 400));

		assertThat(service.getTotalTravelDistance(COURIER_ID)).isCloseTo(200 + 300, within(0.5));
	}

	@Test
	void ignoresOutOfOrderLocation() {
		sendLocation(COURIER_ID, 0, metersNorthOf(ATASEHIR, 500));
		sendLocation(COURIER_ID, 60, metersNorthOf(ATASEHIR, 600));
		sendLocation(COURIER_ID, 30, ATASEHIR.location());
		sendLocation(COURIER_ID, 90, metersNorthOf(ATASEHIR, 700));

		assertThat(service.getTotalTravelDistance(COURIER_ID)).isCloseTo(200, within(0.5));
		assertThat(service.getStoreEntrances(COURIER_ID)).isEmpty();
	}

	@Test
	void duplicateLocationAddsNothing() {
		sendLocation(COURIER_ID, 0, ATASEHIR.location());
		sendLocation(COURIER_ID, 0, ATASEHIR.location());

		assertThat(service.getTotalTravelDistance(COURIER_ID)).isZero();
		assertThat(service.getStoreEntrances(COURIER_ID)).hasSize(1);
	}

	@Test
	void tracksCouriersSeparately() {
		sendLocation("courier-a", 0, metersNorthOf(ATASEHIR, 500));
		sendLocation("courier-b", 0, metersNorthOf(ATASEHIR, 1000));
		sendLocation("courier-a", 10, metersNorthOf(ATASEHIR, 600));

		assertThat(service.getTotalTravelDistance("courier-a")).isCloseTo(100, within(0.5));
		assertThat(service.getTotalTravelDistance("courier-b")).isZero();
	}

	@Test
	void unknownCourierIsNotFound() {
		assertThatThrownBy(() -> service.getTotalTravelDistance("unknown"))
				.isInstanceOf(CourierNotFoundException.class);
		assertThatThrownBy(() -> service.getStoreEntrances("unknown"))
				.isInstanceOf(CourierNotFoundException.class);
	}

	@Test
	void rejectsTimeMoreThanOneMinuteInTheFuture() {
		Instant tooFarInTheFuture = NOW.plusSeconds(61);

		assertThatThrownBy(() -> service.recordLocation(COURIER_ID, tooFarInTheFuture, ATASEHIR.location()))
				.isInstanceOf(InvalidLocationException.class);
		assertThatThrownBy(() -> service.getTotalTravelDistance(COURIER_ID))
				.isInstanceOf(CourierNotFoundException.class);
	}

	@Test
	void toleratesClockSkewOfOneMinute() {
		service.recordLocation(COURIER_ID, NOW.plusSeconds(60), ATASEHIR.location());

		assertThat(service.getStoreEntrances(COURIER_ID)).hasSize(1);
	}

	@Test
	void recordsAndPublishesEntrance() {
		sendLocation(COURIER_ID, 0, metersNorthOf(ATASEHIR, 300));
		sendLocation(COURIER_ID, 20, metersNorthOf(ATASEHIR, 50));

		StoreEntrance expectedEntrance = new StoreEntrance(COURIER_ID, ATASEHIR.name(), secondsAfterStart(20));
		assertThat(service.getStoreEntrances(COURIER_ID)).containsExactly(expectedEntrance);
		assertThat(publishedEvents).containsExactly(expectedEntrance);
	}

	@Test
	void appliesEntranceRuleAcrossAStreamOfLocations() {
		sendLocation(COURIER_ID, 0, metersNorthOf(ATASEHIR, 300));
		sendLocation(COURIER_ID, 20, metersNorthOf(ATASEHIR, 50));
		sendLocation(COURIER_ID, 40, metersNorthOf(ATASEHIR, 20));
		sendLocation(COURIER_ID, 60, metersNorthOf(ATASEHIR, 200));
		sendLocation(COURIER_ID, 70, metersNorthOf(ATASEHIR, 50));
		sendLocation(COURIER_ID, 90, metersNorthOf(ATASEHIR, 200));
		sendLocation(COURIER_ID, 150, metersNorthOf(ATASEHIR, 50));

		assertThat(service.getStoreEntrances(COURIER_ID))
				.extracting(StoreEntrance::time)
				.containsExactly(secondsAfterStart(20), secondsAfterStart(150));
	}

	@Test
	void appliesReentryThresholdPerCourier() {
		sendLocation("courier-a", 0, ATASEHIR.location());
		sendLocation("courier-b", 10, ATASEHIR.location());

		assertThat(service.getStoreEntrances("courier-a")).hasSize(1);
		assertThat(service.getStoreEntrances("courier-b")).hasSize(1);
	}

	@Test
	void concurrentLocationsOfOneCourierInsideAStoreGiveOneEntrance() throws InterruptedException {
		try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
			for (int second = 0; second < 1_000; second++) {
				int secondsAfterStart = second;
				executor.submit(() -> sendLocation(COURIER_ID, secondsAfterStart, ATASEHIR.location()));
			}
			executor.shutdown();
			assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(service.getStoreEntrances(COURIER_ID)).hasSize(1);
		assertThat(service.getTotalTravelDistance(COURIER_ID)).isZero();
	}

	private void sendLocation(String courierId, long secondsAfterStart, GeoPoint location) {
		service.recordLocation(courierId, secondsAfterStart(secondsAfterStart), location);
	}
}
