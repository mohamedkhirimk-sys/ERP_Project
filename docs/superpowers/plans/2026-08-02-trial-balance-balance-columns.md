# Trial Balance Balance Columns Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the movement columns (Debits/Credits) of the trial balance table with classic Balance DR / Balance CR columns, computed client-side from the account `balance` field.

**Architecture:** Single-file frontend change in `frontend/src/features/reports/TrialBalanceTab.tsx`. The backend already returns `trialBalance[].balance` (signed: positive = net debit, negative = net credit) via `GET /api/reports/financial`; all new values are computed client-side from that array. No backend changes, no new API calls.

**Tech Stack:** React + TypeScript + Vite (reporting frontend), Tailwind CSS classes (existing style), oxlint.

## Global Constraints

- Only `frontend/src/features/reports/TrialBalanceTab.tsx` is modified — no other file.
- Preserve the existing component structure, loading/error/empty-state logic, and the "Recent Journal Entries" panel.
- Use the exact Tailwind classes already used in the file (no new classes).
- Verify with `npx tsc -b && npx vite build` and `npx oxlint` in `frontend/` — both must be green.
- Sign convention: `balance > 0` → Balance DR; `balance < 0` → Balance CR (`Math.abs`); `balance === 0` → dash `—` in both columns.

---

### Task 1: Add Balance DR/CR columns to TrialBalanceTab

**Files:**
- Modify: `frontend/src/features/reports/TrialBalanceTab.tsx` (summary cards, table header, table body rows, tfoot; add two computed constants)

**Interfaces:**
- Consumes: `report.trialBalance` — array of `{ accountCode: string; accountName: string; accountType: string; balance: number; totalDebits: number; totalCredits: number }` from `GET /api/reports/financial`.
- Produces: two client-side totals `totalBalanceDr` and `totalBalanceCr` (numbers) used by both the summary cards and the tfoot; no external consumers.

- [ ] **Step 1: Add computed balance totals**

In `frontend/src/features/reports/TrialBalanceTab.tsx`, after the line `const { summary, trialBalance, recentEntries } = report`, add:

```tsx
  const totalBalanceDr = trialBalance.filter((a) => a.balance > 0).reduce((sum, a) => sum + a.balance, 0)
  const totalBalanceCr = trialBalance.filter((a) => a.balance < 0).reduce((sum, a) => sum + Math.abs(a.balance), 0)
```

- [ ] **Step 2: Replace the two movement summary cards**

Replace these two lines (the "Total Debits" and "Total Credits" cards):

```tsx
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-blue-600">${summary.totalDebits.toLocaleString()}</p><p className="text-xs text-gray-500">Total Debits</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-blue-600">${summary.totalCredits.toLocaleString()}</p><p className="text-xs text-gray-500">Total Credits</p></div>
```

with:

```tsx
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-blue-600">${totalBalanceDr.toLocaleString()}</p><p className="text-xs text-gray-500">Total Balance DR</p></div>
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-4"><p className="text-2xl font-bold text-blue-600">${totalBalanceCr.toLocaleString()}</p><p className="text-xs text-gray-500">Total Balance CR</p></div>
```

- [ ] **Step 3: Replace the table header cells**

Replace the two header cells `<th ...>Debits</th>` and `<th ...>Credits</th>` (keep the `Account` and `Type` headers unchanged):

```tsx
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Balance DR</th>
                <th className="px-4 py-3 text-xs font-semibold text-gray-600 uppercase tracking-wider">Balance CR</th>
```

- [ ] **Step 4: Replace the table body balance cells**

In the row map (`{trialBalance.map((a) => (`), replace the two movement cells (currently `<td ...>${a.totalDebits.toLocaleString()}</td>` and `<td ...>${a.totalCredits.toLocaleString()}</td>`) with:

```tsx
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">{a.balance > 0 ? `$${a.balance.toLocaleString()}` : '—'}</td>
                  <td className="px-4 py-3 font-mono text-sm text-gray-900">{a.balance < 0 ? `$${Math.abs(a.balance).toLocaleString()}` : '—'}</td>
```

- [ ] **Step 5: Replace the tfoot totals row**

Replace the two tfoot cells (currently `summary.totalDebits.toLocaleString()` / `summary.totalCredits.toLocaleString()`) with:

```tsx
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${totalBalanceDr.toLocaleString()}</td>
                <td className="px-4 py-3 font-mono text-sm font-semibold text-gray-900">${totalBalanceCr.toLocaleString()}</td>
```

- [ ] **Step 6: Verify build**

Run in `C:\Users\MED\Documents\ERP_Project\frontend`:
`npx tsc -b && npx vite build`
Expected: build succeeds (no errors, no unused-variable errors — `summary` is still used by the "Accounts" and "Journal Entries" cards).

- [ ] **Step 7: Verify lint**

Run in `C:\Users\MED\Documents\ERP_Project\frontend`:
`npx oxlint`
Expected: `Found 0 warnings and 0 errors.`

- [ ] **Step 8: Commit**

```bash
git add frontend/src/features/reports/TrialBalanceTab.tsx
git commit -m "feat: show account balances in trial balance DR/CR columns"
```

---

## Self-Review Notes

- Spec coverage: table Balance DR/CR columns (Steps 3-4), dash for zero/empty column (Step 4), tfoot totals equal (Steps 1+5), summary cards renamed to balance totals (Steps 1-2), movement visibility preserved in "Recent Journal Entries" (untouched), no backend change (none), no new API calls (none).
- No placeholders: every step contains the exact code.
- Type consistency: `totalBalanceDr`/`totalBalanceCr` computed in Step 1, consumed in Steps 2 and 5 — same names, same semantics.
- Zero-balance accounts (1200, 2000, 5000, 5100, 5200, 6000) show `—` in both columns (Step 4 covers both branches).
- Manual check after HMR: tfoot totals DR and CR are equal on current data.
