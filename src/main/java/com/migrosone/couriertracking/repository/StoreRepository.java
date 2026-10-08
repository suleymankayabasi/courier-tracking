package com.migrosone.couriertracking.repository;

import com.migrosone.couriertracking.model.Store;
import java.util.List;

public interface StoreRepository {
	List<Store> findAll();
}
