package dev.promise4.GgUd.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 장소 추천 응답 항목 (GgUd-AI 스키마와 동일)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "추천 장소 항목")
public class PlaceRecommendationItem {

    @JsonProperty("place_id")
    @Schema(description = "장소 ID")
    private String placeId;

    @JsonProperty("place_name")
    @Schema(description = "장소 이름")
    private String placeName;

    @Schema(description = "카테고리")
    private String category;

    @Schema(description = "주소")
    private String address;

    @Schema(description = "위도")
    private Double latitude;

    @Schema(description = "경도")
    private Double longitude;

    @JsonProperty("ai_score")
    @Schema(description = "AI 점수 (0~100)")
    private Double aiScore;

    @JsonProperty("distance_from_midpoint")
    @Schema(description = "중간지점으로부터의 거리 (km)")
    private Double distanceFromMidpoint;
}
