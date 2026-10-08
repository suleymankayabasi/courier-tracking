package com.migrosone.couriertracking.listener;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.geo.DistanceCalculator;
import com.migrosone.couriertracking.repository.CourierStateRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TravelDistanceListener {
	private final CourierStateRepository courierStateRepository;
	private final DistanceCalculator distanceCalculator;
	private final double minMovementMeters;

	public TravelDistanceListener(
			CourierStateRepository courierStateRepository,
			DistanceCalculator distanceCalculator,
			CourierTrackingProperties properties) {
		this.courierStateRepository = courierStateRepository;
		this.distanceCalculator = distanceCalculator;
		this.minMovementMeters = properties.distance().minMovementMeters();
	}

	@EventListener
	public void onCourierLocation(CourierLocationEvent event) {
		CourierPosition current = event.current();
		if (event.isFirstLocation()) {
			courierStateRepository.save(CourierState.startingAt(event.courierId(), current));
			return;
		}

		CourierState state = courierStateRepository.findById(event.courierId())
				.orElseThrow(() -> new IllegalStateException("No state for courier " + event.courierId()));
		double legMeters = distanceCalculator.distanceInMeters(event.previous().point(), current.point());
		if (legMeters < minMovementMeters) {
			state.stayUntil(current.time());
		}
		else {
			state.travelTo(current, legMeters);
		}
	}
}
