package com.migrosone.couriertracking;

import static com.migrosone.couriertracking.support.GeoTestPoints.ATASEHIR;
import static com.migrosone.couriertracking.support.GeoTestPoints.northOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.migrosone.couriertracking.dto.CourierLocationRequest;
import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.repository.CourierLocationRepository;
import com.migrosone.couriertracking.service.CourierService;
import com.migrosone.couriertracking.service.LocationService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("postgres")
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final int WORKER_THREADS = 16;

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

	@Autowired
	private LocationService locationService;

	@Autowired
	private CourierService courierService;

	@Autowired
	private CourierLocationRepository courierLocationRepository;

	@Test
	void courierJourneyCountsEntrancesAndAccumulatesDistance() {
		String courierId = "pg-" + UUID.randomUUID();
		GeoPoint far = northOf(ATASEHIR, 500);
		GeoPoint near = northOf(ATASEHIR, 50);

		record(courierId, T0, far);
		record(courierId, T0.plusSeconds(30), near);
		record(courierId, T0.plusSeconds(40), far);
		record(courierId, T0.plusSeconds(60), near);
		record(courierId, T0.plusSeconds(75), far);
		record(courierId, T0.plusSeconds(50), near);
		record(courierId, T0.plusSeconds(90), near);

		assertThat(courierService.getTotalTravelDistance(courierId)).isCloseTo(2250.0, within(0.01));
		assertThat(courierService.getStoreEntrances(courierId))
				.extracting(entrance -> entrance.entranceTime())
				.containsExactly(T0.plusSeconds(30), T0.plusSeconds(90));
	}

	@Test
	void identicalConcurrentEventsCountTheEntranceOnce() throws Exception {
		String courierId = "pg-concurrent-" + UUID.randomUUID();
		CourierLocationRequest request = new CourierLocationRequest(
				T0, courierId, northOf(ATASEHIR, 30).latitude(), ATASEHIR.longitude());
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int index = 0; index < 100; index++) {
			tasks.add(() -> {
				try {
					locationService.recordLocation(request);
					return true;
				}
				catch (ConcurrencyFailureException retriesExhausted) {
					return false;
				}
			});
		}

		long succeeded = runConcurrently(tasks).stream().filter(Boolean::booleanValue).count();

		assertThat(succeeded).isPositive();
		assertThat(courierService.getStoreEntrances(courierId)).hasSize(1);
		assertThat(courierService.getTotalTravelDistance(courierId)).isZero();
		assertThat(courierLocationRepository.countByCourierId(courierId)).isEqualTo(succeeded);
	}

	private void record(String courierId, Instant time, GeoPoint point) {
		locationService.recordLocation(new CourierLocationRequest(time, courierId, point.latitude(), point.longitude()));
	}

	private static <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(WORKER_THREADS);
		try {
			CountDownLatch startGate = new CountDownLatch(1);
			List<Future<T>> futures = new ArrayList<>();
			for (Callable<T> task : tasks) {
				futures.add(executor.submit(() -> {
					startGate.await();
					return task.call();
				}));
			}
			startGate.countDown();
			List<T> results = new ArrayList<>();
			for (Future<T> future : futures) {
				results.add(future.get(60, TimeUnit.SECONDS));
			}
			return results;
		}
		finally {
			executor.shutdownNow();
		}
	}
}
