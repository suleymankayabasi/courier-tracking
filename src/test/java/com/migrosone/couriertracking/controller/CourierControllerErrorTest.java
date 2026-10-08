package com.migrosone.couriertracking.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.migrosone.couriertracking.service.CourierTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CourierController.class)
class CourierControllerErrorTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CourierTrackingService courierTrackingService;

	@Test
	void unexpectedErrorUsesTheProblemFormatWithoutLeakingDetails() throws Exception {
		given(courierTrackingService.getTotalTravelDistance(anyString()))
				.willThrow(new IllegalStateException("internal detail"));

		mockMvc.perform(get("/api/couriers/{id}/total-distance", "courier-1"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.detail").value("Unexpected server error"));
	}
}
