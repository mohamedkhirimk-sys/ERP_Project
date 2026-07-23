package com.erp.system.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountRequest {
    @NotBlank private String accountCode;
    @NotBlank private String accountName;
    @NotBlank private String accountType;
    private String description;
}
