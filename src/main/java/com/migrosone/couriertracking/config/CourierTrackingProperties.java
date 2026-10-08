package com.migrosone.couriertracking.config;

import com.migrosone.couriertracking.geo.DistanceAlgorithm;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("courier-tracking")
public record CourierTrackingProperties(
		@NotNull Resource storesFile,
		@Valid @NotNull Entrance entrance,
		@Valid @NotNull Distance distance,
		@Valid @NotNull Concurrency concurrency,
		@Valid @NotNull Location location) {
	public record Entrance(
			@Positive double radiusMeters,
			@NotNull @DurationMin(nanos = 0) Duration reentryCooldown) {
	}

	public record Distance(
			@NotNull DistanceAlgorithm algorithm,
			@PositiveOrZero double minMovementMeters) {
	}

	public record Location(@NotNull @DurationMin(nanos = 0) Duration maxFutureSkew) {
	}

	public record Concurrency(
			@PositiveOrZero int maxRetries,
			@NotNull @DurationMin(nanos = 0) Duration retryDelay) {
	}
}
