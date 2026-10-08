package com.migrosone.couriertracking.geo;

import static com.migrosone.couriertracking.support.GeoTestPoints.ATASEHIR;
import static com.migrosone.couriertracking.support.GeoTestPoints.NOVADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.support.GeoTestPoints;
import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTest {
	private static final double ONE_DEGREE_METERS = 111_194.93;

	private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

	@Test
	void sameCoordinatesAreZeroMetersApart() {
		assertThat(calculator.distanceInMeters(ATASEHIR, ATASEHIR)).isEqualTo(0.0);
	}

	@Test
	void oneDegreeOfLatitudeAtEquator() {
		double distance = calculator.distanceInMeters(new GeoPoint(0, 0), new GeoPoint(1, 0));

		assertThat(distance).isCloseTo(ONE_DEGREE_METERS, within(0.01));
	}

	@Test
	void oneDegreeOfLongitudeAtEquator() {
		double distance = calculator.distanceInMeters(new GeoPoint(0, 0), new GeoPoint(0, 1));

		assertThat(distance).isCloseTo(ONE_DEGREE_METERS, within(0.01));
	}

	@Test
	void distanceIsSymmetric() {
		assertThat(calculator.distanceInMeters(ATASEHIR, NOVADA))
				.isCloseTo(calculator.distanceInMeters(NOVADA, ATASEHIR), within(1e-9));
	}

	@Test
	void crossingTheAntimeridianTakesTheShortWay() {
		double expected = Earth.MEAN_RADIUS_METERS * Math.toRadians(0.2);

		double distance = calculator.distanceInMeters(new GeoPoint(0, 179.9), new GeoPoint(0, -179.9));

		assertThat(distance).isCloseTo(expected, within(0.01));
	}

	@Test
	void knownRealWorldDistanceBetweenTwoStores() {
		assertThat(calculator.distanceInMeters(ATASEHIR, NOVADA)).isCloseTo(982.0, within(982.0 * 0.01));
	}

	@Test
	void publishedReferenceDistanceFromNashvilleToLosAngeles() {
		double referenceMeters = 2_887_259.95;

		double distance = calculator.distanceInMeters(new GeoPoint(36.12, -86.67), new GeoPoint(33.94, -118.40));

		assertThat(distance).isCloseTo(referenceMeters, within(referenceMeters * 0.001));
	}

	@Test
	void consecutiveLegsAddUp() {
		GeoPoint start = new GeoPoint(41.0, 29.0);
		GeoPoint second = GeoTestPoints.northOf(start, 100.0);
		GeoPoint third = GeoTestPoints.northOf(second, 250.0);

		double total = calculator.distanceInMeters(start, second) + calculator.distanceInMeters(second, third);

		assertThat(total).isCloseTo(350.0, within(1e-6));
	}
}
