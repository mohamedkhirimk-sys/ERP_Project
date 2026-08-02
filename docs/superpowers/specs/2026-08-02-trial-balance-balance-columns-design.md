# Design — Trial Balance: movement columns + balance columns (DR/CR)

Date: 2026-08-02 · Status: approved (revised 2026-08-02: movement columns restored alongside balance columns)

## Problem

The Trial Balance table in the Financial Report page (`frontend/src/features/reports/TrialBalanceTab.tsx`) must show BOTH the per-account movement totals (Total Debits / Total Credits, cumulative) AND the account balances (Balance DR / Balance CR). The backend already returns all data via `GET /api/reports/financial`: `trialBalance[].totalDebits` / `trialBalance[].totalCredits` (movement totals per account, computed in `ReportService.java:153-160`) and `trialBalance[].balance` (computed in `ReportService.java:165`).

## Decision

Six-column table: Account | Type | Total Debits | Total Credits | Balance DR | Balance CR.

- Movement columns: `totalDebits` / `totalCredits` — cumulative movement totals (all entries, no period filter — per user decision 2026-08-02).
- Balance columns: balances are signed (`balance = balance + debit − credit`, finance-service `JournalEntryServiceImpl.java:37`), so positive = net debit, negative = net credit:
  - `balance > 0` → Balance DR shows the value, Balance CR shows a dash
  - `balance < 0` → Balance CR shows `Math.abs(balance)`, Balance DR shows a dash
  - `balance === 0` → both columns show a dash
- tfoot: single "Total" row with 4 totals: Total Debits / Total Credits (from `summary.totalDebits` / `summary.totalCredits`) and Total Balance DR / Total Balance CR (client-side sums of positive / absolute negative balances). The two balance totals are equal (trial balance invariant).
- Account/Type columns unchanged (code, name, type badge).

## Summary cards

- Four cards: "Accounts", "Journal Entries", "Total Balance DR", "Total Balance CR" (balance totals computed client-side, same values as the tfoot).
- Cards unchanged by this revision.

## Unchanged

- "Recent Journal Entries" panel (movements remain visible there).
- Loading state, error handling (console.error), empty state (`if (!report) return null`).
- All other tabs and pages.

## Verification

- `npx tsc -b && npx vite build` in `frontend/` — green.
- `npx oxlint` in `frontend/` — 0 errors/warnings.
- Manual: table shows 4 numeric columns per account; tfoot shows 4 totals; balance DR/CR totals equal; zero-balance accounts (1200, 2000, 5000, 5100, 5200, 6000) show dashes in the balance columns.
