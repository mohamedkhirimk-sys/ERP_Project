package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TreasuryWithdrawalRequest {
    private Long bankAccountId;

    private BigDecimal amount;

    private String description;
}
