package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 참여자별 이동 정보 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "참여자별 이동 정보")
public class ParticipantTravelInfo {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "출발지 주소", example = "서울시 강남구")
    private String departureAddress;

    @Schema(description = "예상 이동 시간 (분)", example = "25")
    private int travelTimeMinutes;

    @Schema(description = "이동 거리 (m)", example = "8500")
    private int distanceMeters;
}
