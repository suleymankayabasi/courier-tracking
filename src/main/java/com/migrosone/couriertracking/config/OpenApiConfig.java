package com.migrosone.couriertracking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
	@Bean
	OpenAPI courierTrackingOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Courier Tracking API")
				.version("v1")
				.description("Ingests courier geolocations, detects Migros store entrances and reports the total "
						+ "distance travelled by each courier."));
	}
}
