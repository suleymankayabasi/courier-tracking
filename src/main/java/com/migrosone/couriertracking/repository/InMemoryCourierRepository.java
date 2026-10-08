package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Courier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCourierRepository implements CourierRepository {
	private final Map<String, Courier> couriers = new ConcurrentHashMap<>();

	@Override
	public Courier findOrCreate(String courierId) {
		return couriers.computeIfAbsent(courierId, Courier::new);
	}

	@Override
	public Optional<Courier> findById(String courierId) {
		return Optional.ofNullable(couriers.get(courierId));
	}
}
