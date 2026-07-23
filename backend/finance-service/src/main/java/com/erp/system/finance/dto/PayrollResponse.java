package com.erp.system.finance.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PayrollResponse {
    private Long id;
    private String employeeId;
    private String employeeName;
    private BigDecimal grossSalary;
    private BigDecimal deductions;
    private BigDecimal netSalary;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate paymentDate;
    private String status;
    private LocalDateTime createdAt;
}
