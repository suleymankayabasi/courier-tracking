package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Courier;
import java.util.Optional;

public interface CourierRepository {
	Courier findOrCreate(String courierId);

	Optional<Courier> findById(String courierId);
}
