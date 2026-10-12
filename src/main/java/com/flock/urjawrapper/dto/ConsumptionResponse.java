package com.flock.urjawrapper.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.ZonedDateTime;

@Schema(description = "Normalized time-series data point")
public record ConsumptionResponse(
        @Schema(example = "2026-10-12T00:00:00Z") ZonedDateTime timestamp,
        @Schema(example = "124.50") double readingKwh
) {}
