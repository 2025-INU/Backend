package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 카카오맵 연동 데이터 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카카오맵 연동 데이터")
public class MapDataResponse {

    @Schema(description = "약속 ID", example = "1")
    private Long promiseId;

    @Schema(description = "확정된 약속 장소")
    private MapMarker destination;

    @Schema(description = "참여자 출발지 목록")
    private List<ParticipantMarker> participantDepartures;

    @Schema(description = "추천 중간지점 목록")
    private List<MapMarker> recommendedMidpoints;

    @Schema(description = "실시간 위치 (IN_PROGRESS일 때만)")
    private List<ParticipantMarker> currentLocations;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지도 마커")
    public static class MapMarker {
        @Schema(description = "위도", example = "37.5665")
        private double latitude;

        @Schema(description = "경도", example = "126.9780")
        private double longitude;

        @Schema(description = "이름", example = "강남역")
        private String name;

        @Schema(description = "마커 타입", example = "DESTINATION")
        private MarkerType type;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "참여자 마커")
    public static class ParticipantMarker {
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "닉네임", example = "홍길동")
        private String nickname;

        @Schema(description = "프로필 이미지")
        private String profileImageUrl;

        @Schema(description = "위도", example = "37.5665")
        private double latitude;

        @Schema(description = "경도", example = "126.9780")
        private double longitude;

        @Schema(description = "호스트 여부", example = "false")
        private boolean host;
    }

    public enum MarkerType {
        DESTINATION, // 확정 장소
        DEPARTURE, // 출발지
        MIDPOINT, // 추천 중간지점
        CURRENT_LOCATION // 실시간 위치
    }
}
