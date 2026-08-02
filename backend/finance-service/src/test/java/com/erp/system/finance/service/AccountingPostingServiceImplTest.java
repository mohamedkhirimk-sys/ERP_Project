package com.erp.system.finance.service;

import com.erp.system.finance.dto.AccountingPostingRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.entity.JournalEntryLine;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.impl.AccountingPostingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class AccountingPostingServiceImplTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private AccountRepository accountRepository;

    private AccountingPostingService service;

    private Account cash;
    private Account receivable;
    private Account revenue;
    private Account taxPayable;

    @BeforeEach
    void setUp() {
        service = new AccountingPostingServiceImpl(journalEntryRepository, accountRepository);
        cash = account("1000", "Cash", "ASSET");
        receivable = account("1100", "Accounts Receivable", "ASSET");
        revenue = account("4000", "Revenue", "REVENUE");
        taxPayable = account("2200", "Tax Payable (TVA collectée)", "LIABILITY");
    }

    private Account account(String code, String name, String type) {
        return Account.builder()
                .accountCode(code)
                .accountName(name)
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .build();
    }

    private AccountingPostingRequest request(String sourceType, String sourceId, String customer, String amount) {
        return AccountingPostingRequest.builder()
                .sourceType(sourceType)
                .sourceId(sourceId)
                .customerName(customer)
                .totalAmount(new BigDecimal(amount))
                .build();
    }

    private JournalEntryLine line(JournalEntry entry, String accountCode) {
        return entry.getLines().stream()
                .filter(l -> l.getAccount().getAccountCode().equals(accountCode))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line for account " + accountCode));
    }

    @Test
    void postsBalancedSaleEntryForOrderConfirmed() {
        when(journalEntryRepository.findBySourceTypeAndSourceId("ORDER_CONFIRMED", "ORD-1"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("1100")).thenReturn(Optional.of(receivable));
        when(accountRepository.findByAccountCode("4000")).thenReturn(Optional.of(revenue));
        when(accountRepository.findByAccountCode("2200")).thenReturn(Optional.of(taxPayable));
        when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.post(request("ORDER_CONFIRMED", "ORD-1", "Test Co", "100.00"));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        JournalEntry entry = captor.getValue();

        assertThat(entry.getSourceType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(entry.getSourceId()).isEqualTo("ORD-1");
        assertThat(entry.getJournalCode()).isEqualTo("VTE");
        assertThat(entry.getDescription()).contains("ORD-1");
        assertThat(entry.getLines()).hasSize(3);

        JournalEntryLine arLine = line(entry, "1100");
        assertThat(arLine.getDebit()).isEqualByComparingTo("120.00");
        assertThat(arLine.getCredit()).isZero();

        JournalEntryLine revenueLine = line(entry, "4000");
        assertThat(revenueLine.getCredit()).isEqualByComparingTo("100.00");
        assertThat(revenueLine.getDebit()).isZero();

        JournalEntryLine taxLine = line(entry, "2200");
        assertThat(taxLine.getCredit()).isEqualByComparingTo("20.00");
        assertThat(taxLine.getDebit()).isZero();

        BigDecimal totalDebits = entry.getLines().stream()
                .map(JournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entry.getLines().stream()
                .map(JournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebits).isEqualByComparingTo(totalCredits);

        assertThat(receivable.getBalance()).isEqualByComparingTo("120.00");
        assertThat(revenue.getBalance()).isEqualByComparingTo("-100.00");
        assertThat(taxPayable.getBalance()).isEqualByComparingTo("-20.00");
    }

    @Test
    void postsBalancedReceiptEntryForPaymentCompleted() {
        when(journalEntryRepository.findBySourceTypeAndSourceId("PAYMENT_COMPLETED", "ORD-2"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("1000")).thenReturn(Optional.of(cash));
        when(accountRepository.findByAccountCode("1100")).thenReturn(Optional.of(receivable));
        when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.post(request("PAYMENT_COMPLETED", "ORD-2", null, "100.00"));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        JournalEntry entry = captor.getValue();

        assertThat(entry.getJournalCode()).isEqualTo("ENC");
        assertThat(entry.getDescription()).contains("ORD-2");
        assertThat(entry.getLines()).hasSize(2);

        JournalEntryLine cashLine = line(entry, "1000");
        assertThat(cashLine.getDebit()).isEqualByComparingTo("100.00");
        assertThat(cashLine.getCredit()).isZero();

        JournalEntryLine arLine = line(entry, "1100");
        assertThat(arLine.getCredit()).isEqualByComparingTo("100.00");
        assertThat(arLine.getDebit()).isZero();

        assertThat(cash.getBalance()).isEqualByComparingTo("100.00");
        assertThat(receivable.getBalance()).isEqualByComparingTo("-100.00");
    }

    @Test
    void replaysExistingPostingWithoutDuplicating() {
        JournalEntry existing = JournalEntry.builder()
                .id(42L)
                .entryNumber("JE-1")
                .description("Vente Test Co - Commande ORD-1")
                .sourceType("ORDER_CONFIRMED")
                .sourceId("ORD-1")
                .lines(new ArrayList<>())
                .build();
        when(journalEntryRepository.findBySourceTypeAndSourceId("ORDER_CONFIRMED", "ORD-1"))
                .thenReturn(Optional.of(existing));

        JournalEntryResponse response = service.post(request("ORDER_CONFIRMED", "ORD-1", "Test Co", "100.00"));

        assertThat(response.getId()).isEqualTo(42L);
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownSourceType() {
        assertThatThrownBy(() -> service.post(request("REFUND_ISSUED", "X-1", null, "10.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("REFUND_ISSUED");
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void roundsVatToTwoDecimals() {
        when(journalEntryRepository.findBySourceTypeAndSourceId("ORDER_CONFIRMED", "ORD-3"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("1100")).thenReturn(Optional.of(receivable));
        when(accountRepository.findByAccountCode("4000")).thenReturn(Optional.of(revenue));
        when(accountRepository.findByAccountCode("2200")).thenReturn(Optional.of(taxPayable));
        when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.post(request("ORDER_CONFIRMED", "ORD-3", null, "1139.95"));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        JournalEntry entry = captor.getValue();

        JournalEntryLine arLine = line(entry, "1100");
        JournalEntryLine taxLine = line(entry, "2200");
        assertThat(arLine.getDebit()).isEqualByComparingTo("1367.94");
        assertThat(taxLine.getCredit()).isEqualByComparingTo("227.99");

        BigDecimal totalDebits = entry.getLines().stream()
                .map(JournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entry.getLines().stream()
                .map(JournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebits).isEqualByComparingTo(totalCredits);
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.post(request("PAYMENT_COMPLETED", "ORD-4", null, "0.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
        verify(journalEntryRepository, never()).save(any());
    }

    @Test
    void postsBalancedPurchaseEntryForGoodsReceived() {
        Account inventory = account("1200", "Inventory", "ASSET");
        Account payable = account("2000", "Accounts Payable", "LIABILITY");
        when(journalEntryRepository.findBySourceTypeAndSourceId("GOODS_RECEIVED", "PO-1"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("1200")).thenReturn(Optional.of(inventory));
        when(accountRepository.findByAccountCode("2200")).thenReturn(Optional.of(taxPayable));
        when(accountRepository.findByAccountCode("2000")).thenReturn(Optional.of(payable));
        when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.post(request("GOODS_RECEIVED", "PO-1", null, "100.00"));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        JournalEntry entry = captor.getValue();

        assertThat(entry.getSourceType()).isEqualTo("GOODS_RECEIVED");
        assertThat(entry.getSourceId()).isEqualTo("PO-1");
        assertThat(entry.getJournalCode()).isEqualTo("ACH");
        assertThat(entry.getDescription()).contains("PO-1");
        assertThat(entry.getLines()).hasSize(3);

        JournalEntryLine invLine = line(entry, "1200");
        assertThat(invLine.getDebit()).isEqualByComparingTo("100.00");
        assertThat(invLine.getCredit()).isZero();

        JournalEntryLine taxLine = line(entry, "2200");
        assertThat(taxLine.getDebit()).isEqualByComparingTo("20.00");
        assertThat(taxLine.getCredit()).isZero();

        JournalEntryLine payableLine = line(entry, "2000");
        assertThat(payableLine.getCredit()).isEqualByComparingTo("120.00");
        assertThat(payableLine.getDebit()).isZero();

        BigDecimal totalDebits = entry.getLines().stream()
                .map(JournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entry.getLines().stream()
                .map(JournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebits).isEqualByComparingTo(totalCredits);

        assertThat(inventory.getBalance()).isEqualByComparingTo("100.00");
        assertThat(taxPayable.getBalance()).isEqualByComparingTo("20.00");
        assertThat(payable.getBalance()).isEqualByComparingTo("-120.00");
    }
}
