package com.erp.system.finance.service;

import com.erp.system.finance.dto.AccountingPostingRequest;
import com.erp.system.finance.dto.JournalEntryResponse;

public interface AccountingPostingService {

    JournalEntryResponse post(AccountingPostingRequest request);
}
