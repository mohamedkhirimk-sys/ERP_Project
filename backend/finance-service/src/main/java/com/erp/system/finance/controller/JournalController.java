package com.erp.system.finance.controller;

import com.erp.system.finance.dto.JournalDetailResponse;
import com.erp.system.finance.dto.JournalSummaryResponse;
import com.erp.system.finance.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/journals")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    @GetMapping
    public ResponseEntity<List<JournalSummaryResponse>> summary() {
        return ResponseEntity.ok(journalService.summary());
    }

    @GetMapping("/{code}")
    public ResponseEntity<JournalDetailResponse> detail(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(journalService.detail(code, from, to));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
