package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TreasuryTransferRequest {
    private Long fromBankAccountId;

    private Long toBankAccountId;

    private BigDecimal amount;

    private String description;
}
