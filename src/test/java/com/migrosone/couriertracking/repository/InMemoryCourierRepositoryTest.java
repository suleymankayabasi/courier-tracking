package com.migrosone.couriertracking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.migrosone.couriertracking.model.Courier;
import org.junit.jupiter.api.Test;

class InMemoryCourierRepositoryTest {
	private final CourierRepository repository = new InMemoryCourierRepository();

	@Test
	void findsNothingForUnknownCourier() {
		assertThat(repository.findById("unknown")).isEmpty();
	}

	@Test
	void createsCourierOnFirstUpdateAndReturnsTheUpdateResult() {
		String updatedId = repository.updateAtomically("courier-1", Courier::id);

		assertThat(updatedId).isEqualTo("courier-1");
		assertThat(repository.findById("courier-1")).map(Courier::id).contains("courier-1");
	}

	@Test
	void updatesTheSameCourierEveryTime() {
		Courier first = repository.updateAtomically("courier-1", courier -> courier);
		Courier second = repository.updateAtomically("courier-1", courier -> courier);

		assertThat(second).isSameAs(first);
	}

	@Test
	void doesNotKeepCourierWhenTheFirstUpdateFails() {
		assertThatThrownBy(() -> repository.updateAtomically("courier-1", courier -> {
			throw new IllegalStateException("update failed");
		})).isInstanceOf(IllegalStateException.class);

		assertThat(repository.findById("courier-1")).isEmpty();
	}
}
