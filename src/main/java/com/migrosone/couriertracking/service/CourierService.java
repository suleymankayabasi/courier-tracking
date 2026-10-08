package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.domain.entity.Store;
import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.dto.StoreEntranceResponse;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.repository.CourierStateRepository;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourierService {
	private final CourierStateRepository courierStateRepository;
	private final StoreEntranceRepository storeEntranceRepository;

	public CourierService(CourierStateRepository courierStateRepository, StoreEntranceRepository storeEntranceRepository) {
		this.courierStateRepository = courierStateRepository;
		this.storeEntranceRepository = storeEntranceRepository;
	}

	public Double getTotalTravelDistance(String courierId) {
		return courierStateRepository.findById(courierId)
				.map(CourierState::getTotalDistanceMeters)
				.orElseThrow(() -> new CourierNotFoundException(courierId));
	}

	public List<StoreEntranceResponse> getStoreEntrances(String courierId) {
		if (!courierStateRepository.existsById(courierId)) {
			throw new CourierNotFoundException(courierId);
		}
		return storeEntranceRepository.findByCourierIdOrderByEntranceTimeAscIdAsc(courierId).stream()
				.map(this::toResponse)
				.toList();
	}

	private StoreEntranceResponse toResponse(StoreEntrance entrance) {
		Store store = entrance.getStore();
		return new StoreEntranceResponse(store.getId(), store.getName(), entrance.getEntranceTime());
	}
}
