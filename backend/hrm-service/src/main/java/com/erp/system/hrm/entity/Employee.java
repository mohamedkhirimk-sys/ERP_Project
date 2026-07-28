package com.erp.system.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import com.erp.common.audit.Auditable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String department;
    private String position;

    @Column(nullable = false)
    private BigDecimal salary;

    private LocalDate hireDate;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @PrePersist
    protected void onCreate() {
        if (employeeId == null) {
            employeeId = "EMP-" + System.currentTimeMillis();
        }
    }
}
