package com.migrosone.couriertracking.repository;

import static com.migrosone.couriertracking.support.TestFixtures.ATASEHIR;
import static com.migrosone.couriertracking.support.TestFixtures.NOVADA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.Store;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryStoreRepositoryTest {
	@Test
	void returnsTheGivenStores() {
		assertThat(new InMemoryStoreRepository(List.of(ATASEHIR, NOVADA)).findAll()).containsExactly(ATASEHIR, NOVADA);
	}

	@Test
	void isNotAffectedByChangesToTheGivenList() {
		List<Store> stores = new ArrayList<>(List.of(ATASEHIR));
		StoreRepository repository = new InMemoryStoreRepository(stores);

		stores.add(NOVADA);

		assertThat(repository.findAll()).containsExactly(ATASEHIR);
	}

	@Test
	void rejectsEmptyStoreList() {
		assertThatThrownBy(() -> new InMemoryStoreRepository(List.of()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("At least one store is required");
	}

	@Test
	void rejectsDuplicateStoreNames() {
		Store sameNameElsewhere = new Store(ATASEHIR.name(), new GeoPoint(41, 29));

		assertThatThrownBy(() -> new InMemoryStoreRepository(List.of(ATASEHIR, sameNameElsewhere)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Duplicate store name: " + ATASEHIR.name());
	}
}
