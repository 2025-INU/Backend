package dev.promise4.GgUd.controller.dto;

import dev.promise4.GgUd.entity.SubwayStation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 역 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "추천 역 정보")
public class StationRecommendation {

    @Schema(description = "역 ID", example = "1")
    private Long stationId;

    @Schema(description = "역 이름", example = "강남")
    private String stationName;

    @Schema(description = "노선명", example = "2호선")
    private String lineName;

    @Schema(description = "위도", example = "37.4979")
    private double latitude;

    @Schema(description = "경도", example = "127.0276")
    private double longitude;

    @Schema(description = "중간지점으로부터의 거리 (km)", example = "0.5")
    private double distanceFromMidpoint;

    @Schema(description = "참여자들로부터의 평균 거리 (km)", example = "5.2")
    private double averageDistanceFromParticipants;

    public static StationRecommendation from(SubwayStation station, double distanceFromMidpoint, double avgDistance) {
        return StationRecommendation.builder()
                .stationId(station.getId())
                .stationName(station.getStationName())
                .lineName(station.getLineName())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .distanceFromMidpoint(Math.round(distanceFromMidpoint * 100.0) / 100.0)
                .averageDistanceFromParticipants(Math.round(avgDistance * 100.0) / 100.0)
                .build();
    }
}
