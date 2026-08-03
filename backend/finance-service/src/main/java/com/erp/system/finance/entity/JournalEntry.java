package com.erp.system.finance.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.erp.common.audit.Auditable;

@Entity
@Table(name = "journal_entries",
       uniqueConstraints = @UniqueConstraint(name = "uk_journal_entry_source", columnNames = {"sourceType", "sourceId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JournalEntry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String entryNumber;

    @Column(nullable = false)
    private String description;

    private String sourceType;

    private String sourceId;

    private String journalCode;

    private LocalDateTime entryDate;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (entryNumber == null) {
            entryNumber = "JE-" + System.currentTimeMillis();
        }
        if (entryDate == null) {
            entryDate = LocalDateTime.now();
        }
    }
}
