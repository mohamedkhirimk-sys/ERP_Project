package com.erp.system.order.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPostingRequest {
    private String sourceType;
    private String sourceId;
    private String customerName;
    private BigDecimal totalAmount;
}
