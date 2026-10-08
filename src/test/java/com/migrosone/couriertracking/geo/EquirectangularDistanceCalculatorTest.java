package com.migrosone.couriertracking.geo;

import static com.migrosone.couriertracking.support.GeoTestPoints.ATASEHIR;
import static com.migrosone.couriertracking.support.GeoTestPoints.NOVADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.support.GeoTestPoints;
import org.junit.jupiter.api.Test;

class EquirectangularDistanceCalculatorTest {
	private final EquirectangularDistanceCalculator calculator = new EquirectangularDistanceCalculator();
	private final HaversineDistanceCalculator haversine = new HaversineDistanceCalculator();

	@Test
	void sameCoordinatesAreZeroMetersApart() {
		assertThat(calculator.distanceInMeters(ATASEHIR, ATASEHIR)).isEqualTo(0.0);
	}

	@Test
	void distanceAlongAMeridianIsExact() {
		GeoPoint north = GeoTestPoints.northOf(ATASEHIR, 100.0);

		assertThat(calculator.distanceInMeters(ATASEHIR, north)).isCloseTo(100.0, within(1e-6));
	}

	@Test
	void agreesWithHaversineForCityDistances() {
		double expected = haversine.distanceInMeters(ATASEHIR, NOVADA);

		assertThat(calculator.distanceInMeters(ATASEHIR, NOVADA)).isCloseTo(expected, within(expected * 1e-4));
	}

	@Test
	void crossingTheAntimeridianTakesTheShortWay() {
		double expected = Earth.MEAN_RADIUS_METERS * Math.toRadians(0.2);

		double distance = calculator.distanceInMeters(new GeoPoint(0, 179.9), new GeoPoint(0, -179.9));

		assertThat(distance).isCloseTo(expected, within(0.01));
	}

	@Test
	void crossingTheAntimeridianTakesTheShortWayInTheOtherDirectionToo() {
		double expected = Earth.MEAN_RADIUS_METERS * Math.toRadians(0.2);

		double distance = calculator.distanceInMeters(new GeoPoint(0, -179.9), new GeoPoint(0, 179.9));

		assertThat(distance).isCloseTo(expected, within(0.01));
	}
}
