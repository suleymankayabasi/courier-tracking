package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.config.LocationProperties;
import com.migrosone.couriertracking.distance.DistanceCalculator;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationException;
import com.migrosone.couriertracking.model.Courier;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.StoreEntrance;
import com.migrosone.couriertracking.repository.CourierRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class CourierTrackingService {
	private static final Logger log = LoggerFactory.getLogger(CourierTrackingService.class);

	private final CourierRepository courierRepository;
	private final StoreEntranceDetector storeEntranceDetector;
	private final DistanceCalculator distanceCalculator;
	private final ApplicationEventPublisher eventPublisher;
	private final Clock clock;
	private final LocationProperties properties;

	public CourierTrackingService(
			CourierRepository courierRepository,
			StoreEntranceDetector storeEntranceDetector,
			DistanceCalculator distanceCalculator,
			ApplicationEventPublisher eventPublisher,
			Clock clock,
			LocationProperties properties) {
		this.courierRepository = courierRepository;
		this.storeEntranceDetector = storeEntranceDetector;
		this.distanceCalculator = distanceCalculator;
		this.eventPublisher = eventPublisher;
		this.clock = clock;
		this.properties = properties;
	}

	public void recordLocation(String courierId, Instant time, GeoPoint location) {
		requireNotInFuture(time);
		List<StoreEntrance> newEntrances =
				courierRepository.updateAtomically(courierId, courier -> applyLocation(courier, time, location));
		newEntrances.forEach(eventPublisher::publishEvent);
	}

	public double getTotalTravelDistance(String courierId) {
		return findCourier(courierId).totalDistanceMeters();
	}

	public List<StoreEntrance> getStoreEntrances(String courierId) {
		return findCourier(courierId).entrances();
	}

	private List<StoreEntrance> applyLocation(Courier courier, Instant time, GeoPoint location) {
		if (courier.isOutOfOrder(time)) {
			log.debug("Ignoring out-of-order location of courier {} at {}", courier.id(), time);
			return List.of();
		}
		List<StoreEntrance> newEntrances = storeEntranceDetector.findEnteredStores(courier, location, time).stream()
				.map(store -> courier.enter(store, time))
				.toList();
		courier.moveTo(location, time, distanceFromLastLocation(courier, location));
		return newEntrances;
	}

	private void requireNotInFuture(Instant time) {
		Instant latestAcceptedTime = clock.instant().plus(properties.allowedClockSkew());
		if (time.isAfter(latestAcceptedTime)) {
			throw new InvalidLocationException("time " + time + " is more than "
					+ properties.allowedClockSkew().toSeconds() + " seconds ahead of the server clock");
		}
	}

	private double distanceFromLastLocation(Courier courier, GeoPoint location) {
		GeoPoint lastLocation = courier.lastLocation();
		return lastLocation == null ? 0 : distanceCalculator.distanceInMeters(lastLocation, location);
	}

	private Courier findCourier(String courierId) {
		return courierRepository.findById(courierId).orElseThrow(() -> new CourierNotFoundException(courierId));
	}
}
