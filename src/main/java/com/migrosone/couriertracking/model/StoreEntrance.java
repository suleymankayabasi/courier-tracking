package com.migrosone.couriertracking.model;

import java.time.Instant;

public record StoreEntrance(String courierId, String storeName, Instant time) {
}
