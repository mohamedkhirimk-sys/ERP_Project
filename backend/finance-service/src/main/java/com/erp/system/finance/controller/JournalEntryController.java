package com.erp.system.finance.controller;

import com.erp.system.finance.dto.JournalEntryRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.service.JournalEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/journal-entries")
@RequiredArgsConstructor
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    @PostMapping
    public ResponseEntity<JournalEntryResponse> createEntry(@Valid @RequestBody JournalEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journalEntryService.createEntry(request));
    }

    @GetMapping
    public ResponseEntity<Page<JournalEntryResponse>> getAllEntries(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.getAllEntries(pageable));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<Page<JournalEntryResponse>> getEntriesByAccount(
            @PathVariable Long accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(journalEntryService.getEntriesByAccount(accountId, pageable));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
