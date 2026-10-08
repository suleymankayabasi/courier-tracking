package com.migrosone.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Several courier geolocation events, of one or more couriers, in any order")
public record BatchLocationRequest(
		@NotEmpty(message = "locations must not be empty")
		@Size(max = BatchLocationRequest.MAX_LOCATIONS, message = "locations must contain at most {max} items")
		List<@NotNull(message = "location must not be null") @Valid CourierLocationRequest> locations) {
	public static final int MAX_LOCATIONS = 1000;
}
