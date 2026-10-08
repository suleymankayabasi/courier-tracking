package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.distance.DistanceCalculator;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationException;
import com.migrosone.couriertracking.model.Courier;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import com.migrosone.couriertracking.model.StoreEntrance;
import com.migrosone.couriertracking.repository.CourierRepository;
import com.migrosone.couriertracking.repository.StoreRepository;
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

	static final double STORE_RADIUS_METERS = 100;
	static final Duration REENTRY_THRESHOLD = Duration.ofMinutes(1);
	static final Duration ALLOWED_CLOCK_SKEW = Duration.ofMinutes(1);

	private final CourierRepository courierRepository;
	private final StoreRepository storeRepository;
	private final DistanceCalculator distanceCalculator;
	private final ApplicationEventPublisher eventPublisher;

	public CourierTrackingService(
			CourierRepository courierRepository,
			StoreRepository storeRepository,
			DistanceCalculator distanceCalculator,
			ApplicationEventPublisher eventPublisher) {
		this.courierRepository = courierRepository;
		this.storeRepository = storeRepository;
		this.distanceCalculator = distanceCalculator;
		this.eventPublisher = eventPublisher;
	}

	public void recordLocation(String courierId, Instant time, GeoPoint location) {
		// A far-future time would make every later, correct location of this courier look out of order.
		if (time.isAfter(Instant.now().plus(ALLOWED_CLOCK_SKEW))) {
			throw new InvalidLocationException("time must not be in the future: " + time);
		}

		Courier courier = courierRepository.findOrCreate(courierId);
		// Locations of the same courier are processed one at a time; different couriers run in parallel.
		synchronized (courier) {
			if (courier.isOutOfOrder(time)) {
				log.warn("Ignoring out-of-order location of courier {} at {}", courierId, time);
				return;
			}
			GeoPoint previous = courier.lastLocation();
			for (Store store : storeRepository.findAll()) {
				if (isEntrance(courier, store, previous, location, time)) {
					eventPublisher.publishEvent(courier.enter(store, time));
				}
			}
			double legMeters = previous == null ? 0 : distanceCalculator.distanceInMeters(previous, location);
			courier.moveTo(location, time, legMeters);
		}
	}

	public Double getTotalTravelDistance(String courierId) {
		return findCourier(courierId).totalDistanceMeters();
	}

	public List<StoreEntrance> getStoreEntrances(String courierId) {
		return findCourier(courierId).entrances();
	}

	/**
	 * An entrance is counted when the courier moves from outside to inside the store radius, unless the last counted
	 * entrance to the same store was less than {@link #REENTRY_THRESHOLD} ago.
	 */
	private boolean isEntrance(Courier courier, Store store, GeoPoint previous, GeoPoint current, Instant time) {
		boolean movedIn = isNear(store, current) && (previous == null || !isNear(store, previous));
		if (!movedIn) {
			return false;
		}
		Instant lastEntrance = courier.lastEntranceTo(store);
		if (lastEntrance != null && time.isBefore(lastEntrance.plus(REENTRY_THRESHOLD))) {
			log.debug("Re-entry of courier {} to {} at {} is not counted, last entrance was at {}",
					courier.id(), store.name(), time, lastEntrance);
			return false;
		}
		return true;
	}

	private boolean isNear(Store store, GeoPoint point) {
		return distanceCalculator.distanceInMeters(store.location(), point) <= STORE_RADIUS_METERS;
	}

	private Courier findCourier(String courierId) {
		return courierRepository.findById(courierId).orElseThrow(() -> new CourierNotFoundException(courierId));
	}
}
