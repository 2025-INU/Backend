package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.service.MidpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 중간지점 추천 및 확정 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/promises/{promiseId}/midpoint")
@RequiredArgsConstructor
@Tag(name = "Midpoint", description = "중간지점 추천/확정 API")
public class MidpointController {

    private final MidpointService midpointService;

    /**
     * 중간지점 추천 조회
     */
    @GetMapping("/recommendations")
    @Operation(summary = "중간지점 추천 조회", description = "모든 참여자의 출발지를 기반으로 중간지점을 계산하고 가까운 지하철역을 추천합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 조회 성공", content = @Content(schema = @Schema(implementation = MidpointRecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "출발지 미입력 또는 잘못된 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<MidpointRecommendationResponse> getRecommendations(
            @PathVariable Long promiseId) {

        log.debug("GET /api/v1/promises/{}/midpoint/recommendations", promiseId);
        MidpointRecommendationResponse response = midpointService.getRecommendations(promiseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 중간지점 확정 (호스트만)
     */
    @PostMapping("/confirm")
    @Operation(summary = "중간지점 확정", description = "호스트가 최종 중간지점을 확정합니다. 약속 상태가 CONFIRMED로 변경됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "확정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (호스트만 가능)")
    })
    public ResponseEntity<Void> confirmMidpoint(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId,
            @Valid @RequestBody ConfirmMidpointRequest request) {

        log.debug("POST /api/v1/promises/{}/midpoint/confirm - userId: {}, stationId: {}",
                promiseId, userId, request.getStationId());
        midpointService.confirmMidpoint(promiseId, userId, request.getStationId());
        return ResponseEntity.ok().build();
    }
}
