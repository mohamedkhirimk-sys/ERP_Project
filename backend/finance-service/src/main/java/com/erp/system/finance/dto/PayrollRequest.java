package com.erp.system.finance.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayrollRequest {
    @NotBlank private String employeeId;
    @NotBlank private String employeeName;
    @NotNull @Positive private BigDecimal grossSalary;
    private BigDecimal deductions;
    @NotNull private LocalDate payPeriodStart;
    @NotNull private LocalDate payPeriodEnd;
}
