package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalSummaryResponse {
    private String code;
    private String label;
    private long entryCount;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
