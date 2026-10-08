package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.distance.DistanceCalculator;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationException;
import com.migrosone.couriertracking.model.Courier;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import com.migrosone.couriertracking.model.StoreEntrance;
import com.migrosone.couriertracking.repository.CourierRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CourierTrackingService {
	private static final Logger log = LoggerFactory.getLogger(CourierTrackingService.class);

	private static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(1);

	private final CourierRepository courierRepository;
	private final StoreEntranceDetector storeEntranceDetector;
	private final DistanceCalculator distanceCalculator;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;

	public CourierTrackingService(
			CourierRepository courierRepository,
			StoreEntranceDetector storeEntranceDetector,
			DistanceCalculator distanceCalculator,
			ApplicationEventPublisher eventPublisher,
			Clock clock) {
		this.courierRepository = courierRepository;
		this.storeEntranceDetector = storeEntranceDetector;
		this.distanceCalculator = distanceCalculator;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
	}

	public void recordLocation(String courierId, Instant time, GeoPoint location) {
		if (time.isAfter(clock.instant().plus(ALLOWED_CLOCK_SKEW))) {
			throw new InvalidLocationException("time must not be in the future: " + time);
		}

		Courier courier = courierRepository.findOrCreate(courierId);
		List<StoreEntrance> newEntrances;
		synchronized (courier) {
			if (courier.isOutOfOrder(time)) {
				log.debug("Ignoring out-of-order location of courier {} at {}", courierId, time);
				return;
			}
			List<Store> enteredStores = storeEntranceDetector.enteredStores(courier, location, time);
			newEntrances = enteredStores.stream().map(store -> courier.enter(store, time)).toList();

			GeoPoint previous = courier.lastLocation();
			double legMeters = previous == null ? 0 : distanceCalculator.distanceInMeters(previous, location);
			courier.moveTo(location, time, legMeters);
		}
		newEntrances.forEach(eventPublisher::publishEvent);
	}

	public Double getTotalTravelDistance(String courierId) {
		return findCourier(courierId).totalDistanceMeters();
	}

	public List<StoreEntrance> getStoreEntrances(String courierId) {
		return findCourier(courierId).entrances();
	}

	private Courier findCourier(String courierId) {
		return courierRepository.findById(courierId).orElseThrow(() -> new CourierNotFoundException(courierId));
	}
}
