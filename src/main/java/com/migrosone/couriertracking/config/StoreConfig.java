package com.migrosone.couriertracking.config;

import com.migrosone.couriertracking.json.JsonStoreReader;
import com.migrosone.couriertracking.repository.InMemoryStoreRepository;
import com.migrosone.couriertracking.repository.StoreRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
public class StoreConfig {
	@Bean
	public StoreRepository storeRepository(
			JsonStoreReader jsonStoreReader, @Value("classpath:stores.json") Resource storesFile) {
		return new InMemoryStoreRepository(jsonStoreReader.read(storesFile));
	}
}
