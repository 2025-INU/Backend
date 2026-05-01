package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.entity.PromiseStatus;
import dev.promise4.GgUd.service.PlaceRecommendationService;
import dev.promise4.GgUd.service.PromiseService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 약속 관련 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/promises")
@RequiredArgsConstructor
@Tag(name = "Promise", description = "약속 API")
public class PromiseController {

    private final PromiseService promiseService;
    private final PlaceRecommendationService placeRecommendationService;

    /**
     * 약속 생성
     */
    @PostMapping
    @Operation(summary = "약속 생성", description = "새로운 약속을 생성합니다. 생성자는 자동으로 호스트로 참여됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "약속 생성 성공", content = @Content(schema = @Schema(implementation = PromiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<PromiseResponse> createPromise(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePromiseRequest request) {

        log.debug("POST /api/v1/promises - userId: {}", userId);
        PromiseResponse response = promiseService.createPromise(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 초대 정보 조회
     */
    @GetMapping("/invite/{inviteCode}")
    @Operation(summary = "초대 정보 조회", description = "초대 코드로 약속 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = PromiseResponse.class))),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드")
    })
    public ResponseEntity<PromiseResponse> getInviteInfo(
            @PathVariable String inviteCode) {

        log.debug("GET /api/v1/promises/invite/{}", inviteCode);
        PromiseResponse response = promiseService.getInviteInfo(inviteCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 약속 참여
     */
    @PostMapping("/join/{inviteCode}")
    @Operation(summary = "약속 참여", description = "초대 코드를 사용하여 약속에 참여합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여 성공", content = @Content(schema = @Schema(implementation = PromiseResponse.class))),
            @ApiResponse(responseCode = "400", description = "초대 만료/최대 인원 초과/이미 참여"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "유효하지 않은 초대 코드")
    })
    public ResponseEntity<PromiseResponse> joinPromise(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable String inviteCode) {

        log.debug("POST /api/v1/promises/join/{} - userId: {}", inviteCode, userId);
        PromiseResponse response = promiseService.joinPromise(userId, inviteCode);
        return ResponseEntity.ok(response);
    }

    /**
     * 출발지 입력
     */
    @PutMapping("/{promiseId}/departure")
    @Operation(summary = "출발지 입력", description = "참여자의 출발지 정보를 입력/수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입력 성공", content = @Content(schema = @Schema(implementation = ParticipantResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "참여자 아님")
    })
    public ResponseEntity<ParticipantResponse> submitDepartureLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId,
            @Valid @RequestBody UpdateDepartureRequest request) {

        log.debug("PUT /api/v1/promises/{}/departure - userId: {}", promiseId, userId);
        ParticipantResponse response = promiseService.submitDepartureLocation(promiseId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 참여자 목록 조회
     */
    @GetMapping("/{promiseId}/participants")
    @Operation(summary = "참여자 목록 조회", description = "약속의 모든 참여자 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<List<ParticipantResponse>> getParticipants(
            @PathVariable Long promiseId) {

        log.debug("GET /api/v1/promises/{}/participants", promiseId);
        List<ParticipantResponse> response = promiseService.getParticipants(promiseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 내 약속 목록 조회
     */
    @GetMapping
    @Operation(summary = "내 약속 목록 조회", description = "내가 참여한 약속 목록을 조회합니다. 상태별 필터링 및 키워드 검색 가능.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Page<PromiseResponse>> getMyPromises(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "상태 필터") @RequestParam(required = false) PromiseStatus status,
            @Parameter(description = "검색 키워드 (약속 제목 또는 참여자 닉네임)") @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("GET /api/v1/promises - userId: {}, status: {}, keyword: {}", userId, status, keyword);
        Page<PromiseResponse> response = promiseService.getMyPromises(userId, status, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 약속 상세 조회
     */
    @GetMapping("/{promiseId}")
    @Operation(summary = "약속 상세 조회", description = "약속의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = PromiseResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<PromiseResponse> getPromise(
            @PathVariable Long promiseId) {

        log.debug("GET /api/v1/promises/{}", promiseId);
        PromiseResponse response = promiseService.getPromise(promiseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 약속 요약 조회 (제목, 일시, 주최자)
     */
    @GetMapping("/{promiseId}/summary")
    @Operation(summary = "약속 요약 조회", description = "약속의 제목, 일시, 주최자 정보만 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = PromiseSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<PromiseSummaryResponse> getPromiseSummary(
            @PathVariable Long promiseId) {

        log.debug("GET /api/v1/promises/{}/summary", promiseId);
        PromiseSummaryResponse response = promiseService.getPromiseSummary(promiseId);
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 맞춤 장소 추천 (AI 서버 연동)
     */
    @PostMapping("/{promiseId}/place-recommendations")
    @Operation(summary = "장소 추천", description = "자연어 요청으로 AI 기반 장소 추천을 받습니다. 중간지점이 확정된 경우 근처 장소를 우선 추천합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "추천 성공", content = @Content(schema = @Schema(implementation = PlaceRecommendationResponse.class))),
            @ApiResponse(responseCode = "400", description = "참여자 아님 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<PlaceRecommendationResponse> getPlaceRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId,
            @Valid @RequestBody PlaceRecommendationRequest request) {

        log.debug("POST /api/v1/promises/{}/place-recommendations - userId: {}", promiseId, userId);
        PlaceRecommendationResponse response = placeRecommendationService.getPlaceRecommendations(promiseId, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 장소 선택 기록 (히스토리 저장용)
     */
    @PostMapping("/{promiseId}/recommendations/select")
    @Operation(summary = "장소 선택 기록", description = "추천 결과에서 장소를 선택했을 때 히스토리를 저장합니다. 개인화 추천에 활용됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "기록 성공"),
            @ApiResponse(responseCode = "400", description = "참여자 아님 또는 잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Void> recordPlaceSelection(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId,
            @Valid @RequestBody PlaceSelectRequest request) {

        log.debug("POST /api/v1/promises/{}/recommendations/select - userId: {}", promiseId, userId);
        placeRecommendationService.recordPlaceSelection(promiseId, userId, request.getPlaceId(), request.getQueryId());
        return ResponseEntity.ok().build();
    }

    /**
     * 약속 취소 (호스트 전용)
     */
    @PatchMapping("/{promiseId}/cancel")
    @Operation(summary = "약속 취소", description = "호스트가 약속을 취소합니다. 완료되거나 이미 취소된 약속은 취소할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "취소 성공"),
            @ApiResponse(responseCode = "400", description = "취소 불가능한 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "호스트가 아님")
    })
    public ResponseEntity<Void> cancelPromise(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId) {

        log.debug("PATCH /api/v1/promises/{}/cancel - userId: {}", promiseId, userId);
        promiseService.cancelPromise(promiseId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 중간지점 선택 시작 (호스트 전용)
     */
    @PostMapping("/{promiseId}/start-midpoint-selection")
    @Operation(summary = "중간지점 선택 시작", description = "호스트가 모든 참여자의 출발지 입력을 확인 후 다음 단계로 진행합니다. 미입력 참여자가 있으면 실패합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "진행 성공"),
            @ApiResponse(responseCode = "400", description = "미입력 참여자 존재 또는 잘못된 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "호스트가 아님")
    })
    public ResponseEntity<Void> startMidpointSelection(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId) {

        log.debug("POST /api/v1/promises/{}/start-midpoint-selection - userId: {}", promiseId, userId);
        promiseService.startSelectingMidpoint(promiseId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 약속 종료 (호스트 전용)
     */
    @PatchMapping("/{promiseId}/complete")
    @Operation(summary = "약속 종료", description = "호스트가 약속을 종료합니다. IN_PROGRESS 상태에서만 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "종료 성공"),
            @ApiResponse(responseCode = "400", description = "진행 중이 아닌 약속"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "호스트가 아님")
    })
    public ResponseEntity<Void> completePromise(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId) {

        log.debug("PATCH /api/v1/promises/{}/complete - userId: {}", promiseId, userId);
        promiseService.completePromise(promiseId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 약속 상태 조회
     */
    @GetMapping("/{promiseId}/status")
    @Operation(summary = "약속 상태 조회", description = "약속의 현재 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<PromiseStatus> getPromiseStatus(
            @PathVariable Long promiseId) {

        log.debug("GET /api/v1/promises/{}/status", promiseId);
        PromiseStatus status = promiseService.getPromiseStatus(promiseId);
        return ResponseEntity.ok(status);
    }
}
