package com.migrosone.couriertracking;

import static com.migrosone.couriertracking.support.GeoTestPoints.ATASEHIR;
import static com.migrosone.couriertracking.support.GeoTestPoints.NOVADA;
import static com.migrosone.couriertracking.support.GeoTestPoints.northOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.repository.CourierLocationRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CourierTrackingIntegrationTest {
	private static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");
	private static final String ATASEHIR_NAME = "Ataşehir MMM Migros";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CourierLocationRepository courierLocationRepository;

	@Test
	void storesAreLoadedFromStoresJsonAtStartup() throws Exception {
		mockMvc.perform(get("/api/v1/stores"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[0].name").value(ATASEHIR_NAME))
				.andExpect(jsonPath("$[0].lat").value(ATASEHIR.latitude()))
				.andExpect(jsonPath("$[0].lng").value(ATASEHIR.longitude()));
	}

	@Test
	void courierJourneyCountsEntrancesAndAccumulatesDistance(CapturedOutput output) throws Exception {
		String courierId = newCourierId();
		GeoPoint far = northOf(ATASEHIR, 500);
		GeoPoint near = northOf(ATASEHIR, 50);

		postLocation(courierId, T0, far);
		postLocation(courierId, T0.plusSeconds(30), near);
		postLocation(courierId, T0.plusSeconds(35), near);
		postLocation(courierId, T0.plusSeconds(40), far);
		postLocation(courierId, T0.plusSeconds(60), near);
		postLocation(courierId, T0.plusSeconds(75), far);
		postLocation(courierId, T0.plusSeconds(50), far);
		postLocation(courierId, T0.plusSeconds(90), near);
		postLocation(courierId, T0.plusSeconds(200), near);

		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courierId").value(courierId))
				.andExpect(jsonPath("$.totalDistanceMeters").value(closeTo(2250.0, 0.01)));
		mockMvc.perform(get("/api/v1/couriers/{courierId}/store-entrances", courierId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].storeName").value(ATASEHIR_NAME))
				.andExpect(jsonPath("$[0].entranceTime").value("2026-01-01T10:00:30Z"))
				.andExpect(jsonPath("$[1].entranceTime").value("2026-01-01T10:01:30Z"));

		assertThat(output.getOut().lines()
				.filter(line -> line.contains("Courier " + courierId + " entered store " + ATASEHIR_NAME))
				.count()).isEqualTo(2);
		assertThat(output.getOut()).contains("Ignoring out-of-order location of courier " + courierId);
		assertThat(courierLocationRepository.countByCourierId(courierId)).isEqualTo(9);
	}

	@Test
	void entrancesToDifferentStoresAreListedInTimeOrder() throws Exception {
		String courierId = newCourierId();

		postLocation(courierId, T0, northOf(ATASEHIR, 20));
		postLocation(courierId, T0.plusSeconds(120), NOVADA);

		mockMvc.perform(get("/api/v1/couriers/{courierId}/store-entrances", courierId))
				.andExpect(jsonPath("$[*].storeName").value(contains(ATASEHIR_NAME, "Novada MMM Migros")));
	}

	@Test
	void singleMoveFromAtasehirToNovadaIsAboutOneKilometerAndTwoEntrances() throws Exception {
		String courierId = newCourierId();

		postLocation(courierId, T0, ATASEHIR);
		postLocation(courierId, T0.plusSeconds(120), NOVADA);

		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalDistanceMeters").value(closeTo(982.0, 982.0 * 0.01)));
		mockMvc.perform(get("/api/v1/couriers/{courierId}/store-entrances", courierId))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].storeName").value(ATASEHIR_NAME))
				.andExpect(jsonPath("$[0].entranceTime").value("2026-01-01T10:00:00Z"))
				.andExpect(jsonPath("$[1].storeName").value("Novada MMM Migros"))
				.andExpect(jsonPath("$[1].entranceTime").value("2026-01-01T10:02:00Z"));
	}

	@Test
	void batchIsProcessedInEventTimeOrder() throws Exception {
		String courierId = newCourierId();
		GeoPoint start = northOf(ATASEHIR, 300);

		mockMvc.perform(post("/api/v1/couriers/locations/batch")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"locations": [%s, %s]}""".formatted(
								location(courierId, T0.plusSeconds(10), northOf(ATASEHIR, 50)),
								location(courierId, T0, start))))
				.andExpect(status().isAccepted());

		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
				.andExpect(jsonPath("$.totalDistanceMeters").value(closeTo(250.0, 0.01)));
		mockMvc.perform(get("/api/v1/couriers/{courierId}/store-entrances", courierId))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].entranceTime").value("2026-01-01T10:00:10Z"));
	}

	@Test
	void knownCourierWithoutEntrancesHasAnEmptyList() throws Exception {
		String courierId = newCourierId();
		postLocation(courierId, T0, northOf(ATASEHIR, 500));

		mockMvc.perform(get("/api/v1/couriers/{courierId}/store-entrances", courierId))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
				.andExpect(jsonPath("$.totalDistanceMeters").value(0.0));
	}

	@Test
	void unknownCourierIsNotFound() throws Exception {
		String courierId = newCourierId();

		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Courier not found: " + courierId));
		mockMvc.perform(get("/api/v1/couriers/{courierId}/store-entrances", courierId))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidLocationIsRejected() throws Exception {
		mockMvc.perform(post("/api/v1/couriers/locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"time": "2026-01-01T10:00:00Z", "courierId": "c1", "lat": 91, "lng": 29}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.lat").value("lat must be between -90 and 90"));
	}

	@Test
	void openApiDocumentationIsPublished() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("/api/v1/couriers/locations")));
	}

	@Test
	void healthEndpointIsUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	private void postLocation(String courierId, Instant time, GeoPoint point) throws Exception {
		mockMvc.perform(post("/api/v1/couriers/locations")
						.contentType(MediaType.APPLICATION_JSON)
						.content(location(courierId, time, point)))
				.andExpect(status().isAccepted());
	}

	private static String location(String courierId, Instant time, GeoPoint point) {
		return """
				{"time": "%s", "courierId": "%s", "lat": %s, "lng": %s}"""
				.formatted(time, courierId, point.latitude(), point.longitude());
	}

	private static String newCourierId() {
		return "it-" + UUID.randomUUID();
	}
}
