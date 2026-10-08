package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Store;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

/** Migros stores, read once from {@code stores.json} at startup. The application does not start without them. */
@Repository
public class StoreRepository {
	private static final String STORES_FILE = "stores.json";

	private final List<Store> stores;

	public StoreRepository(JsonMapper jsonMapper) {
		try (InputStream input = new ClassPathResource(STORES_FILE).getInputStream()) {
			this.stores = List.of(jsonMapper.readValue(input, Store[].class));
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot read " + STORES_FILE, ex);
		}
		if (stores.isEmpty()) {
			throw new IllegalStateException(STORES_FILE + " contains no stores");
		}
	}

	public List<Store> findAll() {
		return stores;
	}
}
