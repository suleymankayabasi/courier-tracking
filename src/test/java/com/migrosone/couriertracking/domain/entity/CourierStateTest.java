package com.migrosone.couriertracking.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.geo.GeoPoint;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CourierStateTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final CourierPosition START = new CourierPosition(new GeoPoint(41.0, 29.0), T0);

	@Test
	void olderEventIsOutOfOrderButEqualTimeIsNot() {
		CourierState state = CourierState.startingAt("c1", START);

		assertThat(state.isOutOfOrder(T0.minusMillis(1))).isTrue();
		assertThat(state.isOutOfOrder(T0)).isFalse();
		assertThat(state.isOutOfOrder(T0.plusSeconds(1))).isFalse();
	}

	@ParameterizedTest
	@ValueSource(doubles = { -1.0, Double.NaN, Double.POSITIVE_INFINITY })
	void invalidLegIsRejected(double legMeters) {
		CourierState state = CourierState.startingAt("c1", START);

		assertThatThrownBy(() -> state.travelTo(START, legMeters)).isInstanceOf(IllegalArgumentException.class);
		assertThat(state.getTotalDistanceMeters()).isZero();
	}
}
