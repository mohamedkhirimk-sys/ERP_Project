package com.erp.system.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private String status;
    private List<ItemResponse> items;
}
