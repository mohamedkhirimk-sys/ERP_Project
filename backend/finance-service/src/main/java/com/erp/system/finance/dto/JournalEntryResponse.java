package com.erp.system.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryResponse {
    private Long id;
    private String entryNumber;
    private Long accountId;
    private String accountName;
    private String accountCode;
    private String description;
    private BigDecimal debit;
    private BigDecimal credit;
    private LocalDateTime entryDate;
    private LocalDateTime createdAt;
}
