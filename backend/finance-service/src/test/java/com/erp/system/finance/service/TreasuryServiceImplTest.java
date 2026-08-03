package com.erp.system.finance.service;

import com.erp.system.finance.dto.CashMovementResponse;
import com.erp.system.finance.dto.TreasuryDepositRequest;
import com.erp.system.finance.dto.TreasuryExpenseRequest;
import com.erp.system.finance.dto.TreasuryPositionResponse;
import com.erp.system.finance.dto.TreasuryTransferRequest;
import com.erp.system.finance.dto.TreasuryWithdrawalRequest;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.BankAccount;
import com.erp.system.finance.entity.CashMovement;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.entity.JournalEntryLine;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.BankAccountRepository;
import com.erp.system.finance.repository.CashMovementRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.impl.TreasuryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreasuryServiceImplTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CashMovementRepository cashMovementRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    private TreasuryService service;

    @BeforeEach
    void setUp() {
        service = new TreasuryServiceImpl(
                bankAccountRepository, accountRepository, cashMovementRepository, journalEntryRepository);
    }

    private BankAccount bank(long id, String name, String number, String accountCode) {
        Account account = Account.builder()
                .id(id + 10L)
                .accountCode(accountCode)
                .accountName("Banque " + name)
                .accountType("ASSET")
                .balance(BigDecimal.ZERO)
                .build();
        return BankAccount.builder().id(id).name(name).accountNumber(number).account(account).build();
    }

    private void stubMovementSave() {
        when(cashMovementRepository.save(any(CashMovement.class))).thenAnswer(inv -> {
            CashMovement saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });
    }

    private void stubEntrySave() {
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(inv -> {
            JournalEntry saved = inv.getArgument(0);
            saved.setId(7L);
            return saved;
        });
    }

    private void stubNoExistingEntry() {
        when(journalEntryRepository.findBySourceTypeAndSourceId(any(), any())).thenReturn(Optional.empty());
    }

    private JournalEntryLine lineOf(JournalEntry entry, String accountCode) {
        return entry.getLines().stream()
                .filter(line -> line.getAccount().getAccountCode().equals(accountCode))
                .findFirst().orElseThrow();
    }

    @Test
    void transferPostsEntryDebitToCreditFrom() {
        stubMovementSave();
        stubEntrySave();
        stubNoExistingEntry();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank(1L, "BNP", "FR30001", "1010")));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.of(bank(2L, "CIC", "FR30002", "1020")));

        CashMovementResponse response = service.transfer(TreasuryTransferRequest.builder()
                .fromBankAccountId(1L).toBankAccountId(2L).amount(new BigDecimal("100.00"))
                .description("Virement BNP vers CIC").build());

        assertThat(response.getMovementType()).isEqualTo("TRANSFER");
        assertThat(response.getBankAccountId()).isEqualTo(1L);
        assertThat(response.getToBankAccountId()).isEqualTo(2L);
        assertThat(response.getEntryId()).isEqualTo(7L);

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();
        assertThat(entry.getJournalCode()).isEqualTo("BNQ");
        assertThat(entry.getSourceType()).isEqualTo("TREASURY_TRANSFER");
        assertThat(entry.getSourceId()).isEqualTo("5");
        assertThat(entry.getDescription()).contains("Virement");
        assertThat(entry.getLines()).hasSize(2);
        assertThat(lineOf(entry, "1020").getDebit()).isEqualByComparingTo("100.00");
        assertThat(lineOf(entry, "1020").getCredit()).isZero();
        assertThat(lineOf(entry, "1010").getCredit()).isEqualByComparingTo("100.00");
        assertThat(lineOf(entry, "1010").getDebit()).isZero();

        ArgumentCaptor<CashMovement> movementCaptor = ArgumentCaptor.forClass(CashMovement.class);
        verify(cashMovementRepository, org.mockito.Mockito.times(2)).save(movementCaptor.capture());
        assertThat(movementCaptor.getValue().getEntryId()).isEqualTo(7L);
    }

    @Test
    void transferRejectsSameBank() {
        assertThatThrownBy(() -> service.transfer(TreasuryTransferRequest.builder()
                .fromBankAccountId(1L).toBankAccountId(1L).amount(new BigDecimal("100.00")).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same bank");
    }

    @Test
    void transferRejectsUnknownBank() {
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(TreasuryTransferRequest.builder()
                .fromBankAccountId(1L).toBankAccountId(2L).amount(new BigDecimal("100.00")).build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bank account not found");
    }

    @Test
    void transferRejectsNonPositiveAmount() {
        assertThatThrownBy(() -> service.transfer(TreasuryTransferRequest.builder()
                .fromBankAccountId(1L).toBankAccountId(2L).amount(BigDecimal.ZERO).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void expensePostsDebitExpenseCreditBank() {
        stubMovementSave();
        stubEntrySave();
        stubNoExistingEntry();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank(1L, "BNP", "FR30001", "1010")));
        Account expense = Account.builder().id(88L).accountCode("5000").accountName("COGS")
                .accountType("EXPENSE").balance(BigDecimal.ZERO).build();
        when(accountRepository.findById(88L)).thenReturn(Optional.of(expense));

        CashMovementResponse response = service.expense(TreasuryExpenseRequest.builder()
                .bankAccountId(1L).expenseAccountId(88L).amount(new BigDecimal("50.00"))
                .description("Achat fournitures").build());

        assertThat(response.getMovementType()).isEqualTo("EXPENSE");
        assertThat(response.getEntryId()).isEqualTo(7L);

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();
        assertThat(entry.getJournalCode()).isEqualTo("DEC");
        assertThat(entry.getSourceType()).isEqualTo("TREASURY_EXPENSE");
        assertThat(entry.getLines()).hasSize(2);
        assertThat(lineOf(entry, "5000").getDebit()).isEqualByComparingTo("50.00");
        assertThat(lineOf(entry, "1010").getCredit()).isEqualByComparingTo("50.00");
        assertThat(expense.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void expenseRejectsUnknownExpenseAccount() {
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank(1L, "BNP", "FR30001", "1010")));
        when(accountRepository.findById(88L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expense(TreasuryExpenseRequest.builder()
                .bankAccountId(1L).expenseAccountId(88L).amount(new BigDecimal("50.00")).build()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account not found: 88");
    }

    @Test
    void depositPostsDebitBankCreditEquity() {
        stubMovementSave();
        stubEntrySave();
        stubNoExistingEntry();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank(1L, "BNP", "FR30001", "1010")));
        Account equity = Account.builder().id(99L).accountCode("3000").accountName("Owner's Equity")
                .accountType("EQUITY").balance(BigDecimal.ZERO).build();
        when(accountRepository.findByAccountCode("3000")).thenReturn(Optional.of(equity));

        CashMovementResponse response = service.deposit(TreasuryDepositRequest.builder()
                .bankAccountId(1L).amount(new BigDecimal("500.00")).description("Apport en banque").build());

        assertThat(response.getMovementType()).isEqualTo("DEPOSIT");

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();
        assertThat(entry.getJournalCode()).isEqualTo("BNQ");
        assertThat(entry.getSourceType()).isEqualTo("TREASURY_DEPOSIT");
        assertThat(entry.getLines()).hasSize(2);
        assertThat(lineOf(entry, "1010").getDebit()).isEqualByComparingTo("500.00");
        assertThat(lineOf(entry, "3000").getCredit()).isEqualByComparingTo("500.00");
        assertThat(equity.getBalance()).isEqualByComparingTo("-500.00");
    }

    @Test
    void withdrawalPostsDebitEquityCreditBank() {
        stubMovementSave();
        stubEntrySave();
        stubNoExistingEntry();
        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bank(1L, "BNP", "FR30001", "1010")));
        Account equity = Account.builder().id(99L).accountCode("3000").accountName("Owner's Equity")
                .accountType("EQUITY").balance(BigDecimal.ZERO).build();
        when(accountRepository.findByAccountCode("3000")).thenReturn(Optional.of(equity));

        CashMovementResponse response = service.withdraw(TreasuryWithdrawalRequest.builder()
                .bankAccountId(1L).amount(new BigDecimal("30.00")).description("Retrait especes").build());

        assertThat(response.getMovementType()).isEqualTo("WITHDRAWAL");

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();
        assertThat(entry.getJournalCode()).isEqualTo("BNQ");
        assertThat(entry.getSourceType()).isEqualTo("TREASURY_WITHDRAWAL");
        assertThat(entry.getLines()).hasSize(2);
        assertThat(lineOf(entry, "3000").getDebit()).isEqualByComparingTo("30.00");
        assertThat(lineOf(entry, "1010").getCredit()).isEqualByComparingTo("30.00");
    }

    @Test
    void positionReturnsBalancesPerBankVaultAndTotal() {
        when(bankAccountRepository.findAll()).thenReturn(List.of(
                bank(1L, "BNP", "FR30001", "1010"),
                bank(2L, "CIC", "FR30002", "1020")));
        when(accountRepository.findById(11L)).thenReturn(Optional.of(Account.builder()
                .id(11L).accountCode("1010").accountName("BNP").accountType("ASSET")
                .balance(new BigDecimal("100.00")).build()));
        when(accountRepository.findById(12L)).thenReturn(Optional.of(Account.builder()
                .id(12L).accountCode("1020").accountName("CIC").accountType("ASSET")
                .balance(new BigDecimal("50.00")).build()));
        when(accountRepository.findByAccountCode("1000")).thenReturn(Optional.of(Account.builder()
                .id(10L).accountCode("1000").accountName("Cash").accountType("ASSET")
                .balance(new BigDecimal("40.00")).build()));

        TreasuryPositionResponse position = service.position();

        assertThat(position.getBanks()).hasSize(2);
        assertThat(position.getBanks().get(0).getAccountCode()).isEqualTo("1010");
        assertThat(position.getBanks().get(0).getBalance()).isEqualByComparingTo("100.00");
        assertThat(position.getBanks().get(1).getBalance()).isEqualByComparingTo("50.00");
        assertThat(position.getVaultBalance()).isEqualByComparingTo("40.00");
        assertThat(position.getTotal()).isEqualByComparingTo("190.00");
    }

    @Test
    void movementsReturnsAllMovements() {
        CashMovement deposit = CashMovement.builder().id(1L).movementType(CashMovement.MovementType.DEPOSIT)
                .bankAccountId(1L).amount(new BigDecimal("500.00")).description("Apport")
                .entryId(7L).build();
        deposit.setCreatedAt(LocalDateTime.of(2026, 8, 2, 12, 0));
        CashMovement transfer = CashMovement.builder().id(2L).movementType(CashMovement.MovementType.TRANSFER)
                .bankAccountId(1L).toBankAccountId(2L).amount(new BigDecimal("100.00"))
                .description("Virement").entryId(8L).build();
        transfer.setCreatedAt(LocalDateTime.of(2026, 8, 2, 11, 0));
        when(cashMovementRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(deposit, transfer));

        List<CashMovementResponse> movements = service.movements(null);

        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).getMovementType()).isEqualTo("DEPOSIT");
        assertThat(movements.get(0).getEntryId()).isEqualTo(7L);
        assertThat(movements.get(1).getToBankAccountId()).isEqualTo(2L);
    }

    @Test
    void movementsFiltersByBankAccount() {
        when(cashMovementRepository.findByBankAccountIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(
                CashMovement.builder().id(2L).movementType(CashMovement.MovementType.TRANSFER)
                        .bankAccountId(1L).amount(new BigDecimal("100.00")).entryId(8L).build()));

        List<CashMovementResponse> movements = service.movements(1L);

        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getBankAccountId()).isEqualTo(1L);
    }
}
