package com.erp.system.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String accountCode;
    private String accountName;
    private String accountType;
    private String description;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
