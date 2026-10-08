package com.migrosone.couriertracking.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.migrosone.couriertracking.dto.CourierLocationRequest;
import com.migrosone.couriertracking.geo.DistanceCalculator;
import com.migrosone.couriertracking.repository.CourierLocationRepository;
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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.ConcurrencyFailureException;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class LocationConcurrencyIntegrationTest {
	private static final double LEG_METERS = 1.0;
	private static final int WORKER_THREADS = 16;
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

	@Autowired
	private LocationService locationService;

	@Autowired
	private CourierService courierService;

	@Autowired
	private StoreService storeService;

	@Autowired
	private CourierLocationRepository courierLocationRepository;

	@TestConfiguration(proxyBeanMethods = false)
	static class ConstantDistance {
		@Bean
		@Primary
		DistanceCalculator constantDistanceCalculator() {
			return (from, to) -> LEG_METERS;
		}
	}

	@Test
	void concurrentLocationsOfOneCourierLoseNoDistanceAndEnterEachStoreOnce(CapturedOutput output) throws Exception {
		String courierId = "concurrent-" + UUID.randomUUID();
		int events = 200;
		List<Callable<Boolean>> tasks = new ArrayList<>();
		for (int index = 0; index < events; index++) {
			CourierLocationRequest request = new CourierLocationRequest(T0, courierId, 41.0, 29.0 + index * 1e-4);
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
		assertThat(courierService.getTotalTravelDistance(courierId)).isEqualTo((succeeded - 1) * LEG_METERS);
		assertThat(courierService.getStoreEntrances(courierId))
				.extracting(entrance -> entrance.storeId())
				.doesNotHaveDuplicates()
				.hasSize(storeService.findAll().size());
		assertThat(courierLocationRepository.countByCourierId(courierId)).isEqualTo(succeeded);
		assertThat(output.getOut().lines().filter(line -> line.contains("Courier " + courierId + " entered store")))
				.hasSize(storeService.findAll().size());
	}

	@Test
	void couriersProcessedInParallelAccumulateOnlyTheirOwnDistance() throws Exception {
		int couriers = 50;
		int eventsPerCourier = 10;
		String prefix = "parallel-" + UUID.randomUUID() + "-";
		List<Callable<Void>> tasks = new ArrayList<>();
		for (int courier = 0; courier < couriers; courier++) {
			String courierId = prefix + courier;
			tasks.add(() -> {
				for (int index = 0; index < eventsPerCourier; index++) {
					locationService.recordLocation(
							new CourierLocationRequest(T0.plusSeconds(index), courierId, 41.0, 29.0 + index * 1e-4));
				}
				return null;
			});
		}

		runConcurrently(tasks);

		for (int courier = 0; courier < couriers; courier++) {
			assertThat(courierService.getTotalTravelDistance(prefix + courier))
					.isEqualTo((eventsPerCourier - 1) * LEG_METERS);
		}
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
				results.add(future.get(30, TimeUnit.SECONDS));
			}
			return results;
		}
		finally {
			executor.shutdownNow();
		}
	}
}
