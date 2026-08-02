# Trial Balance 6-Column Table Implementation Plan (revision)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restore the cumulative movement columns (Total Debits / Total Credits) in the trial balance table alongside the balance columns (Balance DR / Balance CR), for a 6-column table.

**Architecture:** Single-file frontend change in `frontend/src/features/reports/TrialBalanceTab.tsx`. All data is already returned by `GET /api/reports/financial`: `trialBalance[].totalDebits` / `totalCredits` (cumulative movement totals) and `trialBalance[].balance` (signed). No backend changes, no new API calls.

**Tech Stack:** React + TypeScript + Vite (reporting frontend), Tailwind CSS classes (existing style), oxlint.

## Global Constraints

- Only `frontend/src/features/reports/TrialBalanceTab.tsx` is modified — no other file.
- Preserve the existing component structure, summary cards (unchanged), loading/error/empty-state logic, and the "Recent Journal Entries" panel.
- Use the exact Tailwind classes already used in the file (no new classes).
- Verify with `npx tsc -b && npx vite build` and `npx oxlint` in `frontend/` — both must be green.
- Column order: Account | Type | Total Debits | Total Credits | Balance DR | Balance CR.
- Sign convention: `balance > 0` → Balance DR; `balance < 0` → Balance CR (`Math.abs`); `balance === 0` → dash `—` in both balance columns.
- Movements are cumulative (no period filter) — per user decision 2026-08-02.

---

### Task 1: Add movement columns to the trial balance table

**Files:**
- Modify: `frontend/src/features/reports/TrialBalanceTab.tsx` (table header, table body rows, tfoot)

**Interfaces:**
- Consumes: `report.summary.totalDebits` / `report.summary.totalCredits` (numbers, movement totals); `report.trialBalance[]` items with `totalDebits: number`, `totalCredits: number`, `balance: number`; the existing constants `totalBalanceDr` / `totalBalanceCr` (already defined at lines 28-29 of the file).
- Produces: nothing new; the tfoot shows 4 totals using `summary.totalDebits`, `summary.totalCredits`, `totalBalanceDr`, `totalBalanceCr`. No external consumers.

- [ ] **Step 1: Replace the table header cells**

In `frontend/src/features/reports/TrialBalanceTab.tsx`, replace these two header cells (current lines 50-51, "Balance DR" / "Balance CR"):

```tsx
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Balance DR</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Balance CR</th>
```

with four header cells (movement columns first, then balance columns):

```tsx
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Total Debits</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Total Credits</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Balance DR</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Balance CR</th>
```

Keep the `Account` and `Type` header cells unchanged.

- [ ] **Step 2: Insert the movement cells in the table body**

In the row map (`{trialBalance.map((a) => (`), the row currently has two balance cells (lines 59-60). Insert two movement cells BEFORE them, so the row becomes:

```tsx
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">${a.totalDebits.toLocaleString()}</td>
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">${a.totalCredits.toLocaleString()}</td>
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">{a.balance > 0 ? `$${a.balance.toLocaleString()}` : '—'}</td>
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">{a.balance < 0 ? `$${Math.abs(a.balance).toLocaleString()}` : '—'}</td>
```

The existing two balance cells (with the `—` em-dash, U+2014) stay byte-identical. Note: the movement cells use `${...}` directly (no `{...}` JSX wrapper — the same style as the original pre-revision code).

- [ ] **Step 3: Replace the tfoot totals row**

Replace the two tfoot cells (current lines 67-68, `${totalBalanceDr.toLocaleString()}` / `${totalBalanceCr.toLocaleString()}`) with four tfoot cells:

```tsx
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${summary.totalDebits.toLocaleString()}</td>
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${summary.totalCredits.toLocaleString()}</td>
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${totalBalanceDr.toLocaleString()}</td>
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${totalBalanceCr.toLocaleString()}</td>
```

Keep the `colSpan={2}` "Total" label cell unchanged.

- [ ] **Step 4: Verify build**

Run in `C:\Users\MED\Documents\ERP_Project\frontend`:
`npx tsc -b && npx vite build`
Expected: build succeeds (no errors, no unused-variable errors — `summary` is used by the "Accounts"/"Journal Entries" cards and the new tfoot cells).

- [ ] **Step 5: Verify lint**

Run in `C:\Users\MED\Documents\ERP_Project\frontend`:
`npx oxlint`
Expected: `Found 0 warnings and 0 errors.`

- [ ] **Step 6: Commit**

```bash
git add frontend/src/features/reports/TrialBalanceTab.tsx
git commit -m "feat: show movement totals and balances in trial balance"
```

---

## Self-Review Notes

- Spec coverage: 6-column table (Steps 1-2), movement columns cumulative from `summary`/`trialBalance[].totalDebits`/`totalCredits` (Steps 1-2), balance columns unchanged (Step 2), tfoot 4 totals (Step 3), summary cards unchanged (none), no backend change (none), no new API calls (none).
- No placeholders: every step contains the exact code.
- Type consistency: `summary.totalDebits`/`summary.totalCredits` exist in the `FinancialReport` interface (line 5 of the file) and `totalDebits`/`totalCredits` on each trialBalance item (line 6) — both present since the original page.
- Current file state verified: header cells at lines 50-51, body balance cells at lines 59-60, tfoot cells at lines 67-68.
- Zero-balance accounts (1200, 2000, 5000, 5100, 5200, 6000) show `—` in both balance columns; movement columns show their (zero) totals as `$0`.
