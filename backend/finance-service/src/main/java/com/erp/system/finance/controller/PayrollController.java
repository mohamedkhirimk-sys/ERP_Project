package com.erp.system.finance.controller;

import com.erp.system.finance.dto.PayrollRequest;
import com.erp.system.finance.dto.PayrollResponse;
import com.erp.system.finance.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayrollResponse> createPayroll(@Valid @RequestBody PayrollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.createPayroll(request));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PayrollResponse>> getAllPayrolls(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(payrollService.getAllPayrolls(pageable));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PayrollResponse>> getPayrollsByEmployee(
            @PathVariable String employeeId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(payrollService.getPayrollsByEmployee(employeeId, pageable));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PayrollResponse> processPayment(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.processPayment(id));
    }
}
