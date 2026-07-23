package com.erp.system.finance.controller;

import com.erp.system.finance.dto.PayrollRequest;
import com.erp.system.finance.dto.PayrollResponse;
import com.erp.system.finance.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    public ResponseEntity<PayrollResponse> createPayroll(@Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayroll(request));
    }

    @GetMapping
    public ResponseEntity<List<PayrollResponse>> getAllPayrolls() {
        return ResponseEntity.ok(payrollService.getAllPayrolls());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayrollResponse>> getPayrollsByEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<PayrollResponse> processPayment(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.processPayment(id));
    }
}
