package com.migrosone.couriertracking.listener.entrance;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
class WithinRadiusRule implements EntranceRule {
	private final StoreGeofence storeGeofence;

	WithinRadiusRule(StoreGeofence storeGeofence) {
		this.storeGeofence = storeGeofence;
	}

	@Override
	public boolean allows(EntranceCandidate candidate) {
		return storeGeofence.contains(candidate.store().location(), candidate.current().point());
	}
}
