package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.BankAccountRequest;
import com.erp.system.finance.dto.BankAccountResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.entity.BankAccount;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.repository.BankAccountRepository;
import com.erp.system.finance.service.BankAccountService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final AccountRepository accountRepository;
    private final BankAccountRepository bankAccountRepository;

    @Override
    @Transactional
    public BankAccountResponse create(BankAccountRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Bank name is required");
        }
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Bank account number is required");
        }
        if (bankAccountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new IllegalArgumentException("Bank account with number " + request.getAccountNumber() + " already exists");
        }

        String maxCode = accountRepository.findMaxCashCode();
        int nextCode = (maxCode == null ? 1000 : Integer.parseInt(maxCode)) + 10;
        Account chartAccount = Account.builder()
                .accountCode(String.valueOf(nextCode))
                .accountName(request.getName())
                .accountType("ASSET")
                .description("Cash account linked to bank " + request.getName())
                .build();
        accountRepository.save(chartAccount);

        BankAccount bank = BankAccount.builder()
                .name(request.getName())
                .accountNumber(request.getAccountNumber())
                .account(chartAccount)
                .build();
        return toResponse(bankAccountRepository.save(bank));
    }

    @Override
    public List<BankAccountResponse> getAll() {
        return bankAccountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public BankAccountResponse getById(Long id) {
        return toResponse(bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank account not found: " + id)));
    }

    private BankAccountResponse toResponse(BankAccount bank) {
        return BankAccountResponse.builder()
                .id(bank.getId())
                .name(bank.getName())
                .accountNumber(bank.getAccountNumber())
                .accountId(bank.getAccount().getId())
                .accountCode(bank.getAccount().getAccountCode())
                .build();
    }
}
