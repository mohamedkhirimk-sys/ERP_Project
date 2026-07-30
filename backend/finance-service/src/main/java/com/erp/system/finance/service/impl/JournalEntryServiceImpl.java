package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.*;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.entity.JournalEntryLine;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.JournalEntryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JournalEntryServiceImpl implements JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public JournalEntryResponse createEntry(JournalEntryRequest request) {
        List<JournalEntryLine> lines = request.getLines().stream()
                .map(lineReq -> {
                    Account account = accountRepository.findById(lineReq.getAccountId())
                            .orElseThrow(() -> new RuntimeException("Account not found: " + lineReq.getAccountId()));
                    account.setBalance(account.getBalance().add(lineReq.getDebit().subtract(lineReq.getCredit())));
                    accountRepository.save(account);

                    return JournalEntryLine.builder()
                            .account(account)
                            .debit(lineReq.getDebit())
                            .credit(lineReq.getCredit())
                            .build();
                })
                .toList();

        JournalEntry entry = JournalEntry.builder()
                .description(request.getDescription())
                .lines(lines)
                .build();

        lines.forEach(line -> line.setJournalEntry(entry));

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
        List<JournalEntryLineResponse> lineResponses = e.getLines().stream()
                .map(line -> JournalEntryLineResponse.builder()
                        .id(line.getId())
                        .accountId(line.getAccount().getId())
                        .accountName(line.getAccount().getAccountName())
                        .accountCode(line.getAccount().getAccountCode())
                        .debit(line.getDebit())
                        .credit(line.getCredit())
                        .build())
                .toList();

        return JournalEntryResponse.builder()
                .id(e.getId())
                .entryNumber(e.getEntryNumber())
                .description(e.getDescription())
                .entryDate(e.getEntryDate())
                .createdAt(e.getCreatedAt())
                .lines(lineResponses)
                .build();
    }
}
