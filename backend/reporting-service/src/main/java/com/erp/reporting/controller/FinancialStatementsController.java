package com.erp.reporting.controller;

import com.erp.reporting.dto.BalanceSheetResponse;
import com.erp.reporting.dto.IncomeStatementResponse;
import com.erp.reporting.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class FinancialStatementsController {

    private final ReportService reportService;

    @GetMapping("/balance-sheet")
    public BalanceSheetResponse balanceSheet() {
        return reportService.balanceSheet();
    }

    @GetMapping("/income-statement")
    public IncomeStatementResponse incomeStatement() {
        return reportService.incomeStatement();
    }
}
