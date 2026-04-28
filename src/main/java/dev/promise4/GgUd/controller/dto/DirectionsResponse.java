package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 길찾기 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "길찾기 응답")
public class DirectionsResponse {

    @Schema(description = "경로 목록 (소요 시간 오름차순)")
    private List<RouteOption> routeOptions;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "경로 옵션")
    public static class RouteOption {

        @Schema(description = "총 소요 시간 (분)", example = "45")
        private int totalDuration;

        @Schema(description = "총 거리 (m)", example = "12500")
        private int totalDistance;

        @Schema(description = "경로 단계 목록")
        private List<RouteStep> routes;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "경로 단계")
    public static class RouteStep {

        @Schema(description = "이동 수단", example = "SUBWAY")
        private TransportType type;

        @Schema(description = "안내 문구", example = "2호선 강남역 방면")
        private String instruction;

        @Schema(description = "소요 시간 (분)", example = "15")
        private int duration;

        @Schema(description = "거리 (m)", example = "5000")
        private int distance;

        @Schema(description = "노선 정보 (대중교통의 경우)")
        private String lineName;
    }

    public enum TransportType {
        WALK,
        BUS,
        SUBWAY,
        TRANSFER
    }
}
