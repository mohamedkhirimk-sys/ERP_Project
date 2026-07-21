package com.erp.system.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderRequest(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotNull(message = "Total amount is required") @Positive(message = "Total amount must be positive") BigDecimal totalAmount,
        @NotBlank(message = "Status is required") String status
) {
}
