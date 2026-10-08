package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.config.StoreEntranceProperties;
import com.migrosone.couriertracking.distance.DistanceCalculator;
import com.migrosone.couriertracking.model.Courier;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import com.migrosone.couriertracking.repository.StoreRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StoreEntranceDetector {
	private static final Logger log = LoggerFactory.getLogger(StoreEntranceDetector.class);

	private final StoreRepository storeRepository;
	private final DistanceCalculator distanceCalculator;
	private final StoreEntranceProperties properties;

	public StoreEntranceDetector(
			StoreRepository storeRepository,
			DistanceCalculator distanceCalculator,
			StoreEntranceProperties properties) {
		this.storeRepository = storeRepository;
		this.distanceCalculator = distanceCalculator;
		this.properties = properties;
	}

	public List<Store> findEnteredStores(Courier courier, GeoPoint location, Instant time) {
		GeoPoint previousLocation = courier.lastLocation();
		return storeRepository.findAll().stream()
				.filter(store -> hasMovedInto(store, previousLocation, location))
				.filter(store -> !isReentryWithinThreshold(courier, store, time))
				.toList();
	}

	private boolean hasMovedInto(Store store, GeoPoint previousLocation, GeoPoint location) {
		boolean wasOutside = previousLocation == null || !isWithinRadius(store, previousLocation);
		return wasOutside && isWithinRadius(store, location);
	}

	private boolean isReentryWithinThreshold(Courier courier, Store store, Instant time) {
		Instant lastEntranceTime = courier.lastEntranceTimeTo(store);
		boolean withinThreshold = lastEntranceTime != null
				&& time.isBefore(lastEntranceTime.plus(properties.reentryThreshold()));
		if (withinThreshold) {
			log.debug("Re-entry of courier {} to {} at {} is not counted, last entrance was at {}",
					courier.id(), store.name(), time, lastEntranceTime);
		}
		return withinThreshold;
	}

	private boolean isWithinRadius(Store store, GeoPoint location) {
		return distanceCalculator.distanceInMeters(store.location(), location) <= properties.radiusMeters();
	}
}
