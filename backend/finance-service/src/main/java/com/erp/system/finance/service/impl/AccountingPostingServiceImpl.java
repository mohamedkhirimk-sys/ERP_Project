package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.AccountingPostingRequest;
import com.erp.system.finance.dto.JournalEntryLineResponse;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.JournalEntry;
import com.erp.system.finance.entity.JournalEntryLine;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.JournalEntryRepository;
import com.erp.system.finance.service.AccountingPostingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountingPostingServiceImpl implements AccountingPostingService {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.20");

    private final JournalEntryRepository journalEntryRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public JournalEntryResponse post(AccountingPostingRequest request) {
        PostingSource source = PostingSource.fromString(request.getSourceType());
        if (request.getTotalAmount().signum() <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        return switch (source) {
            case ORDER_CONFIRMED -> postOrderConfirmed(request);
            case PAYMENT_COMPLETED -> postPaymentCompleted(request);
            case GOODS_RECEIVED -> postGoodsReceived(request);
        };
    }

    private JournalEntryResponse postOrderConfirmed(AccountingPostingRequest request) {
        JournalEntry existing = findExisting(request);
        if (existing != null) {
            return toResponse(existing);
        }

        BigDecimal net = request.getTotalAmount();
        BigDecimal vat = net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = net.add(vat);

        String customer = request.getCustomerName() == null ? "" : request.getCustomerName() + " - ";
        return buildAndSave(JournalEntry.builder()
                        .sourceType(request.getSourceType())
                        .sourceId(request.getSourceId())
                        .journalCode("VTE")
                        .description("Vente " + customer + "Commande " + request.getSourceId())
                        .build(),
                List.of(
                        line(account("1100"), gross, BigDecimal.ZERO),
                        line(account("4000"), BigDecimal.ZERO, net),
                        line(account("2200"), BigDecimal.ZERO, vat)));
    }

    private JournalEntryResponse postPaymentCompleted(AccountingPostingRequest request) {
        JournalEntry existing = findExisting(request);
        if (existing != null) {
            return toResponse(existing);
        }

        BigDecimal amount = request.getTotalAmount();
        return buildAndSave(JournalEntry.builder()
                        .sourceType(request.getSourceType())
                        .sourceId(request.getSourceId())
                        .journalCode("ENC")
                        .description("Encaissement - " + request.getSourceId())
                        .build(),
                List.of(
                        line(account("1000"), amount, BigDecimal.ZERO),
                        line(account("1100"), BigDecimal.ZERO, amount)));
    }

    private JournalEntryResponse postGoodsReceived(AccountingPostingRequest request) {
        JournalEntry existing = findExisting(request);
        if (existing != null) {
            return toResponse(existing);
        }

        BigDecimal net = request.getTotalAmount();
        BigDecimal vat = net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = net.add(vat);

        return buildAndSave(JournalEntry.builder()
                        .sourceType(request.getSourceType())
                        .sourceId(request.getSourceId())
                        .journalCode("ACH")
                        .description("Achat - " + request.getSourceId())
                        .build(),
                List.of(
                        line(account("1200"), net, BigDecimal.ZERO),
                        line(account("2200"), vat, BigDecimal.ZERO),
                        line(account("2000"), BigDecimal.ZERO, gross)));
    }

    private JournalEntry findExisting(AccountingPostingRequest request) {
        return journalEntryRepository
                .findBySourceTypeAndSourceId(request.getSourceType(), request.getSourceId())
                .orElse(null);
    }

    private Account account(String accountCode) {
        return accountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountCode));
    }

    private JournalEntryLine line(Account account, BigDecimal debit, BigDecimal credit) {
        return JournalEntryLine.builder()
                .account(account)
                .debit(debit)
                .credit(credit)
                .build();
    }

    private JournalEntryResponse buildAndSave(JournalEntry entry, List<JournalEntryLine> lines) {
        lines.forEach(line -> {
            Account account = line.getAccount();
            account.setBalance(account.getBalance().add(line.getDebit().subtract(line.getCredit())));
            accountRepository.save(account);
            line.setJournalEntry(entry);
        });
        entry.setLines(lines);
        return toResponse(journalEntryRepository.save(entry));
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
