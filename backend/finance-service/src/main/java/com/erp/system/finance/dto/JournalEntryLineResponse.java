package com.erp.system.finance.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryLineResponse {
    private Long id;
    private Long accountId;
    private String accountName;
    private String accountCode;
    private BigDecimal debit;
    private BigDecimal credit;
}
