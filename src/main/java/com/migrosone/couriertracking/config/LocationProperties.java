package com.migrosone.couriertracking.config;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("courier-tracking.location")
public record LocationProperties(Duration allowedClockSkew) {
	public LocationProperties {
		Objects.requireNonNull(allowedClockSkew, "courier-tracking.location.allowed-clock-skew is required");
		if (allowedClockSkew.isNegative()) {
			throw new IllegalArgumentException("courier-tracking.location.allowed-clock-skew must not be negative");
		}
	}
}
