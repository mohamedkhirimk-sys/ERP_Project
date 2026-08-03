package com.erp.system.finance.service;

import com.erp.system.finance.dto.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TreasuryService {
    @Transactional
    CashMovementResponse transfer(TreasuryTransferRequest request);

    @Transactional
    CashMovementResponse expense(TreasuryExpenseRequest request);

    @Transactional
    CashMovementResponse deposit(TreasuryDepositRequest request);

    @Transactional
    CashMovementResponse withdraw(TreasuryWithdrawalRequest request);

    TreasuryPositionResponse position();

    List<CashMovementResponse> movements(Long bankAccountId);
}
