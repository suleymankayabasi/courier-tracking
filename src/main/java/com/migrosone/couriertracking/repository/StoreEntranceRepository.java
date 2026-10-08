package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreEntranceRepository extends JpaRepository<StoreEntrance, Long> {
	Optional<StoreEntrance> findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc(String courierId, Long storeId);

	@EntityGraph(attributePaths = "store")
	List<StoreEntrance> findByCourierIdOrderByEntranceTimeAscIdAsc(String courierId);
}
