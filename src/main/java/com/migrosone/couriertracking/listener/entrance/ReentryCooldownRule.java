package com.migrosone.couriertracking.listener.entrance;

import com.migrosone.couriertracking.config.CourierTrackingProperties;
import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
class ReentryCooldownRule implements EntranceRule {
	private static final Logger log = LoggerFactory.getLogger(ReentryCooldownRule.class);

	private final StoreEntranceRepository storeEntranceRepository;
	private final Duration reentryCooldown;

	ReentryCooldownRule(StoreEntranceRepository storeEntranceRepository, CourierTrackingProperties properties) {
		this.storeEntranceRepository = storeEntranceRepository;
		this.reentryCooldown = properties.entrance().reentryCooldown();
	}

	@Override
	public boolean allows(EntranceCandidate candidate) {
		return storeEntranceRepository
				.findFirstByCourierIdAndStoreIdOrderByEntranceTimeDesc(candidate.courierId(), candidate.store().id())
				.map(StoreEntrance::getEntranceTime)
				.map(lastEntrance -> cooldownElapsed(candidate, lastEntrance))
				.orElse(true);
	}

	private boolean cooldownElapsed(EntranceCandidate candidate, Instant lastEntrance) {
		boolean elapsed = !candidate.time().isBefore(lastEntrance.plus(reentryCooldown));
		if (!elapsed) {
			log.debug("Re-entry of courier {} to store {} at {} not counted: last entrance was at {}, cooldown is {}",
					candidate.courierId(), candidate.store().name(), candidate.time(), lastEntrance, reentryCooldown);
		}
		return elapsed;
	}
}
