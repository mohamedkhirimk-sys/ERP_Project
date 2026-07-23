package com.erp.system.finance.service;

import com.erp.system.finance.dto.PayrollRequest;
import com.erp.system.finance.dto.PayrollResponse;
import java.util.List;

public interface PayrollService {
    PayrollResponse createPayroll(PayrollRequest request);
    List<PayrollResponse> getAllPayrolls();
    List<PayrollResponse> getPayrollsByEmployee(String employeeId);
    PayrollResponse processPayment(Long id);
}
