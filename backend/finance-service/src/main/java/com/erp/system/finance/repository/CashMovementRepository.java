package com.erp.system.finance.repository;

import com.erp.system.finance.entity.CashMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashMovementRepository extends JpaRepository<CashMovement, Long> {
    List<CashMovement> findAllByOrderByCreatedAtDesc();

    List<CashMovement> findByBankAccountIdOrderByCreatedAtDesc(Long bankAccountId);
}
