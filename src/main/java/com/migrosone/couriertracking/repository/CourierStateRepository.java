package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.domain.entity.CourierState;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface CourierStateRepository extends JpaRepository<CourierState, String> {
	@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
	Optional<CourierState> findForUpdateByCourierId(String courierId);
}
