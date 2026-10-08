package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Store;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InMemoryStoreRepository implements StoreRepository {
	private final List<Store> stores;

	public InMemoryStoreRepository(List<Store> stores) {
		if (stores.isEmpty()) {
			throw new IllegalStateException("At least one store is required");
		}
		requireUniqueNames(stores);
		this.stores = List.copyOf(stores);
	}

	@Override
	public List<Store> findAll() {
		return stores;
	}

	private static void requireUniqueNames(List<Store> stores) {
		Set<String> seenNames = new HashSet<>();
		for (Store store : stores) {
			if (!seenNames.add(store.name())) {
				throw new IllegalStateException("Duplicate store name: " + store.name());
			}
		}
	}
}
