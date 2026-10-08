package com.migrosone.couriertracking.listener.entrance;

import static com.migrosone.couriertracking.support.TestStores.persisted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import com.migrosone.couriertracking.service.CachedStore;
import com.migrosone.couriertracking.support.TestProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReentryCooldownRuleTest {
	private static final Instant LAST_ENTRANCE = Instant.parse("2026-01-01T10:00:00Z");
	private static final GeoPoint POINT = new GeoPoint(41.0, 29.0);
	private static final CachedStore STORE = new CachedStore(7, "Store", POINT);

	private final StoreEntranceRepository repository = mock(StoreEntranceRepository.class);
	private final ReentryCooldownRule rule = new ReentryCooldownRule(repository, TestProperties.defaults());

	@Test
	void firstEntranceToAStoreIsAllowed() {
		when(repository.findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc("c1", 7L)).thenReturn(Optional.empty());

		assertThat(rule.allows(candidateAt(LAST_ENTRANCE))).isTrue();
	}

	@Test
	void reentryWithinTheCooldownIsRejected() {
		givenLastEntranceAt(LAST_ENTRANCE);

		assertThat(rule.allows(candidateAt(LAST_ENTRANCE.plusSeconds(30)))).isFalse();
	}

	@Test
	void reentryOneMillisecondBeforeTheCooldownEndsIsRejected() {
		givenLastEntranceAt(LAST_ENTRANCE);

		assertThat(rule.allows(candidateAt(LAST_ENTRANCE.plus(Duration.ofSeconds(60).minusMillis(1))))).isFalse();
	}

	@Test
	void reentryExactlyOneCooldownLaterIsAllowed() {
		givenLastEntranceAt(LAST_ENTRANCE);

		assertThat(rule.allows(candidateAt(LAST_ENTRANCE.plusSeconds(60)))).isTrue();
	}

	@Test
	void reentryAfterTheCooldownIsAllowed() {
		givenLastEntranceAt(LAST_ENTRANCE);

		assertThat(rule.allows(candidateAt(LAST_ENTRANCE.plusSeconds(61)))).isTrue();
	}

	@Test
	void cooldownComesFromConfiguration() {
		ReentryCooldownRule twoMinutes = new ReentryCooldownRule(
				repository, TestProperties.withEntrance(100.0, Duration.ofMinutes(2)));
		givenLastEntranceAt(LAST_ENTRANCE);

		assertThat(twoMinutes.allows(candidateAt(LAST_ENTRANCE.plusSeconds(90)))).isFalse();
		assertThat(twoMinutes.allows(candidateAt(LAST_ENTRANCE.plusSeconds(120)))).isTrue();
	}

	@Test
	void looksUpTheLastEntranceOfThisCourierToThisStore() {
		when(repository.findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc("c1", 7L)).thenReturn(Optional.empty());

		rule.allows(candidateAt(LAST_ENTRANCE));

		verify(repository).findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc("c1", 7L);
	}

	private void givenLastEntranceAt(Instant time) {
		when(repository.findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc("c1", 7L))
				.thenReturn(Optional.of(new StoreEntrance("c1", persisted(7L, "Store", POINT), time)));
	}

	private static EntranceCandidate candidateAt(Instant time) {
		return new EntranceCandidate(new CourierLocationEvent("c1", new CourierPosition(POINT, time), null), STORE);
	}
}
