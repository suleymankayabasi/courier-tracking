package com.migrosone.couriertracking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class CourierControllerIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	private final String courierId = "courier-" + UUID.randomUUID();

	@Test
	void tracksEntrancesAndDistance(CapturedOutput output) throws Exception {
		// 0.0018 degrees of latitude is about 200 m: the courier walks from outside into the Ataşehir store radius.
		postLocation("2026-01-01T10:00:00Z", 40.9941307, 29.1244229).andExpect(status().isOk());
		postLocation("2026-01-01T10:00:30Z", 40.9923307, 29.1244229).andExpect(status().isOk());

		mockMvc.perform(get("/api/couriers/{id}/store-entrances", courierId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].courierId").value(courierId))
				.andExpect(jsonPath("$[0].storeName").value("Ataşehir MMM Migros"))
				.andExpect(jsonPath("$[0].time").value("2026-01-01T10:00:30Z"));
		mockMvc.perform(get("/api/couriers/{id}/total-distance", courierId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courierId").value(courierId))
				.andExpect(jsonPath("$.totalDistanceMeters").value(closeTo(200.15, 0.1)));
		assertThat(output).contains("Courier " + courierId + " entered Ataşehir MMM Migros at 2026-01-01T10:00:30Z");
	}

	@Test
	void unknownCourierIsNotFound() throws Exception {
		mockMvc.perform(get("/api/couriers/{id}/total-distance", courierId)).andExpect(status().isNotFound());
		mockMvc.perform(get("/api/couriers/{id}/store-entrances", courierId)).andExpect(status().isNotFound());
	}

	@Test
	void futureTimeIsRejected() throws Exception {
		postLocation(Instant.now().plusSeconds(3600).toString(), 40.99, 29.12)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").exists());
	}

	@Test
	void validationErrorsNameTheField() throws Exception {
		postLocation("2026-01-01T10:00:00Z", 91, 29.12)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.lat").value("lat must be between -90 and 90"));
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"{\"courierId\":\"c\",\"lat\":40.99,\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"lat\":40.99,\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"c\",\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"c\",\"lat\":40.99}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"\",\"lat\":40.99,\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"bad id\\nforged log line\",\"lat\":40.99,\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"c\",\"lat\":-90.1,\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"c\",\"lat\":40.99,\"lng\":180.1}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"c\",\"lat\":\"abc\",\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00Z\",\"courierId\":\"c\",\"lat\":\"NaN\",\"lng\":29.12}",
			"{\"time\":\"yesterday\",\"courierId\":\"c\",\"lat\":40.99,\"lng\":29.12}",
			"{\"time\":\"2026-01-01T10:00:00\",\"courierId\":\"c\",\"lat\":40.99,\"lng\":29.12}",
			"not json",
			""})
	void invalidLocationIsRejected(String body) throws Exception {
		mockMvc.perform(post("/api/couriers/locations").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest());
	}

	private ResultActions postLocation(String time, double lat, double lng) throws Exception {
		String body = """
				{"time":"%s","courierId":"%s","lat":%s,"lng":%s}""".formatted(time, courierId, lat, lng);
		return mockMvc.perform(post("/api/couriers/locations").contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
