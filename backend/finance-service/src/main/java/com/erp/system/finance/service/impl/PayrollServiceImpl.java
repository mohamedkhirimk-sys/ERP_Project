package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.PayrollRequest;
import com.erp.system.finance.dto.PayrollResponse;
import com.erp.system.finance.entity.PayrollRecord;
import com.erp.system.finance.repository.PayrollRepository;
import com.erp.system.finance.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;

    @Override
    public PayrollResponse createPayroll(PayrollRequest request) {
        BigDecimal deductions = request.getDeductions() != null ? request.getDeductions() : BigDecimal.ZERO;
        PayrollRecord record = PayrollRecord.builder()
                .employeeId(request.getEmployeeId())
                .employeeName(request.getEmployeeName())
                .grossSalary(request.getGrossSalary())
                .deductions(deductions)
                .netSalary(request.getGrossSalary().subtract(deductions))
                .payPeriodStart(request.getPayPeriodStart())
                .payPeriodEnd(request.getPayPeriodEnd())
                .build();
        return toResponse(payrollRepository.save(record));
    }

    @Override
    public List<PayrollResponse> getAllPayrolls() {
        return payrollRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<PayrollResponse> getPayrollsByEmployee(String employeeId) {
        return payrollRepository.findByEmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    @Override
    public PayrollResponse processPayment(Long id) {
        PayrollRecord record = payrollRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payroll record not found: " + id));
        record.setStatus("PAID");
        record.setPaymentDate(LocalDate.now());
        return toResponse(payrollRepository.save(record));
    }

    private PayrollResponse toResponse(PayrollRecord r) {
        return PayrollResponse.builder()
                .id(r.getId())
                .employeeId(r.getEmployeeId())
                .employeeName(r.getEmployeeName())
                .grossSalary(r.getGrossSalary())
                .deductions(r.getDeductions())
                .netSalary(r.getNetSalary())
                .payPeriodStart(r.getPayPeriodStart())
                .payPeriodEnd(r.getPayPeriodEnd())
                .paymentDate(r.getPaymentDate())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
