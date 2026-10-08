package com.migrosone.couriertracking.domain.entity;

import com.migrosone.couriertracking.domain.CourierId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "store_entrances",
		indexes = @Index(name = "idx_store_entrance_courier_store_time",
				columnList = "courier_id, store_id, entrance_time"))
public class StoreEntrance {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = CourierId.MAX_LENGTH)
	private String courierId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@Column(nullable = false)
	private Instant entranceTime;

	protected StoreEntrance() {
	}

	public StoreEntrance(String courierId, Store store, Instant entranceTime) {
		this.courierId = courierId;
		this.store = store;
		this.entranceTime = entranceTime;
	}

	public String getCourierId() {
		return courierId;
	}

	public Store getStore() {
		return store;
	}

	public Instant getEntranceTime() {
		return entranceTime;
	}
}
