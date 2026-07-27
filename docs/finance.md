# Financial Accounting

**Service:** finance-service (port 8098)  
**Database:** `finance_db`  
**Entities:** Account (Chart of Accounts), JournalEntry

---

## 1. Business Procedures

### 1.1 Chart of Accounts

#### Create an Account

**Path:** `/accounts/new`

1. Click **Accounts** in the sidebar
2. Click **+ New Account**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Account Code | Yes | "1000" |
   | Account Name | Yes | "Cash & Bank" |
   | Type | Yes | ASSET |
   | Description | No | "Primary operating account" |
   | Opening Balance | No | 50000.00 |

4. Click **Save Account**

#### Edit an Account

**Path:** `/accounts/{id}/edit`

1. In the account list, click the **edit (pencil)** icon
2. Modify fields
3. Click **Save Account**

#### Delete an Account

1. Click the **delete (trash)** icon
2. Confirm deletion
3. **Warning:** Deleting an account with existing journal entries will orphan those entries

#### Standard Account Types

| Type | Code Range | Examples |
|---|---|---|
| **ASSET** | 1000–1999 | Cash, Bank, Accounts Receivable, Inventory, Equipment |
| **LIABILITY** | 2000–2999 | Accounts Payable, Loans, Accrued Expenses |
| **EQUITY** | 3000–3999 | Owner's Capital, Retained Earnings, Drawings |
| **REVENUE** | 4000–4999 | Sales Revenue, Service Income, Interest Income |
| **EXPENSE** | 5000–5999 | Salaries, Rent, Utilities, Cost of Goods Sold |

#### Recommended Chart of Accounts

| Code | Name | Type |
|---|---|---|
| 1000 | Cash & Bank | ASSET |
| 1100 | Accounts Receivable | ASSET |
| 1200 | Inventory | ASSET |
| 1300 | Fixed Assets | ASSET |
| 2000 | Accounts Payable | LIABILITY |
| 2100 | Accrued Liabilities | LIABILITY |
| 3000 | Owner's Equity | EQUITY |
| 3100 | Retained Earnings | EQUITY |
| 4000 | Sales Revenue | REVENUE |
| 4100 | Service Revenue | REVENUE |
| 5000 | Salaries Expense | EXPENSE |
| 5100 | Rent Expense | EXPENSE |
| 5200 | Utilities Expense | EXPENSE |
| 5300 | Cost of Goods Sold | EXPENSE |

### 1.2 Journal Entries (Double-Entry Bookkeeping)

**Path:** `/journal/new`

1. Click **Journal** in the sidebar
2. Click **+ New Entry**
3. Fill in:

   | Section | Field | Required | Example |
   |---|---|---|---|
   | Header | Date | Yes | 2026-07-23 |
   | | Description | Yes | "Record cash sale" |
   | Lines (at least 2) | Account Code | Yes | "1000" |
   | | Description | No | "Cash received" |
   | | Debit | One per line | 1000.00 |
   | | Credit | One per line | |
   | Lines (2nd) | Account Code | Yes | "4000" |
   | | Description | No | "Sales revenue" |
   | | Debit | | |
   | | Credit | One per line | 1000.00 |

4. Verify the **balance indicator** is green (debits = credits)
5. Click **Create Entry**

### 1.3 Journal Entry Rules

| Rule | Why |
|---|---|
| Minimum 2 lines per entry | Every transaction affects at least two accounts |
| Total Debits must equal Total Credits | Fundamental accounting principle |
| Each line is either Debit OR Credit | Not both on the same line |
| Account Code must exist | References the chart of accounts |
| Entries are immutable | No edit/delete after posting (no PUT endpoint) |

### 1.4 Common Journal Entry Examples

**Cash Sale ($1,000):**

| Account | Code | Debit | Credit |
|---|---|---|---|
| Cash & Bank | 1000 | $1,000 | |
| Sales Revenue | 4000 | | $1,000 |

**Pay Rent ($2,000):**

| Account | Code | Debit | Credit |
|---|---|---|---|
| Rent Expense | 5100 | $2,000 | |
| Cash & Bank | 1000 | | $2,000 |

**Purchase Inventory on Credit ($500):**

| Account | Code | Debit | Credit |
|---|---|---|---|
| Inventory | 1200 | $500 | |
| Accounts Payable | 2000 | | $500 |

**Payroll Entry ($7,500 salary, $1,500 deductions, $6,000 net):**

| Account | Code | Debit | Credit |
|---|---|---|---|
| Salaries Expense | 5000 | $7,500 | |
| Cash & Bank | 1000 | | $6,000 |
| Accrued Liabilities | 2100 | | $1,500 |

### 1.5 View Journal Entries

**Path:** `/journal`

Table shows: entry number, account name + code, description, debit, credit, date.

---

## 2. API Reference

### Accounts

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/accounts` | List all accounts |
| GET | `/api/accounts/{id}` | Get account by ID |
| POST | `/api/accounts` | Create account |
| PUT | `/api/accounts/{id}` | Update account |
| DELETE | `/api/accounts/{id}` | Delete account |

### Journal Entries

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/journal-entries` | List all entries |
| GET | `/api/journal-entries/account/{accountId}` | List by account |
| POST | `/api/journal-entries` | Create entry (immutable) |

### Payroll

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/payroll` | List payroll records |
| POST | `/api/payroll` | Generate payroll |
| PATCH | `/api/payroll/{id}/pay` | Process payment |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Account created | `created_at` on `accounts` | View at `/accounts` |
| Account edited | ❌ Overwritten — no `updated_at` | Only current values visible |
| Account deleted | ❌ Hard delete — orphaned references | Journal entries lose account link |
| Journal entry created | `entry_number` unique + `entry_date` + `created_at` | View at `/journal` — entries are immutable |
| Entry balance | Sum(debits) = Sum(credits) per entry | Built-in form validation |

### 3.2 Critical Audit Control: Balance Verification

The most important audit check — every journal entry must be balanced:

```sql
-- Find all unbalanced journal entries (should return ZERO rows)
SELECT je.id, je.entry_number, je.description, je.entry_date,
       SUM(je.debit) AS total_debit,
       SUM(je.credit) AS total_credit,
       CASE WHEN SUM(je.debit) = SUM(je.credit) THEN 'BALANCED' ELSE 'UNBALANCED' END AS status
FROM finance_db.public.journal_entries je
GROUP BY je.id, je.entry_number, je.description, je.entry_date
HAVING SUM(je.debit) <> SUM(je.credit);
```

> **If this returns any rows: immediate investigation required — the books are out of balance.**

### 3.3 Account Activity Queries

```sql
-- All journal entries for a specific account
SELECT je.id, je.entry_number, je.description AS entry_desc,
       je.debit, je.credit, je.entry_date,
       a.account_code, a.account_name
FROM finance_db.public.journal_entries je
JOIN finance_db.public.accounts a ON je.account_id = a.id
WHERE a.account_code = '1000'
ORDER BY je.entry_date DESC;

-- Current balance of all accounts (from opening balances + journal entries)
SELECT a.account_code, a.account_name, a.account_type, a.balance AS opening_balance,
       COALESCE(SUM(je.debit), 0) AS total_debits,
       COALESCE(SUM(je.credit), 0) AS total_credits,
       a.balance + COALESCE(SUM(je.debit), 0) - COALESCE(SUM(je.credit), 0) AS calculated_balance
FROM finance_db.public.accounts a
LEFT JOIN finance_db.public.journal_entries je ON je.account_id = a.id
GROUP BY a.id, a.account_code, a.account_name, a.account_type, a.balance
ORDER BY a.account_code;

-- Trial balance (all accounts with net balance)
SELECT a.account_code, a.account_name, a.account_type,
       SUM(je.debit) AS total_debits,
       SUM(je.credit) AS total_credits,
       SUM(je.debit) - SUM(je.credit) AS net_balance
FROM finance_db.public.accounts a
LEFT JOIN finance_db.public.journal_entries je ON je.account_id = a.id
GROUP BY a.id, a.account_code, a.account_name, a.account_type
HAVING SUM(je.debit) <> SUM(je.credit)
ORDER BY a.account_code;
```

### 3.4 Account Usage Analysis

```sql
-- Accounts that have never been used in a journal entry
SELECT a.account_code, a.account_name, a.account_type
FROM finance_db.public.accounts a
LEFT JOIN finance_db.public.journal_entries je ON je.account_id = a.id
WHERE je.id IS NULL;

-- Most active accounts (highest transaction volume)
SELECT a.account_code, a.account_name, COUNT(je.id) AS transaction_count,
       SUM(je.debit) AS total_debits,
       SUM(je.credit) AS total_credits
FROM finance_db.public.accounts a
JOIN finance_db.public.journal_entries je ON je.account_id = a.id
GROUP BY a.id, a.account_code, a.account_name
ORDER BY transaction_count DESC
LIMIT 10;
```

### 3.5 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| No `updated_at` on accounts | Account name/code changes invisible | Add `updated_at` to `accounts` |
| No `created_by` on journal entries | Cannot tell who posted the entry | Add `posted_by` to `journal_entries` |
| Hard delete on accounts | Journal lines lose account reference | Prevent deletion if journal lines exist, or implement soft delete |
| No fiscal period | Entries can be backdated to closed periods | Add `fiscal_period` table with `is_open` flag |
| No posting date validation | Entries can be dated in the future | Add validation: `entry_date <= NOW()` |
| Account balance not auto-calculated | `balance` field is manually set | Compute balance from opening + SUM of journal entries |

---

## 4. Database Schemas

```sql
CREATE TABLE accounts (
    id           BIGSERIAL PRIMARY KEY,
    account_code VARCHAR(20) NOT NULL UNIQUE,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    description  TEXT,
    balance      DECIMAL(19,2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE journal_entries (
    id           BIGSERIAL PRIMARY KEY,
    entry_number VARCHAR(50) NOT NULL UNIQUE,
    description  VARCHAR(255),
    debit        DECIMAL(19,2) NOT NULL DEFAULT 0,
    credit       DECIMAL(19,2) NOT NULL DEFAULT 0,
    account_id   BIGINT NOT NULL REFERENCES accounts(id),
    entry_date   TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

*Document version 1.0 — July 2026*
