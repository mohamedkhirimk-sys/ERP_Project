package com.erp.system.finance.controller;

import com.erp.system.finance.dto.JournalEntryRequest;
import com.erp.system.finance.dto.JournalEntryResponse;
import com.erp.system.finance.service.JournalEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public ResponseEntity<List<JournalEntryResponse>> getAllEntries() {
        return ResponseEntity.ok(journalEntryService.getAllEntries());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<JournalEntryResponse>> getEntriesByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(journalEntryService.getEntriesByAccount(accountId));
    }
}
