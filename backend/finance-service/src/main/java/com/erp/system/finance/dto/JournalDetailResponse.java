package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalDetailResponse {
    private String code;
    private String label;
    private LocalDate from;
    private LocalDate to;
    private List<JournalEntryResponse> entries;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
}
