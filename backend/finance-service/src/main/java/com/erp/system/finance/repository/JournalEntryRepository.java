package com.erp.system.finance.repository;

import com.erp.system.finance.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    List<JournalEntry> findByAccountId(Long accountId);

    Page<JournalEntry> findByAccountId(Long accountId, Pageable pageable);
}
