package com.migrosone.couriertracking.dto;

import java.time.Instant;

public record StoreEntranceResponse(long storeId, String storeName, Instant entranceTime) {
}
