package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.domain.entity.Store;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StoreService {
	private volatile List<CachedStore> stores = List.of();

	public void cache(Collection<Store> persistedStores) {
		stores = persistedStores.stream()
				.map(store -> new CachedStore(store.getId(), store.getName(), store.location()))
				.toList();
	}

	public List<CachedStore> findAll() {
		return stores;
	}
}
