# Financial Statements (Balance Sheet & Income Statement) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Balance Sheet and Income Statement reports, computed from the trial balance in reporting-service, displayed as tabs in the Financial Report page.

**Architecture:** Two new endpoints in reporting-service (`ReportService` reuses `fetchList` against `/finance-service/api/accounts`) returning typed DTOs; the frontend FinancialReportPage gains a tab bar (Trial Balance | Balance Sheet | Income Statement) with two new tab components fetching the endpoints via `@/lib/axios`. Gateway route `/api/reports/**` already exists.

**Tech Stack:** Java 21 + Spring Boot 3.2 (reporting-service), React 19 + TypeScript + Vite + Tailwind 4 (frontend), no new dependencies.

## Global Constraints

- UI language: English (labels: Balance Sheet, Income Statement, Assets, Liabilities, Equity, Net income, Revenue, Expenses).
- Reporting-service has NO test framework — verification is `mvn.cmd clean compile` + E2E via gateway (8082).
- Frontend has NO test framework — verification is `npm run build` (tsc + vite) + `npm run lint` (oxlint), both must stay green.
- Only non-zero-balance accounts are included; accounts sorted by `accountCode`.
- Sign conventions: stored balances are positive for debit accounts (ASSET, EXPENSE), negative for credit accounts (LIABILITY, EQUITY, REVENUE). Displayed amounts in reports are always positive (absolute values); `netIncome` may be negative (loss).
- Invariant: `totalAssets == totalLiabilities + totalEquity + netIncome`.
- Data is all-period (no date filter), same scope as the current Trial Balance.
- No PDF export in this plan; no routing/menu changes (FinancialReportPage only); axios only via `@/lib/axios`.

---

### Task 1: Backend DTOs + Balance Sheet endpoint

**Files:**
- Create: `backend/reporting-service/src/main/java/com/erp/reporting/dto/BalanceSheetResponse.java`
- Create: `backend/reporting-service/src/main/java/com/erp/reporting/dto/IncomeStatementResponse.java`
- Create: `backend/reporting-service/src/main/java/com/erp/reporting/dto/AccountLine.java`
- Create: `backend/reporting-service/src/main/java/com/erp/reporting/controller/FinancialStatementsController.java`
- Modify: `backend/reporting-service/src/main/java/com/erp/reporting/service/ReportService.java` (add `balanceSheet()` and `incomeStatement()` methods + `netIncome()` helper)

**Interfaces:**
- Consumes: `ReportService.fetchList(String url)` (private, already exists — unwraps Spring Pages, returns `List<Map<String, Object>>`); account maps contain `id`, `accountCode`, `accountName`, `accountType`, `balance` (numbers deserialized as `Number`).
- Produces (consumed by Task 2 frontend):
  - `GET /api/reports/balance-sheet` → `BalanceSheetResponse`
  - `GET /api/reports/income-statement` → `IncomeStatementResponse`
  - `AccountLine` fields: `accountCode: String`, `accountName: String`, `balance: BigDecimal`
  - `BalanceSheetResponse` fields: `assets: List<AccountLine>`, `liabilities: List<AccountLine>`, `equity: List<AccountLine>`, `netIncome: BigDecimal`, `totalAssets: BigDecimal`, `totalLiabilities: BigDecimal`, `totalEquity: BigDecimal`, `totalLiabilitiesEquity: BigDecimal`
  - `IncomeStatementResponse` fields: `revenue: List<AccountLine>`, `expenses: List<AccountLine>`, `totalRevenue: BigDecimal`, `totalExpenses: BigDecimal`, `netIncome: BigDecimal`

- [ ] **Step 1: Create `AccountLine.java`**

```java
package com.erp.reporting.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class AccountLine {
    private String accountCode;
    private String accountName;
    private BigDecimal balance;
}
```

- [ ] **Step 2: Create `BalanceSheetResponse.java`**

```java
package com.erp.reporting.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BalanceSheetResponse {
    private List<AccountLine> assets;
    private List<AccountLine> liabilities;
    private List<AccountLine> equity;
    private BigDecimal netIncome;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    private BigDecimal totalLiabilitiesEquity;
}
```

- [ ] **Step 3: Create `IncomeStatementResponse.java`**

```java
package com.erp.reporting.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IncomeStatementResponse {
    private List<AccountLine> revenue;
    private List<AccountLine> expenses;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private BigDecimal netIncome;
}
```

- [ ] **Step 4: Add `balanceSheet()`, `incomeStatement()` and helpers to `ReportService.java`**

Insert after the `getFinancialReport()` method (before the `// ─── HR Report ───` section). Imports needed at top of file: none new (`BigDecimal`, `Comparator`, `List`, `Map`, `ArrayList`, `HashMap` already imported; `java.util.stream.Collectors` already imported — add `import java.util.ArrayList;` and `import java.util.HashMap;` if not present — check the existing import block first).

```java
    // ──────────────────────────────────────
    //  Balance Sheet & Income Statement
    // ──────────────────────────────────────

    public BalanceSheetResponse balanceSheet() {
        List<Map<String, Object>> accounts = fetchList("http://finance-service/api/accounts");
        List<AccountLine> assets = lines(accounts, "ASSET");
        List<AccountLine> liabilities = lines(accounts, "LIABILITY");
        List<AccountLine> equity = lines(accounts, "EQUITY");
        BigDecimal netIncome = netIncome(accounts);
        BigDecimal totalAssets = sumLines(assets);
        BigDecimal totalLiabilities = sumLines(liabilities);
        BigDecimal totalEquity = sumLines(equity);
        return BalanceSheetResponse.builder()
                .assets(assets)
                .liabilities(liabilities)
                .equity(equity)
                .netIncome(netIncome)
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiabilities)
                .totalEquity(totalEquity)
                .totalLiabilitiesEquity(totalLiabilities.add(totalEquity).add(netIncome))
                .build();
    }

    public IncomeStatementResponse incomeStatement() {
        List<Map<String, Object>> accounts = fetchList("http://finance-service/api/accounts");
        List<AccountLine> revenue = lines(accounts, "REVENUE");
        List<AccountLine> expenses = lines(accounts, "EXPENSE");
        BigDecimal totalRevenue = sumLines(revenue);
        BigDecimal totalExpenses = sumLines(expenses);
        return IncomeStatementResponse.builder()
                .revenue(revenue)
                .expenses(expenses)
                .totalRevenue(totalRevenue)
                .totalExpenses(totalExpenses)
                .netIncome(totalRevenue.subtract(totalExpenses))
                .build();
    }

    private BigDecimal netIncome(List<Map<String, Object>> accounts) {
        BigDecimal revenue = accounts.stream()
                .filter(a -> "REVENUE".equals(a.get("accountType")))
                .map(a -> new BigDecimal(a.get("balance").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = accounts.stream()
                .filter(a -> "EXPENSE".equals(a.get("accountType")))
                .map(a -> new BigDecimal(a.get("balance").toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return revenue.negate().subtract(expenses);
    }

    private List<AccountLine> lines(List<Map<String, Object>> accounts, String type) {
        return accounts.stream()
                .filter(a -> type.equals(a.get("accountType")))
                .filter(a -> new BigDecimal(a.get("balance").toString()).abs().signum() != 0)
                .map(a -> AccountLine.builder()
                        .accountCode((String) a.get("accountCode"))
                        .accountName((String) a.get("accountName"))
                        .balance(new BigDecimal(a.get("balance").toString()).abs())
                        .build())
                .sorted(Comparator.comparing(AccountLine::getAccountCode))
                .toList();
    }

    private BigDecimal sumLines(List<AccountLine> lines) {
        return lines.stream()
                .map(AccountLine::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
```

Note: `netIncome` math: revenue balances are negative (credit), expenses positive (debit). `−(−150) − 50 = 100` on current data. `lines(...)` includes ONLY non-zero balances; displayed balances are absolute values.

- [ ] **Step 5: Create `FinancialStatementsController.java`**

```java
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
```

- [ ] **Step 6: Compile**

Run: `mvn.cmd clean compile` in `C:\Users\MED\Documents\ERP_Project\backend\reporting-service`
Expected: `BUILD SUCCESS` (or no ERROR lines with `-q`).

- [ ] **Step 7: Commit**

```bash
git add backend/reporting-service/src/main/java/com/erp/reporting/dto/AccountLine.java backend/reporting-service/src/main/java/com/erp/reporting/dto/BalanceSheetResponse.java backend/reporting-service/src/main/java/com/erp/reporting/dto/IncomeStatementResponse.java backend/reporting-service/src/main/java/com/erp/reporting/controller/FinancialStatementsController.java backend/reporting-service/src/main/java/com/erp/reporting/service/ReportService.java
git commit -m "feat: balance sheet and income statement report endpoints"
```

---

### Task 2: Frontend — tabs + Balance Sheet + Income Statement components

**Files:**
- Create: `frontend/src/features/reports/BalanceSheetTab.tsx`
- Create: `frontend/src/features/reports/IncomeStatementTab.tsx`
- Modify: `frontend/src/features/reports/FinancialReportPage.tsx` (tab bar + move existing content into TrialBalanceTab)

**Interfaces:**
- Consumes: `GET /api/reports/balance-sheet` and `GET /api/reports/income-statement` (shapes from Task 1 — JSON camelCase field names identical to the Java fields; `balance`/totals serialized as numbers).
- Produces: `BalanceSheetTab` and `IncomeStatementTab` components, no props, self-fetching.

- [ ] **Step 1: Create `BalanceSheetTab.tsx`**

```tsx
import { useEffect, useState } from 'react'
import api from '@/lib/axios'

interface AccountLine {
  accountCode: string
  accountName: string
  balance: number
}

interface BalanceSheet {
  assets: AccountLine[]
  liabilities: AccountLine[]
  equity: AccountLine[]
  netIncome: number
  totalAssets: number
  totalLiabilities: number
  totalEquity: number
  totalLiabilitiesEquity: number
}

const sectionTitle = 'text-sm font-semibold text-gray-700 uppercase tracking-wider'
const thClass = 'px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-left'
const tdClass = 'px-4 py-3 text-sm text-gray-900'
const tdAmount = 'px-4 py-3 font-mono text-sm text-gray-900 text-right'

function AccountTable({ title, rows }: { title: string; rows: AccountLine[] }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <div className="px-5 py-4 border-b border-gray-100">
        <h2 className={sectionTitle}>{title}</h2>
      </div>
      <table className="w-full">
        <thead>
          <tr className="border-b border-gray-200 bg-gray-50">
            <th className={thClass}>Account</th>
            <th className={`${thClass} text-right`}>Balance</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {rows.map((a) => (
            <tr key={a.accountCode}>
              <td className={tdClass}><span className="font-mono text-gray-600">{a.accountCode}</span><span className="ml-2 font-medium">{a.accountName}</span></td>
              <td className={tdAmount}>${a.balance.toLocaleString()}</td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr><td colSpan={2} className="px-4 py-8 text-center text-gray-500">No accounts</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

export default function BalanceSheetTab() {
  const [sheet, setSheet] = useState<BalanceSheet | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/balance-sheet')
      .then((res) => setSheet(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-gray-500 py-12 text-center">Loading...</p>
  if (!sheet) return null

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div className="space-y-6">
        <AccountTable title="Assets" rows={sheet.assets} />
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <p className={sectionTitle}>Total Assets</p>
          <p className="mt-2 text-2xl font-bold text-gray-900">${sheet.totalAssets.toLocaleString()}</p>
        </div>
      </div>
      <div className="space-y-6">
        <AccountTable title="Liabilities" rows={sheet.liabilities} />
        <AccountTable title="Equity" rows={sheet.equity} />
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
          <p className="text-sm font-medium text-gray-500">Net income</p>
          <p className={`mt-1 text-2xl font-bold ${sheet.netIncome < 0 ? 'text-red-600' : 'text-gray-900'}`}>
            ${sheet.netIncome.toLocaleString()}
          </p>
          <p className="mt-3 text-sm font-medium text-gray-500">Total Liabilities &amp; Equity</p>
          <p className={`mt-1 text-2xl font-bold ${sheet.totalAssets === sheet.totalLiabilitiesEquity ? 'text-gray-900' : 'text-red-600'}`}>
            ${sheet.totalLiabilitiesEquity.toLocaleString()}
          </p>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Create `IncomeStatementTab.tsx`**

```tsx
import { useEffect, useState } from 'react'
import api from '@/lib/axios'

interface AccountLine {
  accountCode: string
  accountName: string
  balance: number
}

interface IncomeStatement {
  revenue: AccountLine[]
  expenses: AccountLine[]
  totalRevenue: number
  totalExpenses: number
  netIncome: number
}

const sectionTitle = 'text-sm font-semibold text-gray-700 uppercase tracking-wider'
const thClass = 'px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider text-left'
const tdClass = 'px-4 py-3 text-sm text-gray-900'
const tdAmount = 'px-4 py-3 font-mono text-sm text-gray-900 text-right'

function AccountTable({ title, rows }: { title: string; rows: AccountLine[] }) {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
      <div className="px-5 py-4 border-b border-gray-100">
        <h2 className={sectionTitle}>{title}</h2>
      </div>
      <table className="w-full">
        <thead>
          <tr className="border-b border-gray-200 bg-gray-50">
            <th className={thClass}>Account</th>
            <th className={`${thClass} text-right`}>Amount</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-gray-100">
          {rows.map((a) => (
            <tr key={a.accountCode}>
              <td className={tdClass}><span className="font-mono text-gray-600">{a.accountCode}</span><span className="ml-2 font-medium">{a.accountName}</span></td>
              <td className={tdAmount}>${a.balance.toLocaleString()}</td>
            </tr>
          ))}
          {rows.length === 0 && (
            <tr><td colSpan={2} className="px-4 py-8 text-center text-gray-500">No accounts</td></tr>
          )}
        </tbody>
      </table>
    </div>
  )
}

export default function IncomeStatementTab() {
  const [statement, setStatement] = useState<IncomeStatement | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    api.get('/api/reports/income-statement')
      .then((res) => setStatement(res.data))
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <p className="text-gray-500 py-12 text-center">Loading...</p>
  if (!statement) return null

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <AccountTable title="Revenue" rows={statement.revenue} />
      <AccountTable title="Expenses" rows={statement.expenses} />
      <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-5">
        <div className="flex justify-between items-center">
          <p className="text-sm font-medium text-gray-500">Total Revenue</p>
          <p className="font-mono text-sm font-semibold text-gray-900">${statement.totalRevenue.toLocaleString()}</p>
        </div>
        <div className="flex justify-between items-center mt-2">
          <p className="text-sm font-medium text-gray-500">Total Expenses</p>
          <p className="font-mono text-sm font-semibold text-gray-900">${statement.totalExpenses.toLocaleString()}</p>
        </div>
        <div className="flex justify-between items-center mt-4 pt-4 border-t border-gray-200">
          <p className="text-sm font-semibold text-gray-700">Net income</p>
          <p className={`font-mono text-lg font-bold ${statement.netIncome < 0 ? 'text-red-600' : 'text-gray-900'}`}>
            ${statement.netIncome.toLocaleString()}
          </p>
        </div>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Restructure `FinancialReportPage.tsx` with a tab bar**

Replace the whole file body (keep the header icon/title/subtitle) with:

```tsx
import { useState } from 'react'
import BalanceSheetTab from './BalanceSheetTab'
import IncomeStatementTab from './IncomeStatementTab'
import TrialBalanceTab from './TrialBalanceTab'

const tabs = [
  { key: 'trial', label: 'Trial Balance' },
  { key: 'balance', label: 'Balance Sheet' },
  { key: 'income', label: 'Income Statement' },
] as const

type TabKey = (typeof tabs)[number]['key']

export default function FinancialReportPage() {
  const [tab, setTab] = useState<TabKey>('trial')

  return (
    <div>
      <div className="flex items-center gap-3 mb-6">
        <div className="w-10 h-10 bg-orange-100 rounded-full flex items-center justify-center">
          <svg className="w-5 h-5 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
        </div>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Financial Report</h1>
          <p className="text-sm text-gray-500">Trial balance and journal activity</p>
        </div>
      </div>

      <div className="flex gap-1 bg-gray-100 rounded-lg p-1 mb-6 w-fit">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`px-4 py-2 rounded-md text-sm font-medium transition ${
              tab === t.key ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'trial' && <TrialBalanceTab />}
      {tab === 'balance' && <BalanceSheetTab />}
      {tab === 'income' && <IncomeStatementTab />}
    </div>
  )
}
```

- [ ] **Step 4: Create `TrialBalanceTab.tsx` with the current page content**

Move the current `FinancialReportPage.tsx` content (interfaces, `typeColors`, state, effects, the summary cards grid, the Trial Balance table WITH its totals `tfoot`, and the Recent Journal Entries panel) into a new file `frontend/src/features/reports/TrialBalanceTab.tsx` — default-exported component `TrialBalanceTab`, no props, identical JSX/classes, with ONLY the outer `<div>` wrapper (no page header, no tab bar — the header stays in FinancialReportPage). Keep the `tfoot` totals row added earlier (`Total` row with `summary.totalDebits`/`summary.totalCredits`).

- [ ] **Step 5: Verify build + lint**

Run in `C:\Users\MED\Documents\ERP_Project\frontend`:
`npm run build` — expected: SUCCESS (tsc + vite, no errors)
`npm run lint` — expected: 0 errors, 0 warnings

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/reports/FinancialReportPage.tsx frontend/src/features/reports/TrialBalanceTab.tsx frontend/src/features/reports/BalanceSheetTab.tsx frontend/src/features/reports/IncomeStatementTab.tsx
git commit -m "feat: balance sheet and income statement tabs in financial report"
```

---

### Task 3: Restart + E2E verification

**Files:** none (operations task)

**Interfaces:**
- Consumes: running reporting-service (old build on disk) → must be restarted with the newly compiled classes.

- [ ] **Step 1: Restart reporting-service**

Find the running instance: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*ReportingApplication*' }` → take its ProcessId and its argfile path (from CommandLine, `@C:\...\cp_*.argfile`).
Stop it, then relaunch (same argfile, main class `com.erp.reporting.ReportingApplication`), redirecting output to `C:\Users\MED\AppData\Local\Temp\opencode\restart_logs\reporting.log` (create the dir if needed). Wait ~15s, then confirm a `java.exe` with `ReportingApplication` is running.

- [ ] **Step 2: E2E via gateway**

Run:
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/reports/balance-sheet" | ConvertTo-Json -Depth 5
Invoke-RestMethod -Uri "http://localhost:8082/api/reports/income-statement" | ConvertTo-Json -Depth 5
```

Expected on current data:
- balance-sheet: `totalAssets` 600.00, `totalLiabilities` 30.00, `totalEquity` 470.00, `netIncome` 100.00, `totalLiabilitiesEquity` 600.00 (equal to totalAssets); assets contain 1000/1010/1020/1100; liabilities contain 2200; equity contains 3000; no zero-balance accounts (no 1200/2000/5000/5100/5200/6000).
- income-statement: `totalRevenue` 150.00 (4000), `totalExpenses` 50.00 (7000), `netIncome` 100.00.

If values differ from expected, capture the actual JSON and report it — do not fix silently.

- [ ] **Step 3: Report results**

Write the E2E results to `.superpowers/sdd/2026-08-02-financial-statements/task-3-report.md` (create the directory), including both JSON payloads.

---

## Self-Review Notes

- Spec coverage: Balance Sheet endpoint (Task 1), Income Statement endpoint (Task 1), tabs + components (Task 2), invariant display (Task 2), net income line in Equity section per spec (Task 2 card shows Net income + Total Liabilities & Equity), verification (Task 3). PDF deferred per spec — not in plan.
- No placeholders: every code step contains full code.
- Type consistency: `AccountLine`/`BalanceSheetResponse`/`IncomeStatementResponse` field names in Task 1 match the TS interfaces in Task 2 (camelCase JSON identical); `netIncome` number semantics identical on both sides.
