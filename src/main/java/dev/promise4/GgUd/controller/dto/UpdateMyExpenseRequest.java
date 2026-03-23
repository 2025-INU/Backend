package dev.promise4.GgUd.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "내 결제 금액 입력/수정 요청")
public class UpdateMyExpenseRequest {

    @NotNull(message = "금액은 필수입니다")
    @DecimalMin(value = "0", message = "결제 금액은 0원 이상이어야 합니다")
    @Schema(description = "결제 금액", example = "14000")
    private BigDecimal amount;
}
