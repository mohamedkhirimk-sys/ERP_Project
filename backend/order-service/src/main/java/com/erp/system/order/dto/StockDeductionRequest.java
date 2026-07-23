package com.erp.system.order.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockDeductionRequest {
    private String sku;
    private Integer quantity;
}
