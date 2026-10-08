package com.migrosone.couriertracking.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PropertiesBindingTest {
	@Autowired
	private StoreEntranceProperties storeEntranceProperties;

	@Autowired
	private LocationProperties locationProperties;

	@Test
	void bindsValuesFromApplicationYml() {
		assertThat(storeEntranceProperties).isEqualTo(new StoreEntranceProperties(100, Duration.ofMinutes(1)));
		assertThat(locationProperties).isEqualTo(new LocationProperties(Duration.ofMinutes(1)));
	}
}
