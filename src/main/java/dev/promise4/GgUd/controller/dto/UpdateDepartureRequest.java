package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 출발지 입력 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "출발지 입력 요청")
public class UpdateDepartureRequest {

    @NotNull(message = "위도는 필수입니다")
    @Schema(description = "출발지 위도", example = "37.5665")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다")
    @Schema(description = "출발지 경도", example = "126.9780")
    private Double longitude;

    @Schema(description = "출발지 주소", example = "서울특별시 중구 세종대로 110")
    private String address;
}
