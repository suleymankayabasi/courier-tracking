package com.migrosone.couriertracking.service;

import com.migrosone.couriertracking.geo.GeoPoint;

public record CachedStore(long id, String name, GeoPoint location) {
}
