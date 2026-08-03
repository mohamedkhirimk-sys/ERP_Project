package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.JournalDetailResponse;
import com.erp.system.finance.dto.JournalEntryLineResponse;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.dto.JournalSummaryResponse;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.JournalService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalEntryRepository journalEntryRepository;

    @Override
    @Transactional
    public List<JournalSummaryResponse> summary() {
        Map<String, List<JournalEntry>> byJournal = journalEntryRepository.findAll().stream()
                .filter(e -> e.getJournalCode() != null)
                .collect(Collectors.groupingBy(JournalEntry::getJournalCode));

        return Arrays.stream(JournalCode.values())
                .map(code -> {
                    List<JournalEntry> entries = byJournal.getOrDefault(code.name(), List.of());
                    return JournalSummaryResponse.builder()
                            .code(code.name())
                            .label(code.getLabel())
                            .entryCount(entries.size())
                            .totalDebit(totals(entries, true))
                            .totalCredit(totals(entries, false))
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public JournalDetailResponse detail(String code, LocalDate from, LocalDate to) {
        JournalCode journal = JournalCode.fromString(code);
        List<JournalEntry> entries = (from != null && to != null)
                ? journalEntryRepository.findByJournalCodeAndEntryDateBetween(
                        code, from.atStartOfDay(), to.atTime(23, 59, 59))
                : journalEntryRepository.findByJournalCode(code);

        return JournalDetailResponse.builder()
                .code(journal.name())
                .label(journal.getLabel())
                .from(from)
                .to(to)
                .entries(entries.stream().map(JournalServiceImpl::toResponse).toList())
                .totalDebit(totals(entries, true))
                .totalCredit(totals(entries, false))
                .build();
    }

    private static BigDecimal totals(List<JournalEntry> entries, boolean debit) {
        return entries.stream()
                .flatMap(e -> e.getLines().stream())
                .map(line -> debit ? line.getDebit() : line.getCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static JournalEntryResponse toResponse(JournalEntry e) {
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
                .journalCode(e.getJournalCode())
                .lines(lineResponses)
                .build();
    }
}
