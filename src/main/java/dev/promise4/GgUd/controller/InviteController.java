package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.entity.Promise;
import dev.promise4.GgUd.repository.PromiseRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 초대 링크 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/promises/{promiseId}/invite")
@RequiredArgsConstructor
@Tag(name = "Invite", description = "초대 API")
public class InviteController {

    private final PromiseRepository promiseRepository;

    /**
     * 초대 링크 조회
     */
    @GetMapping("/link")
    @Operation(summary = "초대 링크 조회", description = "초대 링크 URL을 조회합니다. 클라이언트에서 복사/공유합니다.")
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
