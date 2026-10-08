package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Courier;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCourierRepository implements CourierRepository {
	private final Map<String, Courier> couriers = new ConcurrentHashMap<>();

	@Override
	public <T> T updateAtomically(String courierId, Function<Courier, T> update) {
		AtomicReference<T> result = new AtomicReference<>();
		couriers.compute(courierId, (id, existingCourier) -> {
			Courier courier = existingCourier != null ? existingCourier : new Courier(id);
			result.set(update.apply(courier));
			return courier;
		});
		return result.get();
	}

	@Override
	public Optional<Courier> findById(String courierId) {
		return Optional.ofNullable(couriers.get(courierId));
	}
}
