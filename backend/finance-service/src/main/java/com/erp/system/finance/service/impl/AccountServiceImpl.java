package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.AccountRequest;
import com.erp.system.finance.dto.AccountResponse;
import com.erp.system.finance.entity.Account;
import com.erp.system.finance.repository.AccountRepository;
import com.erp.system.finance.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    public AccountResponse createAccount(AccountRequest request) {
        if (accountRepository.findByAccountCode(request.getAccountCode()).isPresent()) {
            throw new RuntimeException("Account code already exists: " + request.getAccountCode());
        }
        Account account = Account.builder()
                .accountCode(request.getAccountCode())
                .accountName(request.getAccountName())
                .accountType(request.getAccountType())
                .description(request.getDescription())
                .build();
        return toResponse(accountRepository.save(account));
    }

    @Override
    public AccountResponse getAccountById(Long id) {
        return toResponse(accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id)));
    }

    @Override
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public AccountResponse updateAccount(Long id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        account.setAccountCode(request.getAccountCode());
        account.setAccountName(request.getAccountName());
        account.setAccountType(request.getAccountType());
        account.setDescription(request.getDescription());
        return toResponse(accountRepository.save(account));
    }

    @Override
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found: " + id);
        }
        accountRepository.deleteById(id);
    }

    private AccountResponse toResponse(Account a) {
        return AccountResponse.builder()
                .id(a.getId())
                .accountCode(a.getAccountCode())
                .accountName(a.getAccountName())
                .accountType(a.getAccountType())
                .description(a.getDescription())
                .balance(a.getBalance())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
