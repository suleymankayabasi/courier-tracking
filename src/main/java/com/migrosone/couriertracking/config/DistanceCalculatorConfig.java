package com.migrosone.couriertracking.config;

import com.migrosone.couriertracking.geo.DistanceCalculator;
import com.migrosone.couriertracking.geo.EquirectangularDistanceCalculator;
import com.migrosone.couriertracking.geo.HaversineDistanceCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DistanceCalculatorConfig {
	@Bean
	DistanceCalculator distanceCalculator(CourierTrackingProperties properties) {
		return switch (properties.distance().algorithm()) {
			case HAVERSINE -> new HaversineDistanceCalculator();
			case EQUIRECTANGULAR -> new EquirectangularDistanceCalculator();
		};
	}
}
