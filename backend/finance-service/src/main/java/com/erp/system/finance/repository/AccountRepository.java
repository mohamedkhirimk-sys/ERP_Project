package com.erp.system.finance.repository;

import com.erp.system.finance.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountCode(String accountCode);

    @Query("SELECT MAX(a.accountCode) FROM Account a WHERE a.accountCode LIKE '10%'")
    String findMaxCashCode();
}
