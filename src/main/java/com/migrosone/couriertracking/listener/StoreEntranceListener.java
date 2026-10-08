package com.migrosone.couriertracking.listener;

import com.migrosone.couriertracking.domain.entity.StoreEntrance;
import com.migrosone.couriertracking.event.CourierLocationEvent;
import com.migrosone.couriertracking.event.StoreEntranceEvent;
import com.migrosone.couriertracking.listener.entrance.EntranceCandidate;
import com.migrosone.couriertracking.listener.entrance.EntranceRule;
import com.migrosone.couriertracking.repository.StoreEntranceRepository;
import com.migrosone.couriertracking.repository.StoreRepository;
import com.migrosone.couriertracking.service.CachedStore;
import com.migrosone.couriertracking.service.StoreService;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StoreEntranceListener {
	private final StoreService storeService;
	private final List<EntranceRule> rules;
	private final StoreEntranceRepository storeEntranceRepository;
	private final StoreRepository storeRepository;
	private final ApplicationEventPublisher eventPublisher;

	public StoreEntranceListener(
			StoreService storeService,
			List<EntranceRule> rules,
			StoreEntranceRepository storeEntranceRepository,
			StoreRepository storeRepository,
			ApplicationEventPublisher eventPublisher) {
		this.storeService = storeService;
		this.rules = List.copyOf(rules);
		this.storeEntranceRepository = storeEntranceRepository;
		this.storeRepository = storeRepository;
		this.eventPublisher = eventPublisher;
	}

	@EventListener
	public void onCourierLocation(CourierLocationEvent event) {
		for (CachedStore store : storeService.findAll()) {
			EntranceCandidate candidate = new EntranceCandidate(event, store);
			if (passesChain(candidate)) {
				recordEntrance(candidate);
			}
		}
	}

	private boolean passesChain(EntranceCandidate candidate) {
		for (EntranceRule rule : rules) {
			if (!rule.allows(candidate)) {
				return false;
			}
		}
		return true;
	}

	private void recordEntrance(EntranceCandidate candidate) {
		CachedStore store = candidate.store();

		storeEntranceRepository.save(new StoreEntrance(
				candidate.courierId(), storeRepository.getReferenceById(store.id()), candidate.time()));
		eventPublisher.publishEvent(
				new StoreEntranceEvent(candidate.courierId(), store.id(), store.name(), candidate.time()));
	}
}
