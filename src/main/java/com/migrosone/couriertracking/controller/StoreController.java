package com.migrosone.couriertracking.controller;

import com.migrosone.couriertracking.dto.StoreResponse;
import com.migrosone.couriertracking.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stores")
@Tag(name = "Stores")
public class StoreController {
	private final StoreService storeService;

	public StoreController(StoreService storeService) {
		this.storeService = storeService;
	}

	@GetMapping
	@Operation(summary = "All Migros stores loaded from stores.json")
	public List<StoreResponse> getStores() {
		return storeService.findAll().stream()
				.map(store -> new StoreResponse(
						store.id(), store.name(), store.location().latitude(), store.location().longitude()))
				.toList();
	}
}
