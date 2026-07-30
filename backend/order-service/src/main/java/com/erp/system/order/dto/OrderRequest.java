package com.erp.system.order.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotBlank(message = "Status is required")
    private String status;

    @NotEmpty(message = "At least one item is required")
    private List<ItemRequest> items;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    private String idempotencyKey;
}