package com.erp.system.procurement.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPostingRequest {
    private String sourceType;
    private String sourceId;
    private BigDecimal totalAmount;
}
