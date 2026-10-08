package com.migrosone.couriertracking.listener.entrance;

import com.migrosone.couriertracking.domain.CourierPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
class OutsideToInsideTransitionRule implements EntranceRule {
	private static final Logger log = LoggerFactory.getLogger(OutsideToInsideTransitionRule.class);

	private final StoreGeofence storeGeofence;

	OutsideToInsideTransitionRule(StoreGeofence storeGeofence) {
		this.storeGeofence = storeGeofence;
	}

	@Override
	public boolean allows(EntranceCandidate candidate) {
		CourierPosition previous = candidate.previous();
		boolean wasInside = previous != null && storeGeofence.contains(candidate.store().location(), previous.point());
		if (wasInside) {
			log.trace("Courier {} is still inside store {}", candidate.courierId(), candidate.store().name());
		}
		return !wasInside;
	}
}
