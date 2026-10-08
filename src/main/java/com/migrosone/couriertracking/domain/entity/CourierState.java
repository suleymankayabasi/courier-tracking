package com.migrosone.couriertracking.domain.entity;

import com.migrosone.couriertracking.domain.CourierId;
import com.migrosone.couriertracking.domain.CourierPosition;
import com.migrosone.couriertracking.geo.GeoPoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "courier_states")
public class CourierState {
	@Id
	@Column(length = CourierId.MAX_LENGTH)
	private String courierId;

	@Column(nullable = false)
	private double lastLat;

	@Column(nullable = false)
	private double lastLng;

	@Column(nullable = false)
	private Instant lastTime;

	@Column(nullable = false)
	private double totalDistanceMeters;

	@Version
	private Long version;

	protected CourierState() {
	}

	private CourierState(String courierId, CourierPosition position) {
		this.courierId = courierId;
		this.totalDistanceMeters = 0.0;
		moveTo(position);
	}

	public static CourierState startingAt(String courierId, CourierPosition position) {
		return new CourierState(courierId, position);
	}

	public boolean isOutOfOrder(Instant eventTime) {
		return eventTime.isBefore(lastTime);
	}

	public CourierPosition lastPosition() {
		return new CourierPosition(new GeoPoint(lastLat, lastLng), lastTime);
	}

	public void travelTo(CourierPosition position, double legMeters) {
		if (!Double.isFinite(legMeters) || legMeters < 0) {
			throw new IllegalArgumentException("legMeters must be finite and not negative but was " + legMeters);
		}
		totalDistanceMeters += legMeters;
		moveTo(position);
	}

	public void stayUntil(Instant time) {
		this.lastTime = time;
	}

	private void moveTo(CourierPosition position) {
		this.lastLat = position.point().latitude();
		this.lastLng = position.point().longitude();
		this.lastTime = position.time();
	}

	public String getCourierId() {
		return courierId;
	}

	public double getTotalDistanceMeters() {
		return totalDistanceMeters;
	}

}
