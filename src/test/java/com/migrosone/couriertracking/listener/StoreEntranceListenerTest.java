package com.migrosone.couriertracking.listener;

import static com.migrosone.couriertracking.support.TestStores.persisted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.Store;
import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.event.StoreEntranceEvent;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.listener.entrance.EntranceCandidate;
import com.migrosone.couriertracking.listener.entrance.EntranceRule;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import com.migrosone.couriertracking.repository.StoreRepository;
import com.migrosone.couriertracking.service.CachedStore;
import com.migrosone.couriertracking.service.StoreService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

class StoreEntranceListenerTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final CachedStore STORE_A = new CachedStore(1, "Store A", new GeoPoint(41.0, 29.0));
	private static final CachedStore STORE_B = new CachedStore(2, "Store B", new GeoPoint(41.1, 29.0));
	private static final CourierLocationEvent EVENT =
			new CourierLocationEvent("c1", new CourierPosition(new GeoPoint(41.0, 29.0), T0), null);

	private final StoreService storeService = mock(StoreService.class);
	private final StoreEntranceRepository repository = mock(StoreEntranceRepository.class);
	private final StoreRepository storeRepository = mock(StoreRepository.class);
	private final EntranceRule first = mock(EntranceRule.class);
	private final EntranceRule second = mock(EntranceRule.class);
	private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

	private final StoreEntranceListener listener =
			new StoreEntranceListener(storeService, List.of(first, second), repository, storeRepository, eventPublisher);

	@Test
	void candidateAllowedByEveryRuleIsRecordedAndAnnounced() {
		Store storeReference = persisted(1L, "Store A", STORE_A.location());
		when(storeRepository.getReferenceById(1L)).thenReturn(storeReference);
		when(storeService.findAll()).thenReturn(List.of(STORE_A));
		when(first.allows(any())).thenReturn(true);
		when(second.allows(any())).thenReturn(true);

		listener.onCourierLocation(EVENT);

		ArgumentCaptor<StoreEntrance> saved = ArgumentCaptor.forClass(StoreEntrance.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getCourierId()).isEqualTo("c1");
		assertThat(saved.getValue().getStore()).isSameAs(storeReference);
		assertThat(saved.getValue().getEntranceTime()).isEqualTo(T0);
		verify(eventPublisher).publishEvent(new StoreEntranceEvent("c1", 1, "Store A", T0));
	}

	@Test
	void firstRejectingRuleEndsTheChain() {
		when(storeService.findAll()).thenReturn(List.of(STORE_A));
		when(first.allows(any())).thenReturn(false);

		listener.onCourierLocation(EVENT);

		verifyNoInteractions(second, eventPublisher);
		verify(repository, never()).save(any());
	}

	@Test
	void rejectionByALaterRulePreventsTheEntrance() {
		when(storeService.findAll()).thenReturn(List.of(STORE_A));
		when(first.allows(any())).thenReturn(true);
		when(second.allows(any())).thenReturn(false);

		listener.onCourierLocation(EVENT);

		verify(repository, never()).save(any());
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void everyStoreIsAnIndependentCandidate() {
		when(storeRepository.getReferenceById(2L)).thenReturn(persisted(2L, "Store B", STORE_B.location()));
		when(storeService.findAll()).thenReturn(List.of(STORE_A, STORE_B));
		when(first.allows(any())).thenAnswer(invocation -> invocation.<EntranceCandidate>getArgument(0).store() == STORE_B);
		when(second.allows(any())).thenReturn(true);

		listener.onCourierLocation(EVENT);

		ArgumentCaptor<StoreEntrance> saved = ArgumentCaptor.forClass(StoreEntrance.class);
		verify(repository).save(saved.capture());
		assertThat(saved.getValue().getStore().getId()).isEqualTo(2L);
	}
}
