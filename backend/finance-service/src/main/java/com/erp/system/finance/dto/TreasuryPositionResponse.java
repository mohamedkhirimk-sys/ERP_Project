package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TreasuryPositionResponse {
    private List<BankBalanceResponse> banks;

    private BigDecimal vaultBalance;

    private BigDecimal total;
}
