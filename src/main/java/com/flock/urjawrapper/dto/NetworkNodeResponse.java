package com.flock.urjawrapper.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Topological grid tree configuration layout node")
public record NetworkNodeResponse(
        @Schema(example = "Substation-East") String name,
        @Schema(example = "DISTRIBUTION_TRANSFORMER") String type,
        List<NetworkNodeResponse> children,
        List<MeterResponse> leafMeters
) {}
