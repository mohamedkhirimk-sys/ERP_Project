package com.erp.system.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryRequest {
    @NotNull private Long accountId;
    @NotBlank private String description;
    @NotNull @Positive private BigDecimal debit;
    @NotNull @Positive private BigDecimal credit;
}
