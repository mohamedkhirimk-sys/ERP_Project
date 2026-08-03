package com.erp.system.finance.controller;

import com.erp.system.finance.dto.BankAccountRequest;
import com.erp.system.finance.dto.BankAccountResponse;
import com.erp.system.finance.service.BankAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @PostMapping
    public ResponseEntity<BankAccountResponse> create(@Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<BankAccountResponse>> getAll() {
        return ResponseEntity.ok(bankAccountService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BankAccountResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(bankAccountService.getById(id));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
