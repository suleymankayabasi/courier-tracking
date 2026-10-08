package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Store;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JsonFileStoreRepository implements StoreRepository {
	private final List<Store> stores;

	public JsonFileStoreRepository(JsonMapper jsonMapper, @Value("classpath:stores.json") Resource storesFile) {
		Store[] fromFile = read(jsonMapper, storesFile);
		if (fromFile == null || fromFile.length == 0) {
			throw new IllegalStateException(storesFile.getDescription() + " contains no stores");
		}
		this.stores = List.of(fromFile);
		requireUniqueNames(stores);
	}

	@Override
	public List<Store> findAll() {
		return stores;
	}

	private static Store[] read(JsonMapper jsonMapper, Resource storesFile) {
		try (InputStream input = storesFile.getInputStream()) {
			return jsonMapper.readValue(input, Store[].class);
		}
		catch (IOException | JacksonException ex) {
			throw new IllegalStateException("Cannot read stores from " + storesFile.getDescription(), ex);
		}
	}

	private static void requireUniqueNames(List<Store> stores) {
		Set<String> names = new HashSet<>();
		for (Store store : stores) {
			if (!names.add(store.name())) {
				throw new IllegalStateException("Duplicate store name: " + store.name());
			}
		}
	}
}
