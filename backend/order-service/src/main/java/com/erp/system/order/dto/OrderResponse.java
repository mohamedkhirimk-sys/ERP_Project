package com.erp.system.order.dto;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        String orderNumber,
        String customerName,
        BigDecimal totalAmount,
        String status
) {
}
