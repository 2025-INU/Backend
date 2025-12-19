package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 중간지점 추천 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "중간지점 추천 응답")
public class MidpointRecommendationResponse {

    @Schema(description = "계산된 중간지점 좌표")
    private Coordinate calculatedMidpoint;

    @Schema(description = "추천 역 목록 (거리순)")
    private List<StationRecommendation> recommendedStations;

    @Schema(description = "참여자 수", example = "5")
    private int participantCount;
}
