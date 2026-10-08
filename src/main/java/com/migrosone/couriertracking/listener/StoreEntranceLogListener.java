package com.migrosone.couriertracking.listener;

import com.migrosone.couriertracking.event.StoreEntranceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StoreEntranceLogListener {
	private static final Logger log = LoggerFactory.getLogger(StoreEntranceLogListener.class);

	@TransactionalEventListener(fallbackExecution = true)
	public void onStoreEntrance(StoreEntranceEvent event) {
		log.info("Courier {} entered store {} at {}", event.courierId(), event.storeName(), event.entranceTime());
	}
}
