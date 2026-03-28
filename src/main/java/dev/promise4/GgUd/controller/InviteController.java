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
     * 초대 코드 조회 (카카오톡 등으로 공유용)
     */
    @GetMapping("/code")
    @Operation(summary = "초대 코드 조회", description = "6자리 초대 코드를 조회합니다. 카카오톡 등으로 공유하면 상대방이 앱에서 코드를 입력해 참여할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<Map<String, Object>> getInviteCode(@PathVariable Long promiseId) {
        log.debug("GET /api/v1/promises/{}/invite/code", promiseId);

        Promise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("약속을 찾을 수 없습니다"));

        return ResponseEntity.ok(Map.of(
                "inviteCode", promise.getInviteCode(),
                "expiredAt", promise.getInviteExpiredAt().toString(),
                "isValid", promise.isInviteValid()));
    }
}
