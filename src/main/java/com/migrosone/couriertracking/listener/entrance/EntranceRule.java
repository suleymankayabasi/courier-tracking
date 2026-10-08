package com.migrosone.couriertracking.listener.entrance;

public interface EntranceRule {
	boolean allows(EntranceCandidate candidate);
}
