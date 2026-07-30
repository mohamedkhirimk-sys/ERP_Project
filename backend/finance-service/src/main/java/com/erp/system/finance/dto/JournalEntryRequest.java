package com.erp.system.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryRequest {
    @NotBlank private String description;

    @NotEmpty(message = "At least one line is required")
    private List<JournalEntryLineRequest> lines;
}
