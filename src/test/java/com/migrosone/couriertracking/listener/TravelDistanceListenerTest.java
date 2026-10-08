package com.migrosone.couriertracking.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.geo.HaversineDistanceCalculator;
import com.migrosone.couriertracking.repository.CourierStateRepository;
import com.migrosone.couriertracking.support.GeoTestPoints;
import com.migrosone.couriertracking.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TravelDistanceListenerTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final GeoPoint START = new GeoPoint(41.0, 29.0);

	private final CourierStateRepository repository = mock(CourierStateRepository.class);

	@Test
	void firstLocationStartsTheCourierAtZeroMeters() {
		TravelDistanceListener listener = listenerWith(TestProperties.defaults());

		listener.onCourierLocation(new CourierLocationEvent("c1", position(START, T0), null));

		ArgumentCaptor<CourierState> saved = ArgumentCaptor.forClass(CourierState.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getCourierId()).isEqualTo("c1");
		assertThat(saved.getValue().getTotalDistanceMeters()).isZero();
		assertThat(saved.getValue().lastPosition()).isEqualTo(position(START, T0));
	}

	@Test
	void consecutiveLocationsAccumulateTheirLegs() {
		TravelDistanceListener listener = listenerWith(TestProperties.defaults());
		CourierState state = existingState(START, T0);
		GeoPoint second = GeoTestPoints.northOf(START, 100);
		GeoPoint third = GeoTestPoints.northOf(second, 250);

		listener.onCourierLocation(new CourierLocationEvent("c1", position(second, T0.plusSeconds(10)), position(START, T0)));
		listener.onCourierLocation(new CourierLocationEvent(
				"c1", position(third, T0.plusSeconds(20)), position(second, T0.plusSeconds(10))));

		assertThat(state.getTotalDistanceMeters()).isCloseTo(350.0, within(1e-6));
		assertThat(state.lastPosition()).isEqualTo(position(third, T0.plusSeconds(20)));
	}

	@Test
	void longGapBetweenPingsAddsTheStraightLineDistanceLikeAnyOtherLeg() {
		TravelDistanceListener listener = listenerWith(TestProperties.defaults());
		CourierState state = existingState(START, T0);
		GeoPoint later = GeoTestPoints.northOf(START, 500);
		Instant afterSixHours = T0.plus(Duration.ofHours(6));

		listener.onCourierLocation(new CourierLocationEvent("c1", position(later, afterSixHours), position(START, T0)));

		assertThat(state.getTotalDistanceMeters()).isCloseTo(500.0, within(1e-6));
		assertThat(state.lastPosition()).isEqualTo(position(later, afterSixHours));
	}

	@Test
	void laterLocationOfACourierWithoutStateIsAnInconsistency() {
		TravelDistanceListener listener = listenerWith(TestProperties.defaults());
		when(repository.findById("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> listener.onCourierLocation(
				new CourierLocationEvent("ghost", position(START, T0.plusSeconds(10)), position(START, T0))))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("No state for courier ghost");
	}

	@Test
	void stayingAtTheSamePointAddsNothing() {
		TravelDistanceListener listener = listenerWith(TestProperties.defaults());
		CourierState state = existingState(START, T0);

		listener.onCourierLocation(new CourierLocationEvent("c1", position(START, T0.plusSeconds(10)), position(START, T0)));

		assertThat(state.getTotalDistanceMeters()).isZero();
	}

	@Test
	void noiseFilterIgnoresLegsBelowTheThresholdButKeepsTheClockMoving() {
		TravelDistanceListener listener = listenerWith(TestProperties.withMinMovement(3.0));
		CourierState state = existingState(START, T0);
		GeoPoint jitter = GeoTestPoints.northOf(START, 2);

		listener.onCourierLocation(new CourierLocationEvent("c1", position(jitter, T0.plusSeconds(10)), position(START, T0)));

		assertThat(state.getTotalDistanceMeters()).isZero();
		assertThat(state.lastPosition()).isEqualTo(position(START, T0.plusSeconds(10)));
	}

	@Test
	void noiseFilterCountsLegsAtOrAboveTheThreshold() {
		TravelDistanceListener listener = listenerWith(TestProperties.withMinMovement(3.0));
		CourierState state = existingState(START, T0);
		GeoPoint moved = GeoTestPoints.northOf(START, 4);

		listener.onCourierLocation(new CourierLocationEvent("c1", position(moved, T0.plusSeconds(10)), position(START, T0)));

		assertThat(state.getTotalDistanceMeters()).isCloseTo(4.0, within(1e-6));
		assertThat(state.lastPosition().point()).isEqualTo(moved);
	}

	private TravelDistanceListener listenerWith(CourierTrackingProperties properties) {
		return new TravelDistanceListener(repository, new HaversineDistanceCalculator(), properties);
	}

	private CourierState existingState(GeoPoint point, Instant time) {
		CourierState state = CourierState.startingAt("c1", position(point, time));
		when(repository.findById("c1")).thenReturn(Optional.of(state));
		return state;
	}

	private static CourierPosition position(GeoPoint point, Instant time) {
		return new CourierPosition(point, time);
	}
}
