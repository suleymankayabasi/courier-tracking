package com.migrosone.couriertracking.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public record LocationRequest(
		@Schema(example = "2026-01-01T10:00:00Z")
		@NotNull(message = "time is required") Instant time,

		@Schema(example = "courier-1")
		@NotNull(message = "courierId is required")
		@Pattern(regexp = "[A-Za-z0-9._-]{1,64}",
				message = "courierId must be 1-64 letters, digits, '.', '_' or '-'") String courierId,

		@Schema(example = "40.9923307")
		@NotNull(message = "lat is required")
		@DecimalMin(value = "-90", message = "lat must be between -90 and 90")
		@DecimalMax(value = "90", message = "lat must be between -90 and 90") Double lat,

		@Schema(example = "29.1244229")
		@NotNull(message = "lng is required")
		@DecimalMin(value = "-180", message = "lng must be between -180 and 180")
		@DecimalMax(value = "180", message = "lng must be between -180 and 180") Double lng) {
}
