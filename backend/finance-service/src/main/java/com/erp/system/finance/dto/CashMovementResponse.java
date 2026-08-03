package com.erp.system.finance.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CashMovementResponse {
    private Long id;

    private String movementType;

    private Long bankAccountId;

    private Long toBankAccountId;

    private BigDecimal amount;

    private String description;

    private Long entryId;

    private LocalDateTime createdAt;
}
