package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.domain.entity.Store;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
	Optional<Store> findByName(String name);
}
