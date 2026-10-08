package com.migrosone.couriertracking.support;

import com.migrosone.couriertracking.domain.entity.Store;
import com.migrosone.couriertracking.geo.GeoPoint;
import org.springframework.test.util.ReflectionTestUtils;

public final class TestStores {
	private TestStores() {
	}

	public static Store persisted(long id, String name, GeoPoint location) {
		Store store = new Store(name, location);
		ReflectionTestUtils.setField(store, "id", id);
		return store;
	}
}
