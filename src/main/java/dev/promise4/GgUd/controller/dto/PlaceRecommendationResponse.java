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
}
