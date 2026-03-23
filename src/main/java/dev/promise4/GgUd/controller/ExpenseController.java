package dev.promise4.GgUd.controller;

import dev.promise4.GgUd.controller.dto.SettlementResponse;
import dev.promise4.GgUd.controller.dto.UpdateMyExpenseRequest;
import dev.promise4.GgUd.service.ExpenseService;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/promises/{promiseId}/expenses")
@RequiredArgsConstructor
@Tag(name = "Expense", description = "정산 API")
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    @Operation(summary = "정산 현황 조회", description = "약속의 정산 현황과 이체 내역을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SettlementResponse.class))),
            @ApiResponse(responseCode = "400", description = "정산 불가 상태"),
            @ApiResponse(responseCode = "403", description = "참여자가 아님"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<SettlementResponse> getSettlement(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId) {

        log.debug("GET /api/v1/promises/{}/expenses - userId: {}", promiseId, userId);
        return ResponseEntity.ok(expenseService.getSettlement(promiseId, userId));
    }

    @PutMapping("/my")
    @Operation(summary = "내 결제 금액 입력/수정", description = "내가 결제한 금액을 입력하거나 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입력/수정 성공",
                    content = @Content(schema = @Schema(implementation = SettlementResponse.class))),
            @ApiResponse(responseCode = "400", description = "정산 완료 상태 또는 약속 상태 불일치"),
            @ApiResponse(responseCode = "403", description = "참여자가 아님"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<SettlementResponse> updateMyExpense(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId,
            @Valid @RequestBody UpdateMyExpenseRequest request) {

        log.debug("PUT /api/v1/promises/{}/expenses/my - userId: {}", promiseId, userId);
        return ResponseEntity.ok(expenseService.updateMyExpense(promiseId, userId, request.getAmount()));
    }

    @PostMapping("/settle")
    @Operation(summary = "정산 완료", description = "정산을 완료하고 모든 참여자에게 카카오톡 알림을 전송합니다. 호스트만 가능.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정산 완료 성공",
                    content = @Content(schema = @Schema(implementation = SettlementResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 정산 완료 또는 약속 상태 불일치"),
            @ApiResponse(responseCode = "403", description = "호스트가 아님"),
            @ApiResponse(responseCode = "404", description = "약속 없음")
    })
    public ResponseEntity<SettlementResponse> completeSettlement(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PathVariable Long promiseId) {

        log.debug("POST /api/v1/promises/{}/expenses/settle - userId: {}", promiseId, userId);
        return ResponseEntity.ok(expenseService.completeSettlement(promiseId, userId));
    }
}
