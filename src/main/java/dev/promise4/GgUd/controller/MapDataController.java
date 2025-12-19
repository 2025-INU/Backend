package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.Coordinate;
import dev.promise4.GgUd.controller.dto.DirectionsResponse;
import dev.promise4.GgUd.controller.dto.MapDataResponse;
import dev.promise4.GgUd.service.KakaoDirectionsService;
import dev.promise4.GgUd.service.MapDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 카카오맵 연동 데이터 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/promises/{promiseId}")
@RequiredArgsConstructor
@Tag(name = "Map", description = "카카오맵 연동 API")
public class MapDataController {

    private final MapDataService mapDataService;
    private final KakaoDirectionsService kakaoDirectionsService;

    /**
     * 지도 데이터 조회
     */
    @GetMapping("/map-data")
    @Operation(summary = "지도 데이터 조회", description = "카카오맵에 표시할 마커 데이터를 조회합니다. 확정 장소, 참여자 출발지, 실시간 위치 등을 포함합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = MapDataResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<MapDataResponse> getMapData(@PathVariable Long promiseId) {
        log.debug("GET /api/v1/promises/{}/map-data", promiseId);
        MapDataResponse response = mapDataService.getMapData(promiseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 길찾기 조회
     */
    @GetMapping("/directions")
    @Operation(summary = "길찾기 조회", description = "현재 위치에서 약속 장소까지의 경로를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = DirectionsResponse.class))),
            @ApiResponse(responseCode = "400", description = "경로 없음"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<DirectionsResponse> getDirections(
            @PathVariable Long promiseId,
            @Parameter(description = "출발지 위도") @RequestParam double originLat,
            @Parameter(description = "출발지 경도") @RequestParam double originLon,
            @Parameter(description = "도착지 위도") @RequestParam double destLat,
            @Parameter(description = "도착지 경도") @RequestParam double destLon) {

        log.debug("GET /api/v1/promises/{}/directions", promiseId);

        Coordinate origin = Coordinate.of(originLat, originLon);
        Coordinate destination = Coordinate.of(destLat, destLon);

        DirectionsResponse response = kakaoDirectionsService.getDirections(origin, destination).block();
        return ResponseEntity.ok(response);
    }
}
