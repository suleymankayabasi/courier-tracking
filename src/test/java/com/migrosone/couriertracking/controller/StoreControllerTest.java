package com.migrosone.couriertracking.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.migrosone.couriertracking.geo.GeoPoint;
import com.migrosone.couriertracking.service.CachedStore;
import com.migrosone.couriertracking.service.StoreService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreController.class)
class StoreControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StoreService storeService;

	@Test
	void listsTheCachedStores() throws Exception {
		when(storeService.findAll()).thenReturn(List.of(
				new CachedStore(1, "Ataşehir MMM Migros", new GeoPoint(40.9923307, 29.1244229))));

		mockMvc.perform(get("/api/v1/stores"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].name").value("Ataşehir MMM Migros"))
				.andExpect(jsonPath("$[0].lat").value(40.9923307))
				.andExpect(jsonPath("$[0].lng").value(29.1244229));
	}
}
