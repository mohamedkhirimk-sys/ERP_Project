package com.erp.system.finance.service;

import com.erp.system.finance.dto.AccountRequest;
import com.erp.system.finance.dto.AccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AccountService {
    AccountResponse createAccount(AccountRequest request);
    AccountResponse getAccountById(Long id);
    Page<AccountResponse> getAllAccounts(Pageable pageable);
    AccountResponse updateAccount(Long id, AccountRequest request);
    void deleteAccount(Long id);
}
