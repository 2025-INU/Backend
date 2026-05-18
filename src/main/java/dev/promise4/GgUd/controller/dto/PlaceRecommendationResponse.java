package dev.promise4.GgUd.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * AI 장소 추천 응답 (GgUd-AI 스키마와 동일)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 추천 응답")
public class PlaceRecommendationResponse {

    @JsonProperty("promise_id")
    @Schema(description = "약속 ID")
    private Long promiseId;

    @Schema(description = "추천 장소 목록")
    private List<PlaceRecommendationItem> recommendations;

    @Schema(description = "현재 사용자가 호스트 여부", example = "true")
    private boolean host;

    @Schema(description = "확정된 중간지점 역 이름")
    private String midpointStationName;
}
