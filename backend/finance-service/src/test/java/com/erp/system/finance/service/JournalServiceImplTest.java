package com.erp.system.finance.service;

import com.erp.system.finance.dto.JournalDetailResponse;
import com.erp.system.finance.dto.JournalSummaryResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.entity.JournalEntryLine;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.impl.JournalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalServiceImplTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    private JournalService service;

    private Account cash;
    private Account receivable;

    @BeforeEach
    void setUp() {
        service = new JournalServiceImpl(journalEntryRepository);
        cash = Account.builder().id(1L).accountCode("1000").accountName("Cash").accountType("ASSET").build();
        receivable = Account.builder().id(2L).accountCode("1100").accountName("Accounts Receivable").accountType("ASSET").build();
    }

    private JournalEntry entry(String code, String number, LocalDateTime date, Account account, String debit, String credit) {
        JournalEntry e = JournalEntry.builder()
                .id(1L)
                .entryNumber(number)
                .description("Libellé " + number)
                .entryDate(date)
                .journalCode(code)
                .build();
        e.setLines(List.of(JournalEntryLine.builder()
                .account(account)
                .debit(new BigDecimal(debit))
                .credit(new BigDecimal(credit))
                .build()));
        return e;
    }

    @Test
    void summaryListsAllJournalsWithTotals() {
        when(journalEntryRepository.findAll()).thenReturn(List.of(
                entry("VTE", "JE-1", LocalDateTime.of(2026, 8, 2, 10, 0), receivable, "120.00", "0.00"),
                entry("VTE", "JE-2", LocalDateTime.of(2026, 8, 2, 11, 0), receivable, "60.00", "0.00"),
                entry("ENC", "JE-3", LocalDateTime.of(2026, 8, 2, 12, 0), cash, "100.00", "0.00")));

        List<JournalSummaryResponse> summary = service.summary();

        assertThat(summary).hasSize(6);
        JournalSummaryResponse vte = summary.stream().filter(s -> s.getCode().equals("VTE")).findFirst().orElseThrow();
        assertThat(vte.getLabel()).isEqualTo("Journal des ventes");
        assertThat(vte.getEntryCount()).isEqualTo(2);
        assertThat(vte.getTotalDebit()).isEqualByComparingTo("180.00");
        assertThat(vte.getTotalCredit()).isZero();
        JournalSummaryResponse enc = summary.stream().filter(s -> s.getCode().equals("ENC")).findFirst().orElseThrow();
        assertThat(enc.getEntryCount()).isEqualTo(1);
        assertThat(enc.getTotalDebit()).isEqualByComparingTo("100.00");
        JournalSummaryResponse od = summary.stream().filter(s -> s.getCode().equals("OD")).findFirst().orElseThrow();
        assertThat(od.getEntryCount()).isZero();
    }

    @Test
    void detailReturnsEntriesOfJournalWithTotals() {
        when(journalEntryRepository.findByJournalCode("VTE")).thenReturn(List.of(
                entry("VTE", "JE-1", LocalDateTime.of(2026, 8, 2, 10, 0), receivable, "120.00", "0.00"),
                entry("VTE", "JE-2", LocalDateTime.of(2026, 8, 2, 11, 0), receivable, "60.00", "0.00")));

        JournalDetailResponse detail = service.detail("VTE", null, null);

        assertThat(detail.getCode()).isEqualTo("VTE");
        assertThat(detail.getEntries()).hasSize(2);
        assertThat(detail.getEntries().get(0).getEntryNumber()).isEqualTo("JE-1");
        assertThat(detail.getEntries().get(0).getLines()).hasSize(1);
        assertThat(detail.getTotalDebit()).isEqualByComparingTo("180.00");
        assertThat(detail.getTotalCredit()).isZero();
    }

    @Test
    void detailFiltersByDateRange() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 15);
        when(journalEntryRepository.findByJournalCodeAndEntryDateBetween(eq("ENC"), eq(from.atStartOfDay()), eq(to.atTime(23, 59, 59))))
                .thenReturn(List.of(
                        entry("ENC", "JE-3", LocalDateTime.of(2026, 8, 2, 12, 0), cash, "100.00", "0.00")));

        JournalDetailResponse detail = service.detail("ENC", from, to);

        assertThat(detail.getEntries()).hasSize(1);
        assertThat(detail.getTotalDebit()).isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsUnknownJournalCodeInDetail() {
        assertThatThrownBy(() -> service.detail("XXX", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XXX");
    }
}
