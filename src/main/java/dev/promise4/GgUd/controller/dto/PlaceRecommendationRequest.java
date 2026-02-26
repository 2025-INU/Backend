package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "검색어는 필수입니다")
    @Size(max = 200)
    @Schema(description = "자연어 요청 (예: 친구들이랑 분위기 좋은 카페)", required = true)
    private String query;

    @Min(1)
    @Max(20)
    @Schema(description = "추천 개수 (1~20, 기본 10)", example = "10")
    private Integer limit = 10;

    @Schema(description = "true면 캐시 무시하고 AI에서 새로 추천 받음 (다시 추천 받기)", example = "false")
    private Boolean forceRefresh = false;
}
