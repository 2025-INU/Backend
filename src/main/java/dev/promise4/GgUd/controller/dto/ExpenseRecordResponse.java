package dev.promise4.GgUd.controller.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ExpenseRecordResponse {

    private Long userId;
    private String nickname;
    private String profileImageUrl;

    /** 이 참여자가 실제 결제한 금액 */
    private BigDecimal paidAmount;

    /** 양수: 받을 금액, 음수: 보낼 금액 */
    private BigDecimal balanceAmount;

    /** "SENDER" | "RECEIVER" | "SETTLED" */
    private String status;
}
