package com.erp.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialReport {
    private Summary summary;
    private List<AccountBalance> trialBalance;
    private List<JournalSummary> recentEntries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalAccounts;
        private long totalJournalEntries;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountBalance {
        private String accountCode;
        private String accountName;
        private String accountType;
        private BigDecimal balance;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JournalSummary {
        private String entryNumber;
        private String description;
        private BigDecimal debit;
        private BigDecimal credit;
        private LocalDate entryDate;
    }
}
