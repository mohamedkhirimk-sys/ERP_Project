package com.erp.system.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "journal_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String entryNumber;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private BigDecimal debit;

    @Column(nullable = false)
    private BigDecimal credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private LocalDateTime entryDate;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (entryNumber == null) {
            entryNumber = "JE-" + System.currentTimeMillis();
        }
        if (entryDate == null) {
            entryDate = LocalDateTime.now();
        }
    }
}
