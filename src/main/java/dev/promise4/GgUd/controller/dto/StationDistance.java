package dev.promise4.GgUd.controller.dto;

import dev.promise4.GgUd.entity.SubwayStation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 역과 거리 정보
 */
@Getter
@AllArgsConstructor
@Schema(description = "역과 거리 정보")
public class StationDistance {

    @Schema(description = "지하철역")
    private SubwayStation station;

    @Schema(description = "거리 (km)", example = "2.5")
    private double distanceKm;
}
