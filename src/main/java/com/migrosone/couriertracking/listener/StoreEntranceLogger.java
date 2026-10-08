package com.migrosone.couriertracking.listener;

import com.migrosone.couriertracking.model.StoreEntrance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Observer of counted entrances; more reactions (notifications, metrics) can be added as further listeners. */
@Component
public class StoreEntranceLogger {
	private static final Logger log = LoggerFactory.getLogger(StoreEntranceLogger.class);

	@EventListener
	public void onStoreEntrance(StoreEntrance entrance) {
		log.info("Courier {} entered {} at {}", entrance.courierId(), entrance.storeName(), entrance.time());
	}
}
