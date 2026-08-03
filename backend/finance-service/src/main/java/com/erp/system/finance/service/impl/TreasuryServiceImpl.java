package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.*;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.BankAccount;
import com.erp.system.finance.entity.CashMovement;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.entity.JournalEntryLine;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.BankAccountRepository;
import com.erp.system.finance.repository.CashMovementRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.TreasuryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TreasuryServiceImpl implements TreasuryService {

    private static final String EQUITY_CODE = "3000";
    private static final String CASH_CODE = "1000";

    private final BankAccountRepository bankAccountRepository;
    private final AccountRepository accountRepository;
    private final CashMovementRepository cashMovementRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Override
    @Transactional
    public CashMovementResponse transfer(TreasuryTransferRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (request.getFromBankAccountId().equals(request.getToBankAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same bank account");
        }
        BankAccount from = bank(request.getFromBankAccountId());
        BankAccount to = bank(request.getToBankAccountId());
        CashMovement movement = createMovement(CashMovement.MovementType.TRANSFER, from.getId(), to.getId(),
                request.getAmount(), request.getDescription());
        JournalEntry entry = postEntry("TREASURY_TRANSFER", movement, "BNQ",
                "Virement de " + from.getName() + " vers " + to.getName(),
                List.of(
                        line(to.getAccount(), request.getAmount(), BigDecimal.ZERO),
                        line(from.getAccount(), BigDecimal.ZERO, request.getAmount())));
        return finishMovement(movement, entry);
    }

    @Override
    @Transactional
    public CashMovementResponse expense(TreasuryExpenseRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Expense amount must be positive");
        }
        BankAccount bank = bank(request.getBankAccountId());
        Account expenseAccount = accountRepository.findById(request.getExpenseAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found: " + request.getExpenseAccountId()));
        CashMovement movement = createMovement(CashMovement.MovementType.EXPENSE, bank.getId(), null,
                request.getAmount(), request.getDescription());
        JournalEntry entry = postEntry("TREASURY_EXPENSE", movement, "DEC",
                "Dépense: " + (request.getDescription() == null ? "" : request.getDescription()),
                List.of(
                        line(expenseAccount, request.getAmount(), BigDecimal.ZERO),
                        line(bank.getAccount(), BigDecimal.ZERO, request.getAmount())));
        return finishMovement(movement, entry);
    }

    @Override
    @Transactional
    public CashMovementResponse deposit(TreasuryDepositRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        BankAccount bank = bank(request.getBankAccountId());
        CashMovement movement = createMovement(CashMovement.MovementType.DEPOSIT, bank.getId(), null,
                request.getAmount(), request.getDescription());
        JournalEntry entry = postEntry("TREASURY_DEPOSIT", movement, "BNQ",
                "Dépôt: " + (request.getDescription() == null ? "" : request.getDescription()),
                List.of(
                        line(bank.getAccount(), request.getAmount(), BigDecimal.ZERO),
                        line(equity(), BigDecimal.ZERO, request.getAmount())));
        return finishMovement(movement, entry);
    }

    @Override
    @Transactional
    public CashMovementResponse withdraw(TreasuryWithdrawalRequest request) {
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        BankAccount bank = bank(request.getBankAccountId());
        CashMovement movement = createMovement(CashMovement.MovementType.WITHDRAWAL, bank.getId(), null,
                request.getAmount(), request.getDescription());
        JournalEntry entry = postEntry("TREASURY_WITHDRAWAL", movement, "BNQ",
                "Retrait: " + (request.getDescription() == null ? "" : request.getDescription()),
                List.of(
                        line(equity(), request.getAmount(), BigDecimal.ZERO),
                        line(bank.getAccount(), BigDecimal.ZERO, request.getAmount())));
        return finishMovement(movement, entry);
    }

    @Override
    public TreasuryPositionResponse position() {
        List<BankAccount> banks = bankAccountRepository.findAll();
        List<BankBalanceResponse> balances = banks.stream()
                .map(bank -> {
                    Account account = accountRepository.findById(bank.getAccount().getId())
                            .orElseThrow(() -> new RuntimeException("Account not found: " + bank.getAccount().getId()));
                    return BankBalanceResponse.builder()
                            .bankId(bank.getId())
                            .bankName(bank.getName())
                            .accountNumber(bank.getAccountNumber())
                            .accountCode(account.getAccountCode())
                            .balance(account.getBalance())
                            .build();
                })
                .toList();
        BigDecimal total = balances.stream()
                .map(BankBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vaultBalance = vault().getBalance();
        return TreasuryPositionResponse.builder()
                .banks(balances)
                .vaultBalance(vaultBalance)
                .total(total.add(vaultBalance))
                .build();
    }

    @Override
    public List<CashMovementResponse> movements(Long bankAccountId) {
        List<CashMovement> movements = bankAccountId == null
                ? cashMovementRepository.findAllByOrderByCreatedAtDesc()
                : cashMovementRepository.findByBankAccountIdOrderByCreatedAtDesc(bankAccountId);
        return movements.stream().map(this::toResponse).toList();
    }

    private BankAccount bank(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + id));
    }

    private Account equity() {
        return accountRepository.findByAccountCode(EQUITY_CODE)
                .orElseThrow(() -> new RuntimeException("Account not found: " + EQUITY_CODE));
    }

    private Account vault() {
        return accountRepository.findByAccountCode(CASH_CODE)
                .orElseThrow(() -> new RuntimeException("Account not found: " + CASH_CODE));
    }

    private CashMovement createMovement(CashMovement.MovementType type, Long bankId, Long toBankId,
                                        BigDecimal amount, String description) {
        CashMovement movement = CashMovement.builder()
                .movementType(type)
                .bankAccountId(bankId)
                .toBankAccountId(toBankId)
                .amount(amount)
                .description(description)
                .build();
        return cashMovementRepository.save(movement);
    }

    private JournalEntry postEntry(String sourceType, CashMovement movement, String journalCode,
                                   String description, List<JournalEntryLine> lines) {
        JournalEntry existing = journalEntryRepository
                .findBySourceTypeAndSourceId(sourceType, String.valueOf(movement.getId()))
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        JournalEntry entry = JournalEntry.builder()
                .sourceType(sourceType)
                .sourceId(String.valueOf(movement.getId()))
                .journalCode(journalCode)
                .description(description)
                .build();
        lines.forEach(line -> {
            Account account = line.getAccount();
            account.setBalance(account.getBalance().add(line.getDebit().subtract(line.getCredit())));
            accountRepository.save(account);
            line.setJournalEntry(entry);
        });
        entry.setLines(lines);
        return journalEntryRepository.save(entry);
    }

    private CashMovementResponse finishMovement(CashMovement movement, JournalEntry entry) {
        movement.setEntryId(entry.getId());
        return toResponse(cashMovementRepository.save(movement));
    }

    private JournalEntryLine line(Account account, BigDecimal debit, BigDecimal credit) {
        return JournalEntryLine.builder()
                .account(account)
                .debit(debit)
                .credit(credit)
                .build();
    }

    private CashMovementResponse toResponse(CashMovement m) {
        return CashMovementResponse.builder()
                .id(m.getId())
                .movementType(m.getMovementType().name())
                .bankAccountId(m.getBankAccountId())
                .toBankAccountId(m.getToBankAccountId())
                .amount(m.getAmount())
                .description(m.getDescription())
                .entryId(m.getEntryId())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
