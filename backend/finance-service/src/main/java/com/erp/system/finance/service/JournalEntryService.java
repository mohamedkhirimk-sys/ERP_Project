package com.erp.system.finance.service;

import com.erp.system.finance.dto.JournalEntryRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JournalEntryService {
    JournalEntryResponse createEntry(JournalEntryRequest request);
    Page<JournalEntryResponse> getAllEntries(Pageable pageable);
    Page<JournalEntryResponse> getEntriesByAccount(Long accountId, Pageable pageable);
}
