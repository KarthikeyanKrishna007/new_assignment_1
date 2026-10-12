package com.flock.urjawrapper.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured target smart meter representation")
public record MeterResponse(
        @Schema(example = "MTR-10293") String meterId,
        @Schema(example = "FLK-2026-X8") String serialNumber,
        @Schema(example = "active") String status,
        @Schema(example = "Transformer-B") String structuralNode
) {}
