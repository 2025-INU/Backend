package dev.promise4.GgUd.controller.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SettlementTransferResponse {

    private Long fromUserId;
    private String fromNickname;
    private Long toUserId;
    private String toNickname;
    private BigDecimal amount;
}
