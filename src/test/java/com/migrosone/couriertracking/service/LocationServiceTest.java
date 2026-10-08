package com.migrosone.couriertracking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.CourierLocation;
import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.dto.CourierLocationRequest;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.exception.InvalidLocationTimeException;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.repository.CourierLocationRepository;
import com.migrosone.couriertracking.repository.CourierStateRepository;
import com.migrosone.couriertracking.support.TestProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionOperations;

class LocationServiceTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final GeoPoint POINT = new GeoPoint(41.0, 29.0);
	private static final int MAX_RETRIES = 3;

	private final CourierLocationRepository locationRepository = mock(CourierLocationRepository.class);
	private final CourierStateRepository stateRepository = mock(CourierStateRepository.class);
	private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

	private final LocationService service = new LocationService(
			locationRepository, stateRepository, eventPublisher, TransactionOperations.withoutTransaction(),
			Clock.fixed(T0, ZoneOffset.UTC), TestProperties.withMaxRetries(MAX_RETRIES));

	@Test
	void firstLocationIsPublishedWithoutPreviousPosition() {
		when(stateRepository.findForUpdateByCourierId("c1")).thenReturn(Optional.empty());

		service.recordLocation(request("c1", T0, 41.5, 29.25));

		assertThat(publishedEvent()).isEqualTo(
				new CourierLocationEvent("c1", new CourierPosition(new GeoPoint(41.5, 29.25), T0), null));
	}

	@Test
	void laterLocationCarriesThePreviousAcceptedPosition() {
		givenLastAcceptedAt(T0);

		service.recordLocation(request("c1", T0.plusSeconds(10), 41.5, 29.25));

		CourierLocationEvent event = publishedEvent();
		assertThat(event.previous()).isEqualTo(new CourierPosition(POINT, T0));
		assertThat(event.current().time()).isEqualTo(T0.plusSeconds(10));
	}

	@Test
	void locationWithTheSameTimeAsTheLastAcceptedOneIsInOrder() {
		givenLastAcceptedAt(T0);

		service.recordLocation(request("c1", T0, 41.5, 29.25));

		assertThat(publishedEvent().previous()).isEqualTo(new CourierPosition(POINT, T0));
	}

	@Test
	void outOfOrderLocationIsStoredButNotPublished() {
		givenLastAcceptedAt(T0);

		service.recordLocation(request("c1", T0.minusSeconds(1), 41.5, 29.25));

		verify(locationRepository).save(any(CourierLocation.class));
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void everyLocationIsStoredInTheRawLog() {
		when(stateRepository.findForUpdateByCourierId("c1")).thenReturn(Optional.empty());

		service.recordLocation(request("c1", T0, 41.5, 29.25));

		ArgumentCaptor<CourierLocation> saved = ArgumentCaptor.forClass(CourierLocation.class);
		verify(locationRepository).save(saved.capture());
		assertThat(saved.getValue().getCourierId()).isEqualTo("c1");
		assertThat(saved.getValue().getLat()).isEqualTo(41.5);
		assertThat(saved.getValue().getLng()).isEqualTo(29.25);
		assertThat(saved.getValue().getTime()).isEqualTo(T0);
	}

	@Test
	void optimisticLockConflictIsRetried() {
		when(stateRepository.findForUpdateByCourierId("c1"))
				.thenThrow(new ObjectOptimisticLockingFailureException(CourierState.class, "c1"))
				.thenReturn(Optional.empty());

		service.recordLocation(request("c1", T0, 41.5, 29.25));

		verify(stateRepository, times(2)).findForUpdateByCourierId("c1");
		verify(eventPublisher).publishEvent(any(CourierLocationEvent.class));
	}

	@Test
	void duplicateStateInsertOfANewCourierIsRetried() {
		when(stateRepository.findForUpdateByCourierId("c1"))
				.thenThrow(new DataIntegrityViolationException("duplicate primary key"))
				.thenReturn(Optional.empty());

		service.recordLocation(request("c1", T0, 41.5, 29.25));

		verify(stateRepository, times(2)).findForUpdateByCourierId("c1");
	}

	@Test
	void persistentConflictIsRethrownWhenTheRetriesAreExhausted() {
		when(stateRepository.findForUpdateByCourierId("c1"))
				.thenThrow(new ObjectOptimisticLockingFailureException(CourierState.class, "c1"));

		assertThatThrownBy(() -> service.recordLocation(request("c1", T0, 41.5, 29.25)))
				.isInstanceOf(OptimisticLockingFailureException.class);
		verify(stateRepository, times(1 + MAX_RETRIES)).findForUpdateByCourierId("c1");
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void otherFailuresAreNotRetried() {
		when(stateRepository.findForUpdateByCourierId("c1")).thenThrow(new IllegalStateException("boom"));

		assertThatThrownBy(() -> service.recordLocation(request("c1", T0, 41.5, 29.25)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("boom");
		verify(stateRepository, times(1)).findForUpdateByCourierId("c1");
	}

	@Test
	void batchIsProcessedInEventTimeOrder() {
		when(stateRepository.findForUpdateByCourierId("c1")).thenReturn(Optional.empty());

		service.recordLocations(List.of(
				request("c1", T0.plusSeconds(20), 41.2, 29.0),
				request("c1", T0, 41.0, 29.0),
				request("c1", T0.plusSeconds(10), 41.1, 29.0)));

		ArgumentCaptor<CourierLocationEvent> events = ArgumentCaptor.forClass(CourierLocationEvent.class);
		verify(eventPublisher, times(3)).publishEvent(events.capture());
		assertThat(events.getAllValues())
				.extracting(event -> event.current().time())
				.containsExactly(T0, T0.plusSeconds(10), T0.plusSeconds(20));
	}

	@Test
	void locationWithinTheFutureSkewIsAccepted() {
		when(stateRepository.findForUpdateByCourierId("c1")).thenReturn(Optional.empty());

		service.recordLocation(request("c1", T0.plus(TestProperties.MAX_FUTURE_SKEW), 41.0, 29.0));

		verify(eventPublisher).publishEvent(any(CourierLocationEvent.class));
	}

	@Test
	void locationBeyondTheFutureSkewIsRejectedAndNothingIsStored() {
		Instant tooLate = T0.plus(TestProperties.MAX_FUTURE_SKEW).plusMillis(1);

		assertThatThrownBy(() -> service.recordLocation(request("c1", tooLate, 41.0, 29.0)))
				.isInstanceOf(InvalidLocationTimeException.class)
				.hasMessageContaining("in the future");
		verifyNoInteractions(locationRepository, stateRepository, eventPublisher);
	}

	@Test
	void oneFutureLocationRejectsTheWholeBatchBeforeAnythingIsProcessed() {
		Instant tooLate = T0.plus(TestProperties.MAX_FUTURE_SKEW).plusSeconds(1);

		assertThatThrownBy(() -> service.recordLocations(List.of(
				request("c1", T0, 41.0, 29.0),
				request("c1", tooLate, 41.1, 29.0))))
				.isInstanceOf(InvalidLocationTimeException.class);
		verifyNoInteractions(locationRepository, stateRepository, eventPublisher);
	}

	@Test
	void veryOldLocationIsAccepted() {
		when(stateRepository.findForUpdateByCourierId("c1")).thenReturn(Optional.empty());

		service.recordLocation(request("c1", T0.minusSeconds(10L * 365 * 24 * 3600), 41.0, 29.0));

		verify(eventPublisher).publishEvent(any(CourierLocationEvent.class));
	}

	private void givenLastAcceptedAt(Instant time) {
		CourierState state = CourierState.startingAt("c1", new CourierPosition(POINT, time));
		when(stateRepository.findForUpdateByCourierId("c1")).thenReturn(Optional.of(state));
	}

	private CourierLocationEvent publishedEvent() {
		ArgumentCaptor<CourierLocationEvent> captor = ArgumentCaptor.forClass(CourierLocationEvent.class);
		verify(eventPublisher).publishEvent(captor.capture());
		return captor.getValue();
	}

	private static CourierLocationRequest request(String courierId, Instant time, double lat, double lng) {
		return new CourierLocationRequest(time, courierId, lat, lng);
	}
}
