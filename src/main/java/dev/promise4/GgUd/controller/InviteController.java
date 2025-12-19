package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.repository.PromiseRepository;
import dev.promise4.GgUd.service.KakaoMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 초대 메시지 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/promises/{promiseId}/invite")
@RequiredArgsConstructor
@Tag(name = "Invite", description = "초대 API")
public class InviteController {

    private final PromiseRepository promiseRepository;
    private final KakaoMessageService kakaoMessageService;

    /**
     * 초대 메시지 전송
     */
    @PostMapping("/send")
    @Operation(summary = "초대 메시지 전송", description = "카카오톡으로 초대 메시지를 전송합니다. (사용자 동의 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전송 성공"),
            @ApiResponse(responseCode = "400", description = "전송 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<Map<String, Object>> sendInviteMessage(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId,
            @RequestHeader(value = "X-Kakao-Access-Token", required = false) String kakaoAccessToken) {

        log.debug("POST /api/v1/promises/{}/invite/send - userId: {}", promiseId, userId);

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        if (kakaoAccessToken == null || kakaoAccessToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "카카오 액세스 토큰이 필요합니다"));
        }

        String inviteUrl = "https://ggud.promise4.dev/invite/" + promise.getInviteCode();

        Boolean result = kakaoMessageService.sendInviteMessage(
                kakaoAccessToken,
                promise.getTitle(),
                promise.getPromiseDateTime(),
                inviteUrl).block();

        if (Boolean.TRUE.equals(result)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "초대 메시지가 전송되었습니다"));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "메시지 전송에 실패했습니다"));
        }
    }

    /**
     * 초대 링크 조회
     */
    @GetMapping("/link")
    @Operation(summary = "초대 링크 조회", description = "초대 링크 URL을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<Map<String, Object>> getInviteLink(@PathVariable Long promiseId) {
        log.debug("GET /api/v1/promises/{}/invite/link", promiseId);

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        String inviteUrl = "https://ggud.promise4.dev/invite/" + promise.getInviteCode();

        return ResponseEntity.ok(Map.of(
                "inviteCode", promise.getInviteCode(),
                "inviteUrl", inviteUrl,
                "expiredAt", promise.getInviteExpiredAt().toString(),
                "isValid", promise.isInviteValid()));
    }
}
