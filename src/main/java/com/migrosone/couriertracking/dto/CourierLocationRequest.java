package com.migrosone.couriertracking.dto;

import com.migrosone.couriertracking.domain.CourierId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import tools.jackson.databind.annotation.JsonDeserialize;

@Schema(description = "One courier geolocation event")
public record CourierLocationRequest(
		@Schema(description = "Event time, ISO-8601 with offset", example = "2026-01-01T10:00:00Z", type = "string",
				format = "date-time")
		@NotNull(message = "time is required")
		@JsonDeserialize(using = IsoOffsetInstantDeserializer.class) Instant time,

		@Schema(example = "courier-1")
		@NotBlank(message = "courierId is required")
		@Pattern(regexp = CourierId.ALLOWED_PATTERN, message = CourierId.ALLOWED_MESSAGE)
		@Size(max = CourierId.MAX_LENGTH, message = "courierId must be at most {max} characters")
		String courierId,

		@Schema(description = "Latitude in degrees", example = "40.9923307")
		@NotNull(message = "lat is required")
		@DecimalMin(value = "-90.0", message = "lat must be between -90 and 90")
		@DecimalMax(value = "90.0", message = "lat must be between -90 and 90") Double lat,

		@Schema(description = "Longitude in degrees", example = "29.1244229")
		@NotNull(message = "lng is required")
		@DecimalMin(value = "-180.0", message = "lng must be between -180 and 180")
		@DecimalMax(value = "180.0", message = "lng must be between -180 and 180") Double lng) {
}
