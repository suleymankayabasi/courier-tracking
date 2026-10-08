package com.migrosone.couriertracking.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Courier {
	private final String id;
	private final Map<String, Instant> lastEntranceTimeByStoreName = new HashMap<>();
	private final List<StoreEntrance> entrances = new ArrayList<>();
	private GeoPoint lastLocation;
	private Instant lastLocationTime;
	private double totalDistanceMeters;

	public Courier(String id) {
		this.id = Objects.requireNonNull(id, "id must not be null");
	}

	public String id() {
		return id;
	}

	public synchronized boolean isOutOfOrder(Instant time) {
		return lastLocationTime != null && time.isBefore(lastLocationTime);
	}

	public synchronized GeoPoint lastLocation() {
		return lastLocation;
	}

	public synchronized void moveTo(GeoPoint location, Instant time, double travelledMeters) {
		lastLocation = location;
		lastLocationTime = time;
		totalDistanceMeters += travelledMeters;
	}

	public synchronized Instant lastEntranceTimeTo(Store store) {
		return lastEntranceTimeByStoreName.get(store.name());
	}

	public synchronized StoreEntrance enter(Store store, Instant time) {
		StoreEntrance entrance = new StoreEntrance(id, store.name(), time);
		lastEntranceTimeByStoreName.put(store.name(), time);
		entrances.add(entrance);
		return entrance;
	}

	public synchronized double totalDistanceMeters() {
		return totalDistanceMeters;
	}

	public synchronized List<StoreEntrance> entrances() {
		return List.copyOf(entrances);
	}
}
