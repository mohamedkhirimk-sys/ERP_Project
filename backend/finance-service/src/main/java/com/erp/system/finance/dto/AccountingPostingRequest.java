package com.erp.system.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPostingRequest {

    @NotBlank(message = "Source type is required")
    private String sourceType;

    @NotBlank(message = "Source id is required")
    private String sourceId;

    private String customerName;

    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;
}
