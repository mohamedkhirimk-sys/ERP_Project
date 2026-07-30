package com.erp.system.finance.repository;

import com.erp.system.finance.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN je.lines l WHERE l.account.id = :accountId")
    List<JournalEntry> findByAccountId(Long accountId);

    @Query(value = "SELECT DISTINCT je FROM JournalEntry je JOIN je.lines l WHERE l.account.id = :accountId",
           countQuery = "SELECT COUNT(DISTINCT je) FROM JournalEntry je JOIN je.lines l WHERE l.account.id = :accountId")
    Page<JournalEntry> findByAccountId(Long accountId, Pageable pageable);
}
