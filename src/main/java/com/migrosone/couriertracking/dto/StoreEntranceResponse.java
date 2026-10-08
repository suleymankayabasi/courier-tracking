package com.migrosone.couriertracking.dto;

import com.migrosone.couriertracking.model.StoreEntrance;
import java.time.Instant;

public record StoreEntranceResponse(String storeName, Instant time) {
	public static StoreEntranceResponse from(StoreEntrance entrance) {
		return new StoreEntranceResponse(entrance.storeName(), entrance.time());
	}
}
