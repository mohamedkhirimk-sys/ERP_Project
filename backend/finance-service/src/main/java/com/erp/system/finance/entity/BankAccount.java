package com.erp.system.finance.entity;

import com.erp.common.audit.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bank_accounts",
       uniqueConstraints = @UniqueConstraint(name = "uk_bank_account_number", columnNames = {"accountNumber"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccount extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String accountNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
}
