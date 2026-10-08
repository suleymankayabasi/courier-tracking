package com.migrosone.couriertracking.listener;

import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.event.StoreEntranceEvent;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class StoreEntranceLogListenerTest {
	@Test
	void logsCourierStoreAndTime(CapturedOutput output) {
		new StoreEntranceLogListener().onStoreEntrance(
				new StoreEntranceEvent("c1", 1, "Ataşehir MMM Migros", Instant.parse("2026-01-01T10:00:00Z")));

		assertThat(output).contains("Courier c1 entered store Ataşehir MMM Migros at 2026-01-01T10:00:00Z");
	}
}
