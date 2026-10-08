package com.migrosone.couriertracking.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracking state of one courier. All methods are synchronized on the instance, so a caller can make a whole location
 * update atomic with {@code synchronized (courier) { ... }}.
 */
public class Courier {
	private final String id;
	private GeoPoint lastLocation;
	private Instant lastTime;
	private double totalDistanceMeters;
	private final Map<String, Instant> lastEntranceByStore = new HashMap<>();
	private final List<StoreEntrance> entrances = new ArrayList<>();

	public Courier(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	/** True if {@code time} is before the last accepted location of this courier. */
	public synchronized boolean isOutOfOrder(Instant time) {
		return lastTime != null && time.isBefore(lastTime);
	}

	/** Last accepted location, or {@code null} before the first one. */
	public synchronized GeoPoint lastLocation() {
		return lastLocation;
	}

	public synchronized void moveTo(GeoPoint location, Instant time, double legMeters) {
		this.lastLocation = location;
		this.lastTime = time;
		this.totalDistanceMeters += legMeters;
	}

	/** Time of the last counted entrance to the store, or {@code null} if there is none. */
	public synchronized Instant lastEntranceTo(Store store) {
		return lastEntranceByStore.get(store.name());
	}

	public synchronized StoreEntrance enter(Store store, Instant time) {
		StoreEntrance entrance = new StoreEntrance(id, store.name(), time);
		lastEntranceByStore.put(store.name(), time);
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
