package com.erp.system.hrm.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LeaveRequest {
    @NotNull private Long employeeId;
    @NotBlank private String leaveType;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String reason;
}
