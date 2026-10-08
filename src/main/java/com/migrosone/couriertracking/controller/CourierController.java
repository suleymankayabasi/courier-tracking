package com.migrosone.couriertracking.controller;

import com.migrosone.couriertracking.dto.LocationRequest;
import com.migrosone.couriertracking.dto.TotalDistanceResponse;
import com.migrosone.couriertracking.model.GeoPoint;
import com.migrosone.couriertracking.model.StoreEntrance;
import com.migrosone.couriertracking.service.CourierTrackingService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/couriers")
public class CourierController {
	private final CourierTrackingService courierTrackingService;

	public CourierController(CourierTrackingService courierTrackingService) {
		this.courierTrackingService = courierTrackingService;
	}

	@PostMapping("/locations")
	public void recordLocation(@Valid @RequestBody LocationRequest request) {
		courierTrackingService.recordLocation(
				request.courierId(), request.time(), new GeoPoint(request.lat(), request.lng()));
	}

	@GetMapping("/{courierId}/total-distance")
	public TotalDistanceResponse getTotalDistance(@PathVariable String courierId) {
		return new TotalDistanceResponse(courierId, courierTrackingService.getTotalTravelDistance(courierId));
	}

	@GetMapping("/{courierId}/store-entrances")
	public List<StoreEntrance> getStoreEntrances(@PathVariable String courierId) {
		return courierTrackingService.getStoreEntrances(courierId);
	}
}
