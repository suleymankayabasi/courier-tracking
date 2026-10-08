package com.migrosone.couriertracking.listener.entrance;

import static com.migrosone.couriertracking.support.GeoTestPoints.ATASEHIR;
import static com.migrosone.couriertracking.support.GeoTestPoints.NOVADA;
import static com.migrosone.couriertracking.support.GeoTestPoints.northOf;
import static com.migrosone.couriertracking.support.TestStores.persisted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.geo.HaversineDistanceCalculator;
import com.migrosone.couriertracking.listener.StoreEntranceListener;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import com.migrosone.couriertracking.repository.StoreRepository;
import com.migrosone.couriertracking.service.CachedStore;
import com.migrosone.couriertracking.service.StoreService;
import com.migrosone.couriertracking.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class StoreEntranceScenarioTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final CachedStore ATASEHIR_STORE = new CachedStore(1, "Ataşehir MMM Migros", ATASEHIR);
	private static final CachedStore NOVADA_STORE = new CachedStore(2, "Novada MMM Migros", NOVADA);
	private static final GeoPoint OUTSIDE_ATASEHIR = northOf(ATASEHIR, 150);
	private static final GeoPoint INSIDE_ATASEHIR = northOf(ATASEHIR, 50);

	private final List<StoreEntrance> entrances = new ArrayList<>();
	private final Map<String, CourierPosition> lastPositions = new HashMap<>();
	private final StoreEntranceListener listener = listenerWith(TestProperties.defaults());

	@Test
	void approachingFrom150To50MetersCountsOneEntrance() {
		courierAt("c1", OUTSIDE_ATASEHIR, T0);
		courierAt("c1", INSIDE_ATASEHIR, T0.plusSeconds(20));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0.plusSeconds(20));
	}

	@Test
	void stayingInsideForTenMinutesCountsOnlyOnce() {
		courierAt("c1", OUTSIDE_ATASEHIR, T0);
		for (int minute = 1; minute <= 10; minute++) {
			courierAt("c1", northOf(ATASEHIR, minute * 5), T0.plusSeconds(minute * 60L));
		}

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0.plusSeconds(60));
	}

	@Test
	void reenteringThirtySecondsAfterTheEntranceIsNotCounted() {
		courierAt("c1", INSIDE_ATASEHIR, T0);
		courierAt("c1", OUTSIDE_ATASEHIR, T0.plusSeconds(10));
		courierAt("c1", INSIDE_ATASEHIR, T0.plusSeconds(30));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0);
	}

	@Test
	void reenteringTwoMinutesAfterTheEntranceCountsAgain() {
		courierAt("c1", INSIDE_ATASEHIR, T0);
		courierAt("c1", OUTSIDE_ATASEHIR, T0.plusSeconds(60));
		courierAt("c1", INSIDE_ATASEHIR, T0.plusSeconds(120));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0, T0.plusSeconds(120));
	}

	@Test
	void reenteringExactlyOneMinuteAfterTheEntranceCounts() {
		courierAt("c1", INSIDE_ATASEHIR, T0);
		courierAt("c1", OUTSIDE_ATASEHIR, T0.plusSeconds(30));
		courierAt("c1", INSIDE_ATASEHIR, T0.plusSeconds(60));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0, T0.plusSeconds(60));
	}

	@Test
	void suppressedReentryDoesNotRestartTheCooldown() {
		courierAt("c1", INSIDE_ATASEHIR, T0);
		courierAt("c1", OUTSIDE_ATASEHIR, T0.plusSeconds(10));
		courierAt("c1", INSIDE_ATASEHIR, T0.plusSeconds(50));
		courierAt("c1", OUTSIDE_ATASEHIR, T0.plusSeconds(55));
		courierAt("c1", INSIDE_ATASEHIR, T0.plusSeconds(70));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0, T0.plusSeconds(70));
	}

	@Test
	void enteringTwoDifferentStoresCountsEachSeparately() {
		courierAt("c1", INSIDE_ATASEHIR, T0);
		courierAt("c1", NOVADA, T0.plusSeconds(30));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0);
		assertThat(entranceTimes("c1", NOVADA_STORE)).containsExactly(T0.plusSeconds(30));
	}

	@Test
	void couriersAreIndependent() {
		courierAt("c1", INSIDE_ATASEHIR, T0);
		courierAt("c2", INSIDE_ATASEHIR, T0.plusSeconds(10));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0);
		assertThat(entranceTimes("c2", ATASEHIR_STORE)).containsExactly(T0.plusSeconds(10));
	}

	@Test
	void firstLocationAlreadyInsideAStoreCountsAsEntrance() {
		courierAt("c1", INSIDE_ATASEHIR, T0);

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0);
	}

	@Test
	void locationsOutsideEveryStoreCountNothing() {
		courierAt("c1", OUTSIDE_ATASEHIR, T0);
		courierAt("c1", northOf(ATASEHIR, 300), T0.plusSeconds(10));

		assertThat(entrances).isEmpty();
	}

	@Test
	void justInsideTheRadiusIsInsideAndJustOutsideIsNot() {
		courierAt("inside", northOf(ATASEHIR, 99.9), T0);
		courierAt("outside", northOf(ATASEHIR, 100.1), T0);

		assertThat(entranceTimes("inside", ATASEHIR_STORE)).containsExactly(T0);
		assertThat(entranceTimes("outside", ATASEHIR_STORE)).isEmpty();
	}

	@Test
	void radiusAndCooldownComeFromConfiguration() {
		StoreEntranceListener custom = listenerWith(TestProperties.withEntrance(200.0, Duration.ofMinutes(5)));

		courierAt(custom, "c1", northOf(ATASEHIR, 150), T0);
		courierAt(custom, "c1", northOf(ATASEHIR, 300), T0.plusSeconds(60));
		courierAt(custom, "c1", northOf(ATASEHIR, 150), T0.plusSeconds(120));

		assertThat(entranceTimes("c1", ATASEHIR_STORE)).containsExactly(T0);
	}

	private void courierAt(String courierId, GeoPoint point, Instant time) {
		courierAt(listener, courierId, point, time);
	}

	private void courierAt(StoreEntranceListener target, String courierId, GeoPoint point, Instant time) {
		CourierPosition current = new CourierPosition(point, time);
		target.onCourierLocation(new CourierLocationEvent(courierId, current, lastPositions.put(courierId, current)));
	}

	private List<Instant> entranceTimes(String courierId, CachedStore store) {
		return entrances.stream()
				.filter(entrance -> entrance.getCourierId().equals(courierId) && entrance.getStore().getId() == store.id())
				.map(StoreEntrance::getEntranceTime)
				.toList();
	}

	private StoreEntranceListener listenerWith(CourierTrackingProperties properties) {
		StoreGeofence geofence = new StoreGeofence(new HaversineDistanceCalculator(), properties);
		StoreEntranceRepository repository = inMemoryEntranceRepository();
		StoreService storeService = mock(StoreService.class);
		when(storeService.findAll()).thenReturn(List.of(ATASEHIR_STORE, NOVADA_STORE));
		List<EntranceRule> chain = List.of(
				new WithinRadiusRule(geofence),
				new OutsideToInsideTransitionRule(geofence),
				new ReentryCooldownRule(repository, properties));
		StoreRepository storeRepository = mock(StoreRepository.class);
		when(storeRepository.getReferenceById(anyLong())).thenAnswer(invocation -> {
			long id = invocation.getArgument(0);
			CachedStore store = id == ATASEHIR_STORE.id() ? ATASEHIR_STORE : NOVADA_STORE;
			return persisted(id, store.name(), store.location());
		});
		return new StoreEntranceListener(
				storeService, chain, repository, storeRepository, mock(ApplicationEventPublisher.class));
	}

	private StoreEntranceRepository inMemoryEntranceRepository() {
		StoreEntranceRepository repository = mock(StoreEntranceRepository.class);
		when(repository.save(any(StoreEntrance.class))).thenAnswer(invocation -> {
			StoreEntrance entrance = invocation.getArgument(0);
			entrances.add(entrance);
			return entrance;
		});
		when(repository.findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc(anyString(), anyLong()))
				.thenAnswer(invocation -> entrances.stream()
						.filter(entrance -> entrance.getCourierId().equals(invocation.getArgument(0))
								&& Objects.equals(entrance.getStore().getId(), invocation.getArgument(1)))
						.max(Comparator.comparing(StoreEntrance::getEntranceTime)));
		return repository;
	}
}
