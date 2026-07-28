package com.erp.system.hrm.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private Long userId;
    private String phone;
    private String department;
    private String position;
    private BigDecimal salary;
    private LocalDate hireDate;
    private String status;
    private LocalDateTime createdAt;
}
