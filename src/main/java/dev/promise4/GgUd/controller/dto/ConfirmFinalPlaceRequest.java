package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 최종 약속 장소 확정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "최종 약속 장소 확정 요청")
public class ConfirmFinalPlaceRequest {

    @Schema(description = "AI 추천 장소 ID (선택)", example = "abc123")
    private String placeId;

    @NotBlank(message = "장소명은 필수입니다")
    @Schema(description = "장소명", example = "스타벅스 강남점", required = true)
    private String placeName;

    @NotNull(message = "위도는 필수입니다")
    @Schema(description = "위도", example = "37.4979")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    @Schema(description = "경도", example = "127.0276")
    private Double longitude;
}
