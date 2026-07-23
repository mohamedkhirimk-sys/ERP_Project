package com.erp.system.finance.service;

import com.erp.system.finance.dto.JournalEntryRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import java.util.List;

public interface JournalEntryService {
    JournalEntryResponse createEntry(JournalEntryRequest request);
    List<JournalEntryResponse> getAllEntries();
    List<JournalEntryResponse> getEntriesByAccount(Long accountId);
}
