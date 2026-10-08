package com.migrosone.couriertracking.distance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.model.GeoPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HaversineDistanceCalculatorTest {
	private static final double ONE_DEGREE_ON_GREAT_CIRCLE_METERS = 111_194.93;

	private final DistanceCalculator calculator = new HaversineDistanceCalculator();

	@Test
	void returnsZeroForTheSamePoint() {
		GeoPoint point = new GeoPoint(40.9923307, 29.1244229);

		assertThat(calculator.distanceInMeters(point, point)).isZero();
	}

	@Test
	void measuresOneDegreeOfLatitude() {
		double meters = calculator.distanceInMeters(new GeoPoint(40, 29), new GeoPoint(41, 29));

		assertThat(meters).isCloseTo(ONE_DEGREE_ON_GREAT_CIRCLE_METERS, within(0.01));
	}

	@Test
	void measuresOneDegreeOfLongitudeOnTheEquator() {
		double meters = calculator.distanceInMeters(new GeoPoint(0, 0), new GeoPoint(0, 1));

		assertThat(meters).isCloseTo(ONE_DEGREE_ON_GREAT_CIRCLE_METERS, within(0.01));
	}

	@Test
	void isSymmetricBetweenTwoStores() {
		GeoPoint atasehir = new GeoPoint(40.9923307, 29.1244229);
		GeoPoint ortakoy = new GeoPoint(41.055783, 29.0210292);

		assertThat(calculator.distanceInMeters(atasehir, ortakoy))
				.isEqualTo(calculator.distanceInMeters(ortakoy, atasehir))
				.isCloseTo(11_181, within(1.0));
	}

	@ParameterizedTest
	@CsvSource({"0, 0, 0, 180", "45, 0, -45, 180", "40.9923307, 29.1244229, -40.9923307, -150.8755771"})
	void measuresHalfTheEarthsCircumferenceBetweenAntipodalPoints(
			double fromLat, double fromLng, double toLat, double toLng) {
		double meters = calculator.distanceInMeters(new GeoPoint(fromLat, fromLng), new GeoPoint(toLat, toLng));

		assertThat(meters).isCloseTo(Math.PI * 6_371_000, within(1.0));
	}
}
