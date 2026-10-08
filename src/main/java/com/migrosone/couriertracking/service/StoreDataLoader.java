package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.domain.entity.Store;
import com.migrosone.couriertracking.repository.StoreRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

@Component
public class StoreDataLoader implements ApplicationRunner {
	private static final Logger log = LoggerFactory.getLogger(StoreDataLoader.class);

	private final StoreFileReader storeFileReader;
	private final StoreRepository storeRepository;
	private final StoreService storeService;
	private final TransactionOperations transactionOperations;
	private final Resource storesFile;

	public StoreDataLoader(
			StoreFileReader storeFileReader,
			StoreRepository storeRepository,
			StoreService storeService,
			TransactionOperations transactionOperations,
			CourierTrackingProperties properties) {
		this.storeFileReader = storeFileReader;
		this.storeRepository = storeRepository;
		this.storeService = storeService;
		this.transactionOperations = transactionOperations;
		this.storesFile = properties.storesFile();
	}

	@Override
	public void run(ApplicationArguments args) {
		List<Store> fromFile = storeFileReader.read(storesFile);
		List<Store> saved = transactionOperations.execute(status -> fromFile.stream().map(this::upsert).toList());
		storeService.cache(saved);
		log.info("Loaded {} stores from {}", saved.size(), storesFile.getDescription());
	}

	private Store upsert(Store store) {
		return storeRepository.findByName(store.getName())
				.map(existing -> {
					existing.relocateTo(store.location());
					return existing;
				})
				.orElseGet(() -> storeRepository.save(store));
	}
}
