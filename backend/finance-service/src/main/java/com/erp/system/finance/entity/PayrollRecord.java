package com.erp.system.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import com.erp.common.audit.Auditable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payroll_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PayrollRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeId;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private BigDecimal grossSalary;

    private BigDecimal deductions;

    @Builder.Default
    private BigDecimal netSalary = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate payPeriodStart;

    @Column(nullable = false)
    private LocalDate payPeriodEnd;

    private LocalDate paymentDate;

    @Builder.Default
    private String status = "PENDING";

    @PrePersist
    protected void onCreate() {
        if (netSalary == null || netSalary.compareTo(BigDecimal.ZERO) == 0) {
            netSalary = grossSalary.subtract(deductions != null ? deductions : BigDecimal.ZERO);
        }
    }
}
