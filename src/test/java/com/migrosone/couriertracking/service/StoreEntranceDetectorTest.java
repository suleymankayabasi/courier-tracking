package com.migrosone.couriertracking.service;

import static com.migrosone.couriertracking.support.TestFixtures.ATASEHIR;
import static com.migrosone.couriertracking.support.TestFixtures.NOVADA;
import static com.migrosone.couriertracking.support.TestFixtures.START_TIME;
import static com.migrosone.couriertracking.support.TestFixtures.STORE_ENTRANCE_PROPERTIES;
import static com.migrosone.couriertracking.support.TestFixtures.metersNorthOf;
import static com.migrosone.couriertracking.support.TestFixtures.secondsAfterStart;
import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.config.StoreEntranceProperties;
import com.migrosone.couriertracking.distance.HaversineDistanceCalculator;
import com.migrosone.couriertracking.model.Courier;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoreEntranceDetectorTest {
	private final StoreEntranceDetector detector = detectorWith(STORE_ENTRANCE_PROPERTIES);
	private final Courier courier = new Courier("courier-1");

	@Test
	void detectsMoveFromOutsideToInside() {
		courier.moveTo(metersNorthOf(ATASEHIR, 300), START_TIME, 0);

		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 50), secondsAfterStart(20)))
				.containsExactly(ATASEHIR);
	}

	@Test
	void detectsFirstLocationInsideAStore() {
		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 50), START_TIME))
				.containsExactly(ATASEHIR);
	}

	@Test
	void ignoresLocationOutsideEveryStore() {
		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 300), START_TIME)).isEmpty();
	}

	@Test
	void treatsOneHundredMetersAsInside() {
		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 99.9), START_TIME))
				.containsExactly(ATASEHIR);
		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 100.1), START_TIME)).isEmpty();
	}

	@Test
	void ignoresCourierStayingInside() {
		courier.enter(ATASEHIR, START_TIME);
		courier.moveTo(metersNorthOf(ATASEHIR, 50), START_TIME, 0);

		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 20), secondsAfterStart(300))).isEmpty();
	}

	@Test
	void ignoresReentryWithinOneMinute() {
		courier.enter(ATASEHIR, START_TIME);
		courier.moveTo(metersNorthOf(ATASEHIR, 200), secondsAfterStart(20), 0);

		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 50), secondsAfterStart(59))).isEmpty();
	}

	@Test
	void detectsReentryAfterExactlyOneMinute() {
		courier.enter(ATASEHIR, START_TIME);
		courier.moveTo(metersNorthOf(ATASEHIR, 200), secondsAfterStart(30), 0);

		assertThat(detector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 50), secondsAfterStart(60)))
				.containsExactly(ATASEHIR);
	}

	@Test
	void appliesReentryThresholdPerStore() {
		courier.enter(ATASEHIR, START_TIME);
		courier.moveTo(ATASEHIR.location(), START_TIME, 0);

		assertThat(detector.findEnteredStores(courier, NOVADA.location(), secondsAfterStart(30)))
				.containsExactly(NOVADA);
	}

	@Test
	void usesConfiguredRadius() {
		StoreEntranceDetector smallRadiusDetector = detectorWith(new StoreEntranceProperties(50, Duration.ofMinutes(1)));

		assertThat(smallRadiusDetector.findEnteredStores(courier, metersNorthOf(ATASEHIR, 60), START_TIME)).isEmpty();
	}

	@Test
	void usesConfiguredReentryThreshold() {
		StoreEntranceDetector longThresholdDetector = detectorWith(new StoreEntranceProperties(100, Duration.ofMinutes(2)));
		courier.enter(ATASEHIR, START_TIME);
		courier.moveTo(metersNorthOf(ATASEHIR, 200), secondsAfterStart(30), 0);

		assertThat(longThresholdDetector.findEnteredStores(courier, ATASEHIR.location(), secondsAfterStart(90))).isEmpty();
	}

	private static StoreEntranceDetector detectorWith(StoreEntranceProperties properties) {
		return new StoreEntranceDetector(() -> List.of(ATASEHIR, NOVADA), new HaversineDistanceCalculator(), properties);
	}
}
