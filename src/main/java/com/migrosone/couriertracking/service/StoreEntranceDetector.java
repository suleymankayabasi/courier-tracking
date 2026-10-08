package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.distance.DistanceCalculator;
import com.migrosone.couriertracking.model.Courier;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import com.migrosone.couriertracking.repository.StoreRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StoreEntranceDetector {
	private static final Logger log = LoggerFactory.getLogger(StoreEntranceDetector.class);

	private static final double STORE_RADIUS_METERS = 100;
	private static final Duration REENTRY_THRESHOLD = Duration.ofMinutes(1);

	private final StoreRepository storeRepository;
	private final DistanceCalculator distanceCalculator;

	public StoreEntranceDetector(StoreRepository storeRepository, DistanceCalculator distanceCalculator) {
		this.storeRepository = storeRepository;
		this.distanceCalculator = distanceCalculator;
	}

	public List<Store> enteredStores(Courier courier, GeoPoint location, Instant time) {
		GeoPoint previous = courier.lastLocation();
		return storeRepository.findAll().stream()
				.filter(store -> isNear(store, location) && (previous == null || !isNear(store, previous)))
				.filter(store -> !isTooSoonAfterLastEntrance(courier, store, time))
				.toList();
	}

	private boolean isTooSoonAfterLastEntrance(Courier courier, Store store, Instant time) {
		Instant lastEntrance = courier.lastEntranceTo(store);
		boolean tooSoon = lastEntrance != null && time.isBefore(lastEntrance.plus(REENTRY_THRESHOLD));
		if (tooSoon) {
			log.debug("Re-entry of courier {} to {} at {} is not counted, last entrance was at {}",
					courier.id(), store.name(), time, lastEntrance);
		}
		return tooSoon;
	}

	private boolean isNear(Store store, GeoPoint point) {
		return distanceCalculator.distanceInMeters(store.location(), point) <= STORE_RADIUS_METERS;
	}
}
