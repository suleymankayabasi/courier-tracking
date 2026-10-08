package com.migrosone.couriertracking.event;

import java.time.Instant;

public record StoreEntranceEvent(String courierId, long storeId, String storeName, Instant entranceTime) {
}
