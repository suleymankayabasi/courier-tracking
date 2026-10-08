package com.migrosone.couriertracking.service;

import static com.migrosone.couriertracking.support.TestStores.persisted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.dto.StoreEntranceResponse;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.repository.CourierStateRepository;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CourierServiceTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final GeoPoint POINT = new GeoPoint(41.0, 29.0);

	private final CourierStateRepository stateRepository = mock(CourierStateRepository.class);
	private final StoreEntranceRepository entranceRepository = mock(StoreEntranceRepository.class);
	private final CourierService service = new CourierService(stateRepository, entranceRepository);

	@Test
	void totalTravelDistanceOfAKnownCourier() {
		CourierState state = CourierState.startingAt("c1", new CourierPosition(POINT, T0));
		state.travelTo(new CourierPosition(POINT, T0.plusSeconds(10)), 1234.5);
		when(stateRepository.findById("c1")).thenReturn(Optional.of(state));

		assertThat(service.getTotalTravelDistance("c1")).isEqualTo(1234.5);
	}

	@Test
	void totalTravelDistanceOfAnUnknownCourierIsNotFound() {
		when(stateRepository.findById("ghost")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getTotalTravelDistance("ghost"))
				.isInstanceOf(CourierNotFoundException.class)
				.hasMessage("Courier not found: ghost");
	}

	@Test
	void storeEntrancesAreMappedWithTheStoreName() {
		when(stateRepository.existsById("c1")).thenReturn(true);
		when(entranceRepository.findByCourierIdOrderByEntranceTimeAscIdAsc("c1")).thenReturn(List.of(
				new StoreEntrance("c1", persisted(1L, "Store A", POINT), T0),
				new StoreEntrance("c1", persisted(2L, "Store B", POINT), T0.plusSeconds(90))));

		assertThat(service.getStoreEntrances("c1")).containsExactly(
				new StoreEntranceResponse(1, "Store A", T0),
				new StoreEntranceResponse(2, "Store B", T0.plusSeconds(90)));
	}

	@Test
	void knownCourierWithoutEntrancesHasAnEmptyList() {
		when(stateRepository.existsById("c1")).thenReturn(true);
		when(entranceRepository.findByCourierIdOrderByEntranceTimeAscIdAsc("c1")).thenReturn(List.of());

		assertThat(service.getStoreEntrances("c1")).isEmpty();
	}

	@Test
	void storeEntrancesOfAnUnknownCourierAreNotFound() {
		when(stateRepository.existsById("ghost")).thenReturn(false);

		assertThatThrownBy(() -> service.getStoreEntrances("ghost")).isInstanceOf(CourierNotFoundException.class);
	}
}
