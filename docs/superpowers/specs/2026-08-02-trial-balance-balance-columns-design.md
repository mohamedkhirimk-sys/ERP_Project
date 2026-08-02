# Design — Trial Balance: show account balances (DR/CR)

Date: 2026-08-02 · Status: approved

## Problem

The Trial Balance table in the Financial Report page (`frontend/src/features/reports/TrialBalanceTab.tsx`) only displays the per-account movement totals (Debits / Credits columns). The account `balance` is already returned by the backend (`GET /api/reports/financial` → `trialBalance[].balance`, computed in `ReportService.java:165`) but is not displayed.

## Decision

Convert the table into a classic trial balance: two balance columns (Balance DR / Balance CR) replacing the movement columns. Balances are signed: `balance = balance + debit − credit` (finance-service `JournalEntryServiceImpl.java:37`), so positive = net debit, negative = net credit.

## Scope

- Only `frontend/src/features/reports/TrialBalanceTab.tsx` — no backend changes, no new API calls.
- All values computed client-side from the `trialBalance` array already returned by `/api/reports/financial`.

## Table

Columns: Account | Type | Balance DR | Balance CR

- `balance > 0` → Balance DR shows the value, Balance CR shows a dash
- `balance < 0` → Balance CR shows `Math.abs(balance)`, Balance DR shows a dash
- `balance === 0` → both columns show a dash
- tfoot: single "Total" row with client-side totals: Total Balance DR = sum of positive balances, Total Balance CR = sum of absolute values of negative balances. The two totals are equal (trial balance invariant).
- Account/Type columns unchanged (code, name, type badge).

## Summary cards

- "Total Debits" / "Total Credits" cards (currently movement totals from `summary.totalDebits` / `summary.totalCredits`) become "Total Balance DR" / "Total Balance CR" with the same client-side computed values as the tfoot.
- "Accounts" and "Journal Entries" cards unchanged.

## Unchanged

- "Recent Journal Entries" panel (movements remain visible there).
- Loading state, error handling (console.error), empty state (`if (!report) return null`).
- All other tabs and pages.

## Verification

- `npx tsc -b && npx vite build` in `frontend/` — green.
- `npx oxlint` in `frontend/` — 0 errors/warnings.
- Manual: table shows balances in DR/CR columns; tfoot totals equal; zero-balance accounts (1200, 2000, 5000, 5100, 5200, 6000) show dashes.
