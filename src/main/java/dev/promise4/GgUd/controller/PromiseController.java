package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.*;
import dev.promise4.GgUd.entity.PromiseStatus;
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
     * 내 약속 요약 목록 조회 (제목, 일시, 주최자만)
     */
    @GetMapping("/summary")
    @Operation(summary = "내 약속 요약 목록 조회", description = "약속 제목, 일시, 주최자만 포함된 간략한 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Page<PromiseSummaryResponse>> getMyPromiseSummaries(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(description = "상태 필터") @RequestParam(required = false) PromiseStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("GET /api/v1/promises/summary - userId: {}, status: {}", userId, status);
        Page<PromiseSummaryResponse> response = promiseService.getMyPromiseSummaries(userId, status, pageable);
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
