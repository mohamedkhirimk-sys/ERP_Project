package com.erp.system.finance.controller;

import com.erp.system.finance.dto.AccountingPostingRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.service.AccountingPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounting/postings")
@RequiredArgsConstructor
public class AccountingPostingController {

    private final AccountingPostingService accountingPostingService;

    @PostMapping
    public ResponseEntity<JournalEntryResponse> post(@Valid @RequestBody AccountingPostingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountingPostingService.post(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
