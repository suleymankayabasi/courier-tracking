package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Courier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** In-memory courier store; data is lost on restart. */
@Repository
public class CourierRepository {
	private final Map<String, Courier> couriers = new ConcurrentHashMap<>();

	public Courier findOrCreate(String courierId) {
		return couriers.computeIfAbsent(courierId, Courier::new);
	}

	public Optional<Courier> findById(String courierId) {
		return Optional.ofNullable(couriers.get(courierId));
	}
}
