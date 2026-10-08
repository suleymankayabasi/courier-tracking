package com.migrosone.couriertracking.event;

import com.migrosone.couriertracking.domain.CourierPosition;
import java.util.Objects;

public record CourierLocationEvent(String courierId, CourierPosition current, CourierPosition previous) {
	public CourierLocationEvent {
		Objects.requireNonNull(courierId, "courierId must not be null");
		Objects.requireNonNull(current, "current must not be null");
	}

	public boolean isFirstLocation() {
		return previous == null;
	}
}
