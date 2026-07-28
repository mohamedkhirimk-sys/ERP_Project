package com.erp.system.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import com.erp.common.audit.Auditable;

@Entity
@Table(name = "accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String accountCode;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private String accountType;

    private String description;

    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

}
