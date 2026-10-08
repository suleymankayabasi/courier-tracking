package com.migrosone.couriertracking.distance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.model.GeoPoint;
import org.junit.jupiter.api.Test;

class HaversineDistanceCalculatorTest {
	private static final double ONE_DEGREE_ON_GREAT_CIRCLE_METERS = 111_194.93;

	private final DistanceCalculator calculator = new HaversineDistanceCalculator();

	@Test
	void samePointIsZero() {
		GeoPoint point = new GeoPoint(40.9923307, 29.1244229);

		assertThat(calculator.distanceInMeters(point, point)).isZero();
	}

	@Test
	void oneDegreeOfLatitude() {
		double meters = calculator.distanceInMeters(new GeoPoint(40, 29), new GeoPoint(41, 29));

		assertThat(meters).isCloseTo(ONE_DEGREE_ON_GREAT_CIRCLE_METERS, within(0.01));
	}

	@Test
	void oneDegreeOfLongitudeOnTheEquator() {
		double meters = calculator.distanceInMeters(new GeoPoint(0, 0), new GeoPoint(0, 1));

		assertThat(meters).isCloseTo(ONE_DEGREE_ON_GREAT_CIRCLE_METERS, within(0.01));
	}

	@Test
	void isSymmetric() {
		GeoPoint atasehir = new GeoPoint(40.9923307, 29.1244229);
		GeoPoint ortakoy = new GeoPoint(41.055783, 29.0210292);

		assertThat(calculator.distanceInMeters(atasehir, ortakoy))
				.isEqualTo(calculator.distanceInMeters(ortakoy, atasehir))
				.isCloseTo(11_181, within(1.0));
	}
}
