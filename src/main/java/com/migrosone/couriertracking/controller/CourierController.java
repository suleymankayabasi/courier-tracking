package com.migrosone.couriertracking.controller;

import com.migrosone.couriertracking.domain.CourierId;
import com.migrosone.couriertracking.dto.BatchLocationRequest;
import com.migrosone.couriertracking.dto.CourierLocationRequest;
import com.migrosone.couriertracking.dto.StoreEntranceResponse;
import com.migrosone.couriertracking.dto.TotalDistanceResponse;
import com.migrosone.couriertracking.service.CourierService;
import com.migrosone.couriertracking.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couriers")
@Tag(name = "Couriers", description = "Location ingestion and courier queries")
public class CourierController {
	private final LocationService locationService;
	private final CourierService courierService;

	public CourierController(LocationService locationService, CourierService courierService) {
		this.locationService = locationService;
		this.courierService = courierService;
	}

	@PostMapping("/locations")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(summary = "Ingest one courier location",
			description = "Out-of-order locations (older than the courier's last accepted one) are stored in the raw "
					+ "log but ignored for entrances and distance.")
	@ApiResponse(responseCode = "202", description = "Location accepted")
	@ApiResponse(responseCode = "400", description = "Invalid location")
	public void recordLocation(@Valid @RequestBody CourierLocationRequest request) {
		locationService.recordLocation(request);
	}

	@PostMapping("/locations/batch")
	@ResponseStatus(HttpStatus.ACCEPTED)
	@Operation(summary = "Ingest several courier locations",
			description = "Locations are processed one by one in event-time order; the batch is not atomic.")
	@ApiResponse(responseCode = "202", description = "Locations accepted")
	@ApiResponse(responseCode = "400", description = "Invalid batch or location")
	public void recordLocations(@Valid @RequestBody BatchLocationRequest request) {
		locationService.recordLocations(request.locations());
	}

	@GetMapping("/{courierId}/total-distance")
	@Operation(summary = "Total distance travelled by a courier, in meters")
	@ApiResponse(responseCode = "200", description = "Distance of a known courier")
	@ApiResponse(responseCode = "404", description = "No location of this courier was accepted yet")
	public TotalDistanceResponse getTotalDistance(@PathVariable @NotBlank @Size(max = CourierId.MAX_LENGTH) @Pattern(regexp = CourierId.ALLOWED_PATTERN) String courierId) {
		return new TotalDistanceResponse(courierId, courierService.getTotalTravelDistance(courierId));
	}

	@GetMapping("/{courierId}/store-entrances")
	@Operation(summary = "Counted store entrances of a courier, oldest first")
	@ApiResponse(responseCode = "200", description = "Entrances of a known courier, possibly empty")
	@ApiResponse(responseCode = "404", description = "No location of this courier was accepted yet")
	public List<StoreEntranceResponse> getStoreEntrances(@PathVariable @NotBlank @Size(max = CourierId.MAX_LENGTH) @Pattern(regexp = CourierId.ALLOWED_PATTERN) String courierId) {
		return courierService.getStoreEntrances(courierId);
	}
}
