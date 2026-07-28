package com.erp.system.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import com.erp.common.audit.Auditable;
import java.time.LocalDate;

@Entity
@Table(name = "leaves")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Leave extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private String reason;

    @Builder.Default
    private String status = "PENDING";

}
