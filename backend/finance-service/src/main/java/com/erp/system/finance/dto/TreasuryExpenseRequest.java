package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TreasuryExpenseRequest {
    private Long bankAccountId;

    private Long expenseAccountId;

    private BigDecimal amount;

    private String description;
}
