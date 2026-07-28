package com.erp.system.finance.repository;

import com.erp.system.finance.entity.PayrollRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PayrollRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByEmployeeId(String employeeId);

    Page<PayrollRecord> findByEmployeeId(String employeeId, Pageable pageable);

    boolean existsByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(
            String employeeId, LocalDate payPeriodStart, LocalDate payPeriodEnd);
}
