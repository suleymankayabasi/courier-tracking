package com.migrosone.couriertracking.listener.entrance;

import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.service.CachedStore;
import java.time.Instant;

public record EntranceCandidate(CourierLocationEvent event, CachedStore store) {
	public String courierId() {
		return event.courierId();
	}

	public CourierPosition current() {
		return event.current();
	}

	public CourierPosition previous() {
		return event.previous();
	}

	public Instant time() {
		return event.current().time();
	}
}
