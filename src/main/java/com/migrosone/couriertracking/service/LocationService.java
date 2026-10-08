package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.CourierLocation;
import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.dto.CourierLocationRequest;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.exception.InvalidLocationTimeException;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.repository.CourierLocationRepository;
import com.migrosone.couriertracking.repository.CourierStateRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

@Service
public class LocationService {
	private static final Logger log = LoggerFactory.getLogger(LocationService.class);
	private static final double RETRY_DELAY_MULTIPLIER = 2.0;

	private final CourierLocationRepository courierLocationRepository;
	private final CourierStateRepository courierStateRepository;
	private final ApplicationEventPublisher eventPublisher;
	private final TransactionOperations transactionOperations;
	private final RetryTemplate retryTemplate;
	private final Clock clock;
	private final Duration maxFutureSkew;

	public LocationService(
			CourierLocationRepository courierLocationRepository,
			CourierStateRepository courierStateRepository,
			ApplicationEventPublisher eventPublisher,
			TransactionOperations transactionOperations,
			Clock clock,
			CourierTrackingProperties properties) {
		this.courierLocationRepository = courierLocationRepository;
		this.courierStateRepository = courierStateRepository;
		this.eventPublisher = eventPublisher;
		this.transactionOperations = transactionOperations;
		this.retryTemplate = new RetryTemplate(retryPolicy(properties.concurrency()));
		this.clock = clock;
		this.maxFutureSkew = properties.location().maxFutureSkew();
	}

	public void recordLocation(CourierLocationRequest request) {
		requireAcceptableTime(request);
		AtomicInteger attempts = new AtomicInteger();
		retryTemplate.invoke(() -> {
			if (attempts.incrementAndGet() > 1) {
				log.debug("Retrying location of courier {} after a concurrent update (attempt {})",
						request.courierId(), attempts.get());
			}
			transactionOperations.executeWithoutResult(status -> process(request));
		});
	}

	public void recordLocations(List<CourierLocationRequest> requests) {
		requests.forEach(this::requireAcceptableTime);
		requests.stream()
				.sorted(Comparator.comparing(CourierLocationRequest::time))
				.forEach(this::recordLocation);
	}

	private void requireAcceptableTime(CourierLocationRequest request) {
		Instant latestAccepted = clock.instant().plus(maxFutureSkew);
		if (request.time().isAfter(latestAccepted)) {
			throw new InvalidLocationTimeException("time %s is more than %s in the future of the server clock"
					.formatted(request.time(), maxFutureSkew));
		}
	}

	private void process(CourierLocationRequest request) {
		String courierId = request.courierId();
		CourierPosition current = new CourierPosition(new GeoPoint(request.lat(), request.lng()), request.time());
		courierLocationRepository.save(new CourierLocation(courierId, current));

		Optional<CourierState> state = courierStateRepository.findForUpdateByCourierId(courierId);
		if (state.isPresent() && state.get().isOutOfOrder(current.time())) {
			log.warn("Ignoring out-of-order location of courier {}: {} is before the last accepted {}",
					courierId, current.time(), state.get().lastPosition().time());
			return;
		}
		CourierPosition previous = state.map(CourierState::lastPosition).orElse(null);
		eventPublisher.publishEvent(new CourierLocationEvent(courierId, current, previous));
	}

	private static RetryPolicy retryPolicy(CourierTrackingProperties.Concurrency concurrency) {
		return RetryPolicy.builder()
				.includes(ConcurrencyFailureException.class, DataIntegrityViolationException.class)
				.maxRetries(concurrency.maxRetries())
				.delay(concurrency.retryDelay())
				.jitter(concurrency.retryDelay())
				.multiplier(RETRY_DELAY_MULTIPLIER)
				.build();
	}
}
