package com.erp.system.finance.service;

import com.erp.system.finance.dto.JournalEntryLineRequest;
import com.erp.system.finance.dto.JournalEntryRequest;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.impl.JournalEntryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceImplTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private AccountRepository accountRepository;

    private JournalEntryService service;

    private Account cash;
    private Account equity;

    @BeforeEach
    void setUp() {
        service = new JournalEntryServiceImpl(journalEntryRepository, accountRepository);
        cash = Account.builder().id(1L).accountCode("1000").accountName("Cash")
                .accountType("ASSET").balance(BigDecimal.ZERO).build();
        equity = Account.builder().id(2L).accountCode("3000").accountName("Owner's Equity")
                .accountType("EQUITY").balance(BigDecimal.ZERO).build();
        lenient().when(accountRepository.findById(1L)).thenReturn(Optional.of(cash));
        lenient().when(accountRepository.findById(2L)).thenReturn(Optional.of(equity));
        lenient().when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private JournalEntryRequest request(String journalCode) {
        return JournalEntryRequest.builder()
                .description("Dépôt initial")
                .journalCode(journalCode)
                .lines(List.of(
                        JournalEntryLineRequest.builder().accountId(1L).debit(new BigDecimal("500.00")).credit(BigDecimal.ZERO).build(),
                        JournalEntryLineRequest.builder().accountId(2L).debit(BigDecimal.ZERO).credit(new BigDecimal("500.00")).build()))
                .build();
    }

    @Test
    void defaultsManualEntryToGeneralJournal() {
        service.createEntry(request(null));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getJournalCode()).isEqualTo("OD");
    }

    @Test
    void honorsExplicitJournalCode() {
        service.createEntry(request("BNQ"));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getJournalCode()).isEqualTo("BNQ");
    }

    @Test
    void rejectsUnknownJournalCode() {
        assertThatThrownBy(() -> service.createEntry(request("XXX")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("XXX");
        verify(journalEntryRepository, org.mockito.Mockito.never()).save(any());
    }
}
