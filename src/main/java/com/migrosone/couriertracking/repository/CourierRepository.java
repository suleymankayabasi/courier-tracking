package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Courier;
import java.util.Optional;
import java.util.function.Function;

public interface CourierRepository {
	<T> T updateAtomically(String courierId, Function<Courier, T> update);

	Optional<Courier> findById(String courierId);
}
