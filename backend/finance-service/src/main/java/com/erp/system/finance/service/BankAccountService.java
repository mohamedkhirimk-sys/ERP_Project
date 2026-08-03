package com.erp.system.finance.service;

import com.erp.system.finance.dto.BankAccountRequest;
import com.erp.system.finance.dto.BankAccountResponse;

import java.util.List;

public interface BankAccountService {
    BankAccountResponse create(BankAccountRequest request);

    List<BankAccountResponse> getAll();

    BankAccountResponse getById(Long id);
}
