package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.JournalEntryRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.JournalEntryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class JournalEntryServiceImpl implements JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public JournalEntryResponse createEntry(JournalEntryRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getAccountId()));

        BigDecimal balanceChange = request.getDebit().subtract(request.getCredit());
        account.setBalance(account.getBalance().add(balanceChange));
        accountRepository.save(account);

        JournalEntry entry = JournalEntry.builder()
                .account(account)
                .description(request.getDescription())
                .debit(request.getDebit())
                .credit(request.getCredit())
                .build();
        return toResponse(journalEntryRepository.save(entry));
    }

    @Override
    public Page<JournalEntryResponse> getAllEntries(Pageable pageable) {
        return journalEntryRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public Page<JournalEntryResponse> getEntriesByAccount(Long accountId, Pageable pageable) {
        return journalEntryRepository.findByAccountId(accountId, pageable).map(this::toResponse);
    }

    private JournalEntryResponse toResponse(JournalEntry e) {
        return JournalEntryResponse.builder()
                .id(e.getId())
                .entryNumber(e.getEntryNumber())
                .accountId(e.getAccount().getId())
                .accountName(e.getAccount().getAccountName())
                .accountCode(e.getAccount().getAccountCode())
                .description(e.getDescription())
                .debit(e.getDebit())
                .credit(e.getCredit())
                .entryDate(e.getEntryDate())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
