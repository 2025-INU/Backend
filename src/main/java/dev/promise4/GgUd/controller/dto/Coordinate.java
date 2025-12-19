package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 좌표 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "좌표")
public class Coordinate {

    @Schema(description = "위도", example = "37.5665")
    private double latitude;

    @Schema(description = "경도", example = "126.9780")
    private double longitude;

    /**
     * Haversine 공식으로 두 좌표 사이의 거리 계산 (km)
     */
    public double distanceTo(Coordinate other) {
        final double R = 6371; // 지구 반지름 (km)

        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                        * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public static Coordinate of(double latitude, double longitude) {
        return new Coordinate(latitude, longitude);
    }
}
