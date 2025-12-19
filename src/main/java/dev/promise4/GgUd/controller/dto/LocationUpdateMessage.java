package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 위치 업데이트 메시지 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "위치 업데이트 메시지")
public class LocationUpdateMessage {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "위도", example = "37.5665")
    private double latitude;

    @Schema(description = "경도", example = "126.9780")
    private double longitude;

    @Schema(description = "타임스탬프")
    private LocalDateTime timestamp;

    public static LocationUpdateMessage of(Long userId, String nickname, double latitude, double longitude) {
        return LocationUpdateMessage.builder()
                .userId(userId)
                .nickname(nickname)
                .latitude(latitude)
                .longitude(longitude)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
