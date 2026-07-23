package com.erp.system.sales.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceRequest {
    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    @NotBlank(message = "Status is required")
    private String status;

    private LocalDateTime dueDate;

    @NotEmpty(message = "At least one item is required")
    private List<InvoiceItemRequest> items;
}
