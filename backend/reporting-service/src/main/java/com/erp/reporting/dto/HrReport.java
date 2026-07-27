package com.erp.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrReport {
    private Summary summary;
    private List<DepartmentSummary> byDepartment;
    private List<PayrollSummary> recentPayroll;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalEmployees;
        private long activeEmployees;
        private long terminatedEmployees;
        private long pendingLeaves;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private String department;
        private long employeeCount;
        private BigDecimal totalSalary;
        private BigDecimal avgSalary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayrollSummary {
        private String employeeName;
        private BigDecimal grossSalary;
        private BigDecimal deductions;
        private BigDecimal netSalary;
        private String status;
        private String period;
    }
}
