package com.erp.system.finance.repository;

import com.erp.system.finance.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    boolean existsByAccountNumber(String accountNumber);
}
