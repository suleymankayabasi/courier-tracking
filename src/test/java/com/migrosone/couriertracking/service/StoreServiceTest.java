package com.migrosone.couriertracking.service;

import static com.migrosone.couriertracking.support.TestStores.persisted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.migrosone.couriertracking.geo.GeoPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class StoreServiceTest {
	private final StoreService service = new StoreService();

	@Test
	void isEmptyUntilStoresAreCached() {
		assertThat(service.findAll()).isEmpty();
	}

	@Test
	void cachesDetachedCopiesOfThePersistedStoresInOrder() {
		service.cache(List.of(
				persisted(1L, "Store A", new GeoPoint(41.0, 29.0)), persisted(2L, "Store B", new GeoPoint(41.1, 29.1))));

		assertThat(service.findAll()).containsExactly(
				new CachedStore(1, "Store A", new GeoPoint(41.0, 29.0)),
				new CachedStore(2, "Store B", new GeoPoint(41.1, 29.1)));
	}

	@Test
	void cachedStoresCannotBeModifiedByCallers() {
		service.cache(List.of(persisted(1L, "Store A", new GeoPoint(41.0, 29.0))));

		assertThatThrownBy(() -> service.findAll().clear()).isInstanceOf(UnsupportedOperationException.class);
	}
}
