package com.erp.system.finance.repository;

import com.erp.system.finance.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Optional<JournalEntry> findBySourceTypeAndSourceId(String sourceType, String sourceId);

    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN FETCH je.lines WHERE je.journalCode = :code ORDER BY je.entryDate ASC")
    List<JournalEntry> findByJournalCode(String code);

    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN FETCH je.lines WHERE je.journalCode = :code AND je.entryDate BETWEEN :from AND :to ORDER BY je.entryDate ASC")
    List<JournalEntry> findByJournalCodeAndEntryDateBetween(String code, LocalDateTime from, LocalDateTime to);

    @Query("SELECT DISTINCT je FROM JournalEntry je JOIN je.lines l WHERE l.account.id = :accountId")
    List<JournalEntry> findByAccountId(Long accountId);

    @Query(value = "SELECT DISTINCT je FROM JournalEntry je JOIN je.lines l WHERE l.account.id = :accountId",
           countQuery = "SELECT COUNT(DISTINCT je) FROM JournalEntry je JOIN je.lines l WHERE l.account.id = :accountId")
    Page<JournalEntry> findByAccountId(Long accountId, Pageable pageable);
}
