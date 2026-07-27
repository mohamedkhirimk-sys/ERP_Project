package com.erp.reporting.controller;

import com.erp.reporting.dto.*;
import com.erp.reporting.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }

    @GetMapping("/sales")
    public ResponseEntity<SalesReport> getSalesReport() {
        return ResponseEntity.ok(reportService.getSalesReport());
    }

    @GetMapping("/inventory")
    public ResponseEntity<InventoryReport> getInventoryReport() {
        return ResponseEntity.ok(reportService.getInventoryReport());
    }

    @GetMapping("/financial")
    public ResponseEntity<FinancialReport> getFinancialReport() {
        return ResponseEntity.ok(reportService.getFinancialReport());
    }

    @GetMapping("/hr")
    public ResponseEntity<HrReport> getHrReport() {
        return ResponseEntity.ok(reportService.getHrReport());
    }

    @GetMapping("/procurement")
    public ResponseEntity<ProcurementReport> getProcurementReport() {
        return ResponseEntity.ok(reportService.getProcurementReport());
    }
}
