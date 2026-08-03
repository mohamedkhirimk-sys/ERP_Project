package com.erp.system.finance.controller;

import com.erp.system.finance.dto.*;
import com.erp.system.finance.service.TreasuryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/treasury")
@RequiredArgsConstructor
public class TreasuryController {

    private final TreasuryService treasuryService;

    @PostMapping("/transfers")
    public ResponseEntity<CashMovementResponse> transfer(@RequestBody TreasuryTransferRequest request) {
        return ResponseEntity.ok(treasuryService.transfer(request));
    }

    @PostMapping("/expenses")
    public ResponseEntity<CashMovementResponse> expense(@RequestBody TreasuryExpenseRequest request) {
        return ResponseEntity.ok(treasuryService.expense(request));
    }

    @PostMapping("/deposits")
    public ResponseEntity<CashMovementResponse> deposit(@RequestBody TreasuryDepositRequest request) {
        return ResponseEntity.ok(treasuryService.deposit(request));
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<CashMovementResponse> withdraw(@RequestBody TreasuryWithdrawalRequest request) {
        return ResponseEntity.ok(treasuryService.withdraw(request));
    }

    @GetMapping("/position")
    public ResponseEntity<TreasuryPositionResponse> position() {
        return ResponseEntity.ok(treasuryService.position());
    }

    @GetMapping("/movements")
    public ResponseEntity<List<CashMovementResponse>> movements(
            @RequestParam(required = false) Long bankAccountId) {
        return ResponseEntity.ok(treasuryService.movements(bankAccountId));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
