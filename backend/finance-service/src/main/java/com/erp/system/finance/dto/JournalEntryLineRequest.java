package com.erp.system.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryLineRequest {
    @NotNull private Long accountId;

    @NotNull @PositiveOrZero private BigDecimal debit;

    @NotNull @PositiveOrZero private BigDecimal credit;
}
