package com.migrosone.couriertracking.domain.entity;

import com.migrosone.couriertracking.geo.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "stores")
public class Store {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String name;

	@Column(nullable = false)
	private double lat;

	@Column(nullable = false)
	private double lng;

	protected Store() {
	}

	public Store(String name, GeoPoint location) {
		this.name = Objects.requireNonNull(name, "name must not be null");
		relocateTo(location);
	}

	public void relocateTo(GeoPoint location) {
		this.lat = location.latitude();
		this.lng = location.longitude();
	}

	public GeoPoint location() {
		return new GeoPoint(lat, lng);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
