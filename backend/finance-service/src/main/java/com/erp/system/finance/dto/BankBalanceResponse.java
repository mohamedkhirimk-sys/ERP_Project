package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BankBalanceResponse {
    private Long bankId;

    private String bankName;

    private String accountNumber;

    private String accountCode;

    private BigDecimal balance;
}
