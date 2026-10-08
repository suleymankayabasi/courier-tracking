package com.migrosone.couriertracking.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("courier-tracking.store-entrance")
public record StoreEntranceProperties(double radiusMeters, Duration reentryThreshold) {
	public StoreEntranceProperties {
		if (radiusMeters <= 0) {
			throw new IllegalArgumentException("courier-tracking.store-entrance.radius-meters must be positive");
		}
		Objects.requireNonNull(reentryThreshold, "courier-tracking.store-entrance.reentry-threshold is required");
		if (reentryThreshold.isNegative()) {
			throw new IllegalArgumentException("courier-tracking.store-entrance.reentry-threshold must not be negative");
		}
	}
}
