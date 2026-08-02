package com.erp.reporting.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BalanceSheetResponse {
    private List<AccountLine> assets;
    private List<AccountLine> liabilities;
    private List<AccountLine> equity;
    private BigDecimal netIncome;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal totalLiabilitiesEquity;
}
