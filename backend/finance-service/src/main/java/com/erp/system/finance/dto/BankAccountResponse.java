package com.erp.system.finance.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BankAccountResponse {
    private Long id;

    private String name;

    private String accountNumber;

    private Long accountId;

    private String accountCode;
}
