package com.erp.system.finance.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JournalEntryResponse {
    private Long id;
    private String entryNumber;
    private String description;
    private LocalDateTime entryDate;
    private LocalDateTime createdAt;
    private List<JournalEntryLineResponse> lines;
}
