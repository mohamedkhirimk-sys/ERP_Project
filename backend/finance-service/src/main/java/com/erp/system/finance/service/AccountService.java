package com.erp.system.finance.service;

import com.erp.system.finance.dto.AccountRequest;
import com.erp.system.finance.dto.AccountResponse;
import java.util.List;

public interface AccountService {
    AccountResponse createAccount(AccountRequest request);
    AccountResponse getAccountById(Long id);
    List<AccountResponse> getAllAccounts();
    AccountResponse updateAccount(Long id, AccountRequest request);
    void deleteAccount(Long id);
}
