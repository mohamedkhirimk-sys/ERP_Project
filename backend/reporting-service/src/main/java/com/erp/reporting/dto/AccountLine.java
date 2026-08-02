package com.erp.reporting.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountLine {
    private String accountCode;
    private String accountName;
    private BigDecimal balance;
}
