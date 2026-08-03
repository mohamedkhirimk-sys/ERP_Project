package com.erp.system.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BankAccountRequest {
    @NotBlank private String name;

    @NotBlank private String accountNumber;
}
