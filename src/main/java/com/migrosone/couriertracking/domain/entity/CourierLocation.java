package com.migrosone.couriertracking.domain.entity;

import com.migrosone.couriertracking.domain.CourierId;
import com.migrosone.couriertracking.domain.CourierPosition;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "courier_locations",
		indexes = @Index(name = "idx_courier_location_courier_time", columnList = "courier_id, event_time"))
public class CourierLocation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = CourierId.MAX_LENGTH)
	private String courierId;

	@Column(nullable = false)
	private double lat;

	@Column(nullable = false)
	private double lng;

	@Column(name = "event_time", nullable = false)
	private Instant time;

	protected CourierLocation() {
	}

	public CourierLocation(String courierId, CourierPosition position) {
		this.courierId = courierId;
		this.lat = position.point().latitude();
		this.lng = position.point().longitude();
		this.time = position.time();
	}

	public String getCourierId() {
		return courierId;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public Instant getTime() {
		return time;
	}
}
