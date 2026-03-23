package dev.promise4.GgUd.controller.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SettlementResponse {

    private Long promiseId;
    private String promiseName;
    private BigDecimal totalAmount;
    private BigDecimal perPersonAmount;
    private int participantCount;
    private boolean isSettlementCompleted;
    private LocalDateTime settlementCompletedAt;
    private List<ExpenseRecordResponse> expenses;
    private List<SettlementTransferResponse> transfers;
}
