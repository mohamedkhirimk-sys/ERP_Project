package com.erp.system.finance.entity;

import com.erp.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cash_movements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CashMovement extends Auditable {

    public enum MovementType {
        TRANSFER, EXPENSE, DEPOSIT, WITHDRAWAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType movementType;

    private Long bankAccountId;

    private Long toBankAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    private Long entryId;
}
