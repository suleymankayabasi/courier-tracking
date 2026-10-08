package com.migrosone.couriertracking.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.migrosone.couriertracking.domain.CourierId;
import com.migrosone.couriertracking.domain.entity.CourierState;
import com.migrosone.couriertracking.dto.CourierLocationRequest;
import com.migrosone.couriertracking.dto.StoreEntranceResponse;
import com.migrosone.couriertracking.exception.CourierNotFoundException;
import com.migrosone.couriertracking.exception.InvalidLocationTimeException;
import com.migrosone.couriertracking.service.CourierService;
import com.migrosone.couriertracking.service.LocationService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CourierController.class)
class CourierControllerTest {
	private static final String LOCATIONS_URL = "/api/v1/couriers/locations";
	private static final String BATCH_URL = "/api/v1/couriers/locations/batch";
	private static final String BODY_TEMPLATE = """
			{"time": "%s", "courierId": "%s", "lat": %s, "lng": %s}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LocationService locationService;

	@MockitoBean
	private CourierService courierService;

	@Test
	void validLocationIsPassedToTheServiceAndAccepted() throws Exception {
		postJson(body("2026-01-01T10:00:00Z", "courier-1", "41.5", "29.25"))
				.andExpect(status().isAccepted())
				.andExpect(content().string(""));

		verify(locationService).recordLocation(
				new CourierLocationRequest(Instant.parse("2026-01-01T10:00:00Z"), "courier-1", 41.5, 29.25));
	}

	@Test
	void timeWithOffsetIsNormalisedToTheSameInstant() throws Exception {
		postJson(body("2026-01-01T13:00:00+03:00", "c1", "41.0", "29.0")).andExpect(status().isAccepted());

		ArgumentCaptor<CourierLocationRequest> captor = ArgumentCaptor.forClass(CourierLocationRequest.class);
		verify(locationService).recordLocation(captor.capture());
		assertThat(captor.getValue().time()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
	}

	@ParameterizedTest
	@CsvSource({ "90.0, 180.0", "-90.0, -180.0" })
	void coordinateBoundariesAreAccepted(String lat, String lng) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", "c1", lat, lng)).andExpect(status().isAccepted());
	}

	@Test
	void missingCourierIdIsRejectedWithTheCommonErrorBody() throws Exception {
		postJson("""
				{"time": "2026-01-01T10:00:00Z", "lat": 41.0, "lng": 29.0}""")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.error").value("Bad Request"))
				.andExpect(jsonPath("$.message").value("Request validation failed"))
				.andExpect(jsonPath("$.path").value(LOCATIONS_URL))
				.andExpect(jsonPath("$.fieldErrors.courierId").value("courierId is required"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "\\t", "  \\n  " })
	void blankCourierIdIsRejected(String courierId) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", courierId, "41.0", "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.courierId").value("courierId is required"));
		verifyNoInteractions(locationService);
	}

	@Test
	void tooLongCourierIdIsRejected() throws Exception {
		String courierId = "c".repeat(CourierId.MAX_LENGTH + 1);

		postJson(body("2026-01-01T10:00:00Z", courierId, "41.0", "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.courierId").value("courierId must be at most 64 characters"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "a\\nb", "a\\rb", "a b", "c1 ", "a/b", "ç" })
	void courierIdWithDisallowedCharactersIsRejected(String courierId) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", courierId, "41.0", "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.courierId").value(CourierId.ALLOWED_MESSAGE));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "courier-1", "COURIER_1", "device:42", "a.b-c_d", "1" })
	void courierIdWithAllowedCharactersIsAccepted(String courierId) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", courierId, "41.0", "29.0")).andExpect(status().isAccepted());
	}

	@Test
	void missingTimeIsRejected() throws Exception {
		postJson("""
				{"courierId": "c1", "lat": 41.0, "lng": 29.0}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.time").value("time is required"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "90.0001", "-90.0001" })
	void latitudeOutOfRangeIsRejected(String lat) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", "c1", lat, "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.lat").value("lat must be between -90 and 90"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "180.0001", "-180.0001" })
	void longitudeOutOfRangeIsRejected(String lng) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", "c1", "41.0", lng))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.lng").value("lng must be between -180 and 180"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', textBlock = """
			{"time": "2026-01-01T10:00:00Z", "courierId": null, "lat": 41.0, "lng": 29.0} | courierId | courierId is required
			{"time": null, "courierId": "c1", "lat": 41.0, "lng": 29.0}                   | time      | time is required
			{"time": "2026-01-01T10:00:00Z", "courierId": "c1", "lat": null, "lng": 29.0} | lat       | lat is required
			{"time": "2026-01-01T10:00:00Z", "courierId": "c1", "lat": 41.0, "lng": null} | lng       | lng is required
			""")
	void explicitJsonNullIsRejectedLikeAMissingField(String json, String field, String message) throws Exception {
		postJson(json)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.%s".formatted(field)).value(message))
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)));
		verifyNoInteractions(locationService);
	}

	@Test
	void emptyJsonObjectReportsEveryMissingField() throws Exception {
		postJson("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors", aMapWithSize(4)));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "2026-01-01T10:00:00", "2026-01-01", "yesterday" })
	void timeWithoutOffsetOrNotADateTimeIsMalformed(String time) throws Exception {
		postJson(body(time, "c1", "41.0", "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
		verifyNoInteractions(locationService);
	}

	@Test
	void numericEpochTimeIsMalformed() throws Exception {
		postJson("""
				{"time": 1767261600, "courierId": "c1", "lat": 41.0, "lng": 29.0}""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "\"abc\"", "true", "[41.0]", "{}", "NaN" })
	void nonNumericCoordinateIsMalformed(String lat) throws Exception {
		postJson(body("2026-01-01T10:00:00Z", "c1", lat, "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Malformed request body"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "null", "[]", "\"text\"", "{\"courierId\": " })
	void emptyOrNonObjectBodyIsMalformed(String json) throws Exception {
		postJson(json)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Malformed request body"));
		verifyNoInteractions(locationService);
	}

	@Test
	void nonJsonContentTypeIsUnsupported() throws Exception {
		mockMvc.perform(post(LOCATIONS_URL).contentType(MediaType.TEXT_PLAIN).content("hello"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.status").value(415))
				.andExpect(jsonPath("$.path").value(LOCATIONS_URL));
		verifyNoInteractions(locationService);
	}

	@Test
	void unsupportedMethodIsNotAllowed() throws Exception {
		mockMvc.perform(get(LOCATIONS_URL))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.status").value(405))
				.andExpect(jsonPath("$.error").value("Method Not Allowed"));
	}

	@Test
	void validBatchIsPassedToTheServiceAndAccepted() throws Exception {
		postJson(BATCH_URL, """
				{"locations": [%s, %s]}""".formatted(
				body("2026-01-01T10:00:10Z", "c1", "41.1", "29.0"), body("2026-01-01T10:00:00Z", "c2", "41.0", "29.0")))
				.andExpect(status().isAccepted());

		verify(locationService).recordLocations(List.of(
				new CourierLocationRequest(Instant.parse("2026-01-01T10:00:10Z"), "c1", 41.1, 29.0),
				new CourierLocationRequest(Instant.parse("2026-01-01T10:00:00Z"), "c2", 41.0, 29.0)));
	}

	@Test
	void invalidLocationsOfABatchAreReportedByIndex() throws Exception {
		postJson(BATCH_URL, """
				{"locations": [%s, null]}""".formatted(body("2026-01-01T10:00:00Z", "c1", "91", "29.0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors['locations[0].lat']").value("lat must be between -90 and 90"))
				.andExpect(jsonPath("$.fieldErrors['locations[1]']").value("location must not be null"));
		verifyNoInteractions(locationService);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"locations\": []}" })
	void emptyBatchIsRejected(String json) throws Exception {
		postJson(BATCH_URL, json)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.locations").value("locations must not be empty"));
		verifyNoInteractions(locationService);
	}

	@Test
	void oversizedBatchIsRejected() throws Exception {
		String location = body("2026-01-01T10:00:00Z", "c1", "41.0", "29.0");
		String locations = String.join(",", Collections.nCopies(1001, location));

		postJson(BATCH_URL, "{\"locations\": [" + locations + "]}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.locations").value("locations must contain at most 1000 items"));
		verifyNoInteractions(locationService);
	}

	@Test
	void totalDistanceOfACourier() throws Exception {
		when(courierService.getTotalTravelDistance("courier-1")).thenReturn(1234.5);

		mockMvc.perform(get("/api/v1/couriers/courier-1/total-distance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courierId").value("courier-1"))
				.andExpect(jsonPath("$.totalDistanceMeters").value(1234.5));
	}

	@Test
	void courierIdWithDisallowedCharactersInThePathIsRejected() throws Exception {
		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", "a b"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		verifyNoInteractions(courierService);
	}

	@Test
	void tooLongCourierIdInThePathIsRejected() throws Exception {
		String courierId = "c".repeat(CourierId.MAX_LENGTH + 1);

		mockMvc.perform(get("/api/v1/couriers/{courierId}/total-distance", courierId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
		verifyNoInteractions(courierService);
	}

	@Test
	void storeEntrancesOfACourier() throws Exception {
		when(courierService.getStoreEntrances("courier-1")).thenReturn(List.of(
				new StoreEntranceResponse(1, "Ataşehir MMM Migros", Instant.parse("2026-01-01T10:00:00Z"))));

		mockMvc.perform(get("/api/v1/couriers/courier-1/store-entrances"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].storeId").value(1))
				.andExpect(jsonPath("$[0].storeName").value("Ataşehir MMM Migros"))
				.andExpect(jsonPath("$[0].entranceTime").value("2026-01-01T10:00:00Z"));
	}

	@Test
	void unknownCourierIsNotFound() throws Exception {
		when(courierService.getTotalTravelDistance("ghost")).thenThrow(new CourierNotFoundException("ghost"));

		mockMvc.perform(get("/api/v1/couriers/ghost/total-distance"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("Courier not found: ghost"))
				.andExpect(jsonPath("$.path").value("/api/v1/couriers/ghost/total-distance"));
	}

	@Test
	void locationTooFarInTheFutureIsBadRequest() throws Exception {
		doThrow(new InvalidLocationTimeException("time 2999-01-01T00:00:00Z is more than PT5M in the future"))
				.when(locationService).recordLocation(any());

		postJson(body("2999-01-01T00:00:00Z", "c1", "41.0", "29.0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value(containsString("in the future")));
	}

	@Test
	void exhaustedConcurrencyRetriesAreAConflict() throws Exception {
		doThrow(new ObjectOptimisticLockingFailureException(CourierState.class, "c1"))
				.when(locationService).recordLocation(any());

		postJson(body("2026-01-01T10:00:00Z", "c1", "41.0", "29.0"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Concurrent update of the same courier, please retry"));
	}

	@Test
	void unexpectedFailureIsAGenericServerErrorWithoutInternals() throws Exception {
		when(courierService.getTotalTravelDistance(any())).thenThrow(new IllegalStateException("secret-internal-detail"));

		mockMvc.perform(get("/api/v1/couriers/c1/total-distance"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Unexpected server error"))
				.andExpect(content().string(not(containsString("secret-internal-detail"))))
				.andExpect(content().string(not(containsString("IllegalStateException"))));
	}

	private ResultActions postJson(String json) throws Exception {
		return postJson(LOCATIONS_URL, json);
	}

	private ResultActions postJson(String url, String json) throws Exception {
		return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String body(String time, String courierId, String lat, String lng) {
		return BODY_TEMPLATE.formatted(time, courierId, lat, lng);
	}
}
