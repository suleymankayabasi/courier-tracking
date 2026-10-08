package com.migrosone.couriertracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.geo.DistanceAlgorithm;
import com.migrosone.couriertracking.geo.DistanceCalculator;
import com.migrosone.couriertracking.geo.EquirectangularDistanceCalculator;
import com.migrosone.couriertracking.geo.HaversineDistanceCalculator;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

class CourierTrackingPropertiesTest {
	private static final String[] VALID = {
			"courier-tracking.stores-file=classpath:stores.json",
			"courier-tracking.entrance.radius-meters=100",
			"courier-tracking.entrance.reentry-cooldown=60s",
			"courier-tracking.distance.algorithm=haversine",
			"courier-tracking.distance.min-movement-meters=0",
			"courier-tracking.location.max-future-skew=5m",
			"courier-tracking.concurrency.max-retries=5",
			"courier-tracking.concurrency.retry-delay=10ms" };

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
			.withUserConfiguration(PropertiesConfiguration.class, DistanceCalculatorConfig.class)
			.withPropertyValues(VALID);

	@Test
	void bindsTheConfiguredValues() {
		runner.run(context -> {
			CourierTrackingProperties properties = context.getBean(CourierTrackingProperties.class);
			assertThat(properties.storesFile().exists()).isTrue();
			assertThat(properties.entrance().radiusMeters()).isEqualTo(100.0);
			assertThat(properties.entrance().reentryCooldown()).isEqualTo(Duration.ofMinutes(1));
			assertThat(properties.distance().algorithm()).isEqualTo(DistanceAlgorithm.HAVERSINE);
			assertThat(properties.location().maxFutureSkew()).isEqualTo(Duration.ofMinutes(5));
			assertThat(properties.concurrency().retryDelay()).isEqualTo(Duration.ofMillis(10));
			assertThat(context.getBean(DistanceCalculator.class)).isInstanceOf(HaversineDistanceCalculator.class);
		});
	}

	@Test
	void distanceAlgorithmIsSelectedFromConfiguration() {
		runner.withPropertyValues("courier-tracking.distance.algorithm=equirectangular")
				.run(context -> assertThat(context.getBean(DistanceCalculator.class))
						.isInstanceOf(EquirectangularDistanceCalculator.class));
	}

	@Test
	void nonPositiveRadiusStopsTheStartup() {
		runner.withPropertyValues("courier-tracking.entrance.radius-meters=0")
				.run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("radiusMeters"));
	}

	@Test
	void negativeCooldownStopsTheStartup() {
		runner.withPropertyValues("courier-tracking.entrance.reentry-cooldown=-1s")
				.run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("reentryCooldown"));
	}

	@Test
	void unknownAlgorithmStopsTheStartup() {
		runner.withPropertyValues("courier-tracking.distance.algorithm=vincenty")
				.run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("DistanceAlgorithm.vincenty"));
	}

	@Test
	void missingSectionStopsTheStartup() {
		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
				.withUserConfiguration(PropertiesConfiguration.class)
				.withPropertyValues("courier-tracking.stores-file=classpath:stores.json")
				.run(context -> assertThat(context).getFailure().rootCause().hasMessageContaining("entrance"));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(CourierTrackingProperties.class)
	static class PropertiesConfiguration {
	}
}
