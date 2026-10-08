package com.migrosone.couriertracking.domain;

import com.migrosone.couriertracking.geo.GeoPoint;
import java.time.Instant;
import java.util.Objects;

public record CourierPosition(GeoPoint point, Instant time) {
	public CourierPosition {
		Objects.requireNonNull(point, "point must not be null");
		Objects.requireNonNull(time, "time must not be null");
	}
}
