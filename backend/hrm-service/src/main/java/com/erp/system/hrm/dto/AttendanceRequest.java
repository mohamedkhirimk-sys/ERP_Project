package com.erp.system.hrm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceRequest {
    @NotNull private Long employeeId;
    @NotNull private LocalDate date;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;
    private String status;
}
