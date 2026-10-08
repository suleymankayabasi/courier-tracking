package com.migrosone.couriertracking.listener.entrance;

import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.service.CachedStore;
import com.migrosone.couriertracking.support.TestProperties;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutsideToInsideTransitionRuleTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final CachedStore STORE = new CachedStore(1, "Store", new GeoPoint(41.0, 29.0));
	private static final GeoPoint OUTSIDE = new GeoPoint(41.1, 29.0);
	private static final GeoPoint ON_BOUNDARY = new GeoPoint(41.2, 29.0);
	private static final GeoPoint INSIDE = new GeoPoint(41.3, 29.0);
	private static final Map<GeoPoint, Double> DISTANCE_TO_STORE = Map.of(OUTSIDE, 150.0, ON_BOUNDARY, 100.0, INSIDE, 50.0);

	private final OutsideToInsideTransitionRule rule = new OutsideToInsideTransitionRule(
			new StoreGeofence((point, store) -> DISTANCE_TO_STORE.get(point), TestProperties.defaults()));

	@Test
	void firstLocationOfACourierCountsAsComingFromOutside() {
		assertThat(rule.allows(candidate(null))).isTrue();
	}

	@Test
	void previousPositionOutsideTheStoreIsATransition() {
		assertThat(rule.allows(candidate(OUTSIDE))).isTrue();
	}

	@Test
	void previousPositionInsideTheStoreIsNotATransition() {
		assertThat(rule.allows(candidate(INSIDE))).isFalse();
	}

	@Test
	void previousPositionOnTheBoundaryWasAlreadyInside() {
		assertThat(rule.allows(candidate(ON_BOUNDARY))).isFalse();
	}

	private static EntranceCandidate candidate(GeoPoint previousPoint) {
		CourierPosition previous = previousPoint == null ? null : new CourierPosition(previousPoint, T0);
		CourierLocationEvent event = new CourierLocationEvent("c1", new CourierPosition(INSIDE, T0.plusSeconds(10)), previous);
		return new EntranceCandidate(event, STORE);
	}
}
