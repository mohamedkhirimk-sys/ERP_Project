package com.erp.reporting.controller;

import com.erp.reporting.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class DemoDataController {

    private final DemoDataService demoDataService;

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> seedDemoData() {
        Map<String, Object> result = demoDataService.seedAll();
        return ResponseEntity.ok(result);
    }
}
