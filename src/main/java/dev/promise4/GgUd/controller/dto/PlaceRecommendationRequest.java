package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 장소 추천 요청 (AI 서버 호출용)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 맞춤 장소 추천 요청")
public class PlaceRecommendationRequest {

    @Size(max = 200)
    @Schema(description = "자연어 요청 (예: 친구들이랑 분위기 좋은 카페). 미입력 시 '맛집'으로 자동 설정됩니다.")
    private String query;

    @Schema(description = "추천 탭 (ALL, RESTAURANT, CAFE, BAR)", example = "ALL")
    private PlaceRecommendationTab tab = PlaceRecommendationTab.ALL;
}
