package com.erp.system.finance.service.impl;

import com.erp.system.finance.dto.PayrollRequest;
import com.erp.system.finance.dto.PayrollResponse;
import com.erp.system.finance.entity.PayrollRecord;
import com.erp.system.finance.repository.PayrollRepository;
import com.erp.system.finance.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;

    @Override
    public PayrollResponse createPayroll(PayrollRequest request) {
        if (payrollRepository.existsByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(
                request.getEmployeeId(), request.getPayPeriodStart(), request.getPayPeriodEnd())) {
            throw new IllegalArgumentException(
                    "Payroll already exists for employee " + request.getEmployeeId()
                    + " in period " + request.getPayPeriodStart() + " to " + request.getPayPeriodEnd());
        }
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
    public Page<PayrollResponse> getAllPayrolls(Pageable pageable) {
        return payrollRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    public Page<PayrollResponse> getPayrollsByEmployee(String employeeId, Pageable pageable) {
        return payrollRepository.findByEmployeeId(employeeId, pageable).map(this::toResponse);
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
