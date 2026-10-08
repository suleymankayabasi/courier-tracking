package com.migrosone.couriertracking.support;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.config.CourierTrackingProperties.Concurrency;
import com.migrosone.couriertracking.config.CourierTrackingProperties.Distance;
import com.migrosone.couriertracking.config.CourierTrackingProperties.Entrance;
import com.migrosone.couriertracking.geo.DistanceAlgorithm;
import com.migrosone.couriertracking.config.CourierTrackingProperties.Location;
import java.time.Duration;
import org.springframework.core.io.ClassPathResource;

public final class TestProperties {
	public static final double RADIUS_METERS = 100.0;
	public static final Duration REENTRY_COOLDOWN = Duration.ofSeconds(60);
	public static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);

	private TestProperties() {
	}

	public static CourierTrackingProperties defaults() {
		return withEntrance(RADIUS_METERS, REENTRY_COOLDOWN);
	}

	public static CourierTrackingProperties withEntrance(double radiusMeters, Duration reentryCooldown) {
		return of(new Entrance(radiusMeters, reentryCooldown), 0.0, 3);
	}

	public static CourierTrackingProperties withMinMovement(double minMovementMeters) {
		return of(new Entrance(RADIUS_METERS, REENTRY_COOLDOWN), minMovementMeters, 3);
	}

	public static CourierTrackingProperties withMaxRetries(int maxRetries) {
		return of(new Entrance(RADIUS_METERS, REENTRY_COOLDOWN), 0.0, maxRetries);
	}

	private static CourierTrackingProperties of(Entrance entrance, double minMovementMeters, int maxRetries) {
		return new CourierTrackingProperties(
				new ClassPathResource("stores.json"),
				entrance,
				new Distance(DistanceAlgorithm.HAVERSINE, minMovementMeters),
				new Concurrency(maxRetries, Duration.ZERO),
				new Location(MAX_FUTURE_SKEW));
	}
}
