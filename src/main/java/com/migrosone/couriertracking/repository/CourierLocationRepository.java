package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.domain.entity.CourierLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierLocationRepository extends JpaRepository<CourierLocation, Long> {
	long countByCourierId(String courierId);
}
