package com.erp.system.finance.service;

import com.erp.system.finance.dto.PayrollRequest;
import com.erp.system.finance.dto.PayrollResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PayrollService {
    PayrollResponse createPayroll(PayrollRequest request);
    Page<PayrollResponse> getAllPayrolls(Pageable pageable);
    Page<PayrollResponse> getPayrollsByEmployee(String employeeId, Pageable pageable);
    PayrollResponse processPayment(Long id);
}
