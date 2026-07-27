# Audit Trail Procedures

> How to trace, verify, and reconstruct every business transaction in the ERP system.

## Current Audit Capabilities

The system provides these audit primitives:

| Primitive | Available? | Details |
|---|---|---|
| Creation timestamp (`createdAt`) | Yes — every entity | Records when a record was inserted |
| Sequential numbering | Yes — key entities | Each entity gets a unique human-readable number |
| Status field | Yes — lifecycle entities | Status tracks current state (PENDING → PAID, etc.) |
| Immutable records | Partial | Journal entries are append-only; no PUT endpoint |
| `updatedAt` | **No** | No field tracks when a record was last modified |
| `createdBy` / `updatedBy` | **No** | No field tracks which user performed an action |
| Status change history | **No** | No table logs who changed status and when |
| Before/after values on edits | **No** | Old values are overwritten, not preserved |
| Soft delete | **No** | Records are hard-deleted from the database |
| Dedicated audit log table | **No** | No centralized `audit_log` table |

> **Note:** For a production-compliant audit trail, the gaps marked **No** above require implementation. Below, each procedure identifies what can be traced today and what additional data would strengthen the chain.

---

## Table of Contents

1. [User & Access Audit](#1-user--access-audit)
2. [Product & Inventory Audit](#2-product--inventory-audit)
3. [Order-to-Cash Audit](#3-order-to-cash-audit)
4. [Procurement-to-Pay Audit](#4-procurement-to-pay-audit)
5. [Human Resources Audit](#5-human-resources-audit)
6. [Payroll Audit](#6-payroll-audit)
7. [Financial Accounting Audit](#7-financial-accounting-audit)
8. [Reconciliation Procedures](#8-reconciliation-procedures)
9. [Audit Log Implementation Plan](#9-audit-log-implementation-plan)

---

## 1. User & Access Audit

**Service:** Identity Service  
**Entity:** User (table `users`)

### 1.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| User registration | `createdAt` timestamp on user record | Query `users` table by `username` or `email` |
| User login | JWT token issuance | Token contains `iat` (issued-at) claim — decode the JWT at `jwt.io` |
| Password change | **Not logged** — password hash is overwritten | No audit trail exists for password changes |

### 1.2 Audit Query

To see when a user was created and their role:

```sql
-- All services connect to PostgreSQL 17
SELECT id, username, email, role, created_at
FROM erp_db.public.users
WHERE username = 'admin';
```

### 1.3 JWT Token Decoding (Login Evidence)

The JWT issued at login contains:
```json
{
  "sub": "admin",
  "role": "ADMIN",
  "iat": 1720000000,
  "exp": 1720086400
}
```
- `iat`: timestamp of login
- `exp`: token expiry (24h)
- Decode at `jwt.io` or via command line to audit who logged in and when

### 1.4 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| No `lastLogin` field | Cannot track account usage | Add `lastLogin` timestamp to `users` table |
| No failed login tracking | Cannot detect brute force | Add `loginAttempts` counter and `lockedUntil` fields |
| No password change history | Password changes are invisible | Create `password_history` table |
| No `createdBy` on user | Cannot tell which admin created a user | Add `created_by` foreign key to `users` |

---

## 2. Product & Inventory Audit

**Service:** Product Service, Inventory Service  
**Entities:** Product, Stock

### 2.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Product created | `id` auto-increment + `createdAt` | View product in list at `/products` — creation order is by ascending `id` |
| Product edited | **Overwritten** — no `updatedAt`, no old value saved | Compare PUT request payload with current state (no forward trace) |
| Product deleted | **Record removed** — hard delete | No audit trail exists. The product `id` gap in auto-increment is the only clue |
| Stock initialized | Stock record appears in `/inventory` | View stock record with SKU, quantity, warehouse |
| Stock adjusted | Quantity changes via order or GRN | Stock quantity reflects cumulative effect, but individual adjustments are **not logged** |

### 2.2 Audit Query

```sql
-- View all products, ordered by creation
SELECT id, sku, name, price, created_at
FROM product_db.public.products
ORDER BY created_at DESC;

-- View stock levels
SELECT id, product_sku, quantity, warehouse_location
FROM inventory_db.public.stocks;
```

### 2.3 Tracing a Stock Change

Current limitation — if stock goes from 10 to 7:
1. You can see the current quantity is 7
2. You **cannot** see when the change happened, which order caused it, or who authorized it
3. The only cross-reference is: check `orders` table for the matching SKU around the time of change

### 2.4 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| No stock movement log | Cannot trace why stock changed | Create `stock_movements` table: `id, sku, before_qty, after_qty, reference_type (ORDER/GRN/ADJUSTMENT), reference_id, changed_by, changed_at` |
| No `updatedAt` on product | Cannot tell when product was last edited | Add `updated_at` to `products` table |
| No `updatedBy` on product | Cannot tell who edited the product | Add `updated_by` to `products` table |
| Hard delete on products | Orphaned references in orders/invoices | Implement soft delete (`active` boolean flag) |

---

## 3. Order-to-Cash Audit

**Service:** Order Service, Sales Service, Payment Service  
**Entities:** Customer → Order → Invoice → Payment

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Customer created | `createdAt` on customer record | View customer at `/customers` — creation date visible |
| Customer deleted | **Hard delete** — no trace | No audit trail exists |
| Order created | `orderNumber` unique + `createdAt` on `orders` table | View order at `/orders` |
| Order items | `order_items` table links SKUs to order | Query `order_items` by `order_id` |
| Invoice created | `invoiceNumber` unique + `issuedAt` on `invoices` table | View invoice at `/invoices` |
| Invoice status changed | PATCH updates status **in place** — no history | Can only see current status, not previous |
| Payment processed | `createdAt` on `payments` table | Check payment record with `order_id` |

### 3.2 Full Order-to-Cash Trace (Cross-Service)

To trace a sale from start to finish:

```sql
-- 1. Find the customer
SELECT id, name, email, created_at
FROM sales_db.public.customers
WHERE name = 'Acme Corp';

-- 2. Find orders for this customer
SELECT id, order_number, customer_name, total_amount, status
FROM order_db.public.orders
WHERE customer_name = 'Acme Corp'
ORDER BY id;

-- 3. Find invoices for this customer
SELECT id, invoice_number, total_amount, status, issued_at, due_date
FROM sales_db.public.invoices
WHERE customer_id = (SELECT id FROM sales_db.public.customers WHERE name = 'Acme Corp');

-- 4. Find payments for the order
SELECT id, order_id, amount, status, payment_method, created_at
FROM payment_db.public.payments
WHERE order_id = (SELECT id FROM order_db.public.orders WHERE order_number = 'ORD-001');
```

### 3.3 Invoice Status Audit

Since status changes are overwritten (no history table):

```sql
-- Current state only
SELECT id, invoice_number, status, issued_at
FROM sales_db.public.invoices;
```

To reconstruct history, you would need application-level logging or a `status_history` table.

### 3.4 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| No order status history | Cannot see when order was CONFIRMED → SHIPPED → DELIVERED | Create `order_status_history` table: `id, order_id, from_status, to_status, changed_by, changed_at` |
| No invoice status history | Cannot see when invoice went PENDING → OVERDUE → PAID | Create `invoice_status_history` table |
| No `createdBy` on orders | Cannot tell which staff member created the order | Add `created_by` to `orders` |
| Hard delete on customers | Orphaned invoice references | Implement soft delete (`active` flag) |

---

## 4. Procurement-to-Pay Audit

**Service:** Procurement Service, Inventory Service  
**Entities:** Vendor → Purchase Order → Goods Received Note

### 4.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Vendor created | `createdAt` on vendor record | View vendor at `/vendors` |
| PO created | `poNumber` unique + `orderedAt` on `purchase_orders` | View PO at `/purchase-orders` |
| PO status changed | PATCH updates status in place — **no history** | Can only see current status (APPROVED / CANCELLED) |
| GRN created | `grnNumber` unique + `receivedAt` on `goods_received_notes` | View GRN through API |
| Stock increased | GRN creation triggers stock increment | Cross-reference GRN with inventory stock levels |

### 4.2 Full Procurement Trace (Cross-Service)

```sql
-- 1. Find the vendor
SELECT id, name, email, created_at
FROM procurement_db.public.vendors
WHERE name = 'Office Supplies Inc.';

-- 2. Find POs for this vendor
SELECT id, po_number, total_amount, status, ordered_at
FROM procurement_db.public.purchase_orders
WHERE vendor_id = (SELECT id FROM procurement_db.public.vendors WHERE name = 'Office Supplies Inc.');

-- 3. Find GRNs for the PO
SELECT id, grn_number, status, received_at
FROM procurement_db.public.goods_received_notes
WHERE purchase_order_id = (SELECT id FROM procurement_db.public.purchase_orders WHERE po_number = 'PO-001');

-- 4. Verify stock was updated
SELECT id, product_sku, quantity, warehouse_location
FROM inventory_db.public.stocks
WHERE product_sku IN (
    SELECT product_sku FROM inventory_db.public.stocks
    WHERE id IN (...)
);
```

### 4.3 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| No PO status history | Cannot see PO lifecycle (PENDING → APPROVED → RECEIVED) | Create `po_status_history` table |
| No `createdBy` on PO/GRN | Cannot tell who placed the PO or received goods | Add `created_by` to both entities |
| No GRN line items table | Cannot tell exactly which SKUs and quantities were received | Create `grn_items` table (similar to `order_items`) |
| No `updatedAt` on vendor | Cannot tell when vendor details were updated | Add `updated_at` to `vendors` |

---

## 5. Human Resources Audit

**Service:** HRM Service  
**Entities:** Employee, Attendance, Leave

### 5.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Employee created | `employeeId` unique + `createdAt` + `hireDate` | View employee at `/employees` |
| Employee edited | **Overwritten** — no `updatedAt` | Cannot see what changed or when |
| Employee deleted | **Hard delete** — no trace | No audit trail exists |
| Attendance recorded | `date` + `clockIn` + `clockOut` on `attendance` table | Query attendance by `employee_id` |
| Leave submitted | `createdAt` on `leaves` table + status PENDING | View leave through API |
| Leave approved/rejected | Status changes in place — **no history** | Can only see current status |

### 5.2 HR Audit Queries

```sql
-- Employee creation timeline
SELECT id, employee_id, first_name, last_name, department, position, salary, status, hire_date, created_at
FROM hrm_db.public.employees
ORDER BY created_at DESC;

-- Attendance for a specific employee
SELECT id, date, clock_in, clock_out, status
FROM hrm_db.public.attendance
WHERE employee_id = (SELECT id FROM hrm_db.public.employees WHERE employee_id = 'EMP-001')
ORDER BY date DESC;

-- Leave requests and their current status
SELECT id, employee_id, leave_type, start_date, end_date, status, reason, created_at
FROM hrm_db.public.leaves
ORDER BY created_at DESC;
```

### 5.3 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| Salary change history | Salary edits are invisible — payroll uses current salary | Add `salary_history` table: `employee_id, old_salary, new_salary, changed_by, changed_at` |
| No `updatedAt` on employee | Cannot detect unauthorized profile edits | Add `updated_at` to `employees` |
| No leave status history | Cannot see who approved/rejected a leave | Add `leave_status_history` table with `changed_by` |
| No `createdBy` on leave approval | Cannot know which manager approved | Add `approved_by` to `leaves` table |
| Hard delete on employees | Payroll/attendance records become orphaned | Implement soft delete (`status = TERMINATED`) — this already exists! Use it instead of deleting |

---

## 6. Payroll Audit

**Service:** Finance Service  
**Entity:** PayrollRecord

### 6.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Payroll generated | `createdAt` on `payroll_records` | View `/payroll` — each record has a creation timestamp |
| Payroll paid | Status changes PENDING → PAID, `paymentDate` set | Click "Pay" — status updates in place |
| Net salary calculation | Formula: `netSalary = grossSalary - deductions` | Gross and deductions are stored; net is computed on creation |

### 6.2 Payroll Audit Query

```sql
-- Full payroll history
SELECT id, employee_id, employee_name, gross_salary, deductions, net_salary,
       pay_period_start, pay_period_end, payment_date, status, created_at
FROM finance_db.public.payroll_records
ORDER BY created_at DESC;

-- Payroll by employee
SELECT * FROM finance_db.public.payroll_records
WHERE employee_id = 'EMP-001'
ORDER BY pay_period_start DESC;
```

### 6.3 Cross-Reference: Payroll → Employee

To verify payroll accuracy:

```sql
-- Check that payroll used the correct salary
SELECT pr.id, pr.employee_id, pr.gross_salary, e.salary AS current_salary,
       pr.gross_salary = e.salary AS salary_matches
FROM finance_db.public.payroll_records pr
JOIN hrm_db.public.employees e ON pr.employee_id = e.employee_id;
```

### 6.4 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| No `updatedAt` | Cannot tell when payroll was paid (use `paymentDate` instead) | `paymentDate` partially covers this, but add `paid_at` and `paid_by` |
| No `createdBy` | Cannot tell who generated payroll | Add `generated_by` to `payroll_records` |
| No `paidBy` | Cannot tell who processed the payment | Add `paid_by` to `payroll_records` |
| No payroll run log | Cannot see that payroll was generated for period X | Current records serve as the log; add a `payroll_run` header table |

---

## 7. Financial Accounting Audit

**Service:** Finance Service  
**Entities:** Account (Chart of Accounts), JournalEntry

### 7.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Account created | `createdAt` on `accounts` table | View account at `/accounts` |
| Account edited | **Overwritten** — no `updatedAt` | Old name/code/type not preserved |
| Account deleted | **Hard delete** — no trace | Journal entries referencing this account become orphaned |
| Journal entry created | `entryNumber` unique + `createdAt` + `entryDate` | View at `/journal` — entries are **immutable** (no PUT) |
| Journal lines | Each entry stores `debit`, `credit`, and `account` per line | Balance check: sum of debits = sum of credits |

### 7.2 Double-Entry Verification (Critical Audit Control)

The journal entry system has a built-in audit control: **debits must equal credits**.

To verify all entries are balanced:

```sql
-- Check each journal entry for balance
SELECT je.id, je.entry_number, je.description, je.entry_date, je.created_at,
       SUM(je.debit) AS total_debit,
       SUM(je.credit) AS total_credit,
       CASE WHEN SUM(je.debit) = SUM(je.credit) THEN 'BALANCED' ELSE 'UNBALANCED' END AS status
FROM finance_db.public.journal_entries je
GROUP BY je.id, je.entry_number, je.description, je.entry_date, je.created_at
HAVING SUM(je.debit) <> SUM(je.credit);
```
> If this query returns any rows, the accounting books are **out of balance** — immediate investigation required.

### 7.3 Account Activity Audit

```sql
-- Show all journal entries for a specific account
SELECT je.id, je.entry_number, je.description AS entry_desc,
       je.debit, je.credit, je.entry_date, a.account_code, a.account_name
FROM finance_db.public.journal_entries je
JOIN finance_db.public.accounts a ON je.account_id = a.id
WHERE a.account_code = '1000'
ORDER BY je.entry_date DESC;
```

### 7.4 Audit Gaps & Recommendations

| Gap | Risk | Recommendation |
|---|---|---|
| No `updatedAt` on accounts | Account changes invisible | Add `updated_at` to `accounts` |
| Hard delete on accounts | Journal entries lose account reference | Implement soft delete or prevent deletion if journal lines exist |
| No `createdBy` on journal entries | Cannot tell who posted the entry | Add `posted_by` to `journal_entries` |
| No posting period lock | Entries can be backdated or posted to closed periods | Add `fiscal_period` with open/close status |

---

## 8. Reconciliation Procedures

### 8.1 Order-to-Cash Reconciliation

Reconcile that every order has a corresponding invoice and payment:

```sql
-- Orders without invoices
SELECT o.order_number, o.customer_name, o.total_amount, o.status
FROM order_db.public.orders o
LEFT JOIN sales_db.public.invoices i ON i.customer_id = (
    SELECT id FROM sales_db.public.customers WHERE name = o.customer_name LIMIT 1
)
WHERE i.id IS NULL;
```

### 8.2 Procurement Reconciliation

Reconcile that every PO with RECEIVED status has a GRN:

```sql
-- POs marked RECEIVED but without a GRN
SELECT po.po_number, po.vendor_id, po.total_amount, po.ordered_at
FROM procurement_db.public.purchase_orders po
LEFT JOIN procurement_db.public.goods_received_notes grn
    ON grn.purchase_order_id = po.id
WHERE po.status = 'RECEIVED' AND grn.id IS NULL;
```

### 8.3 Payroll-to-Employee Reconciliation

Verify every payroll record references a valid, active employee:

```sql
-- Payroll records for non-existent or terminated employees
SELECT pr.id, pr.employee_id, pr.employee_name, pr.pay_period_start, pr.pay_period_end, pr.status
FROM finance_db.public.payroll_records pr
LEFT JOIN hrm_db.public.employees e ON pr.employee_id = e.employee_id
WHERE e.id IS NULL OR e.status != 'ACTIVE';
```

### 8.4 Stock Integrity Check

Verify stock quantities are consistent with order/GRN activity:

```sql
-- Stock that went negative (impossible in physical inventory)
SELECT product_sku, quantity
FROM inventory_db.public.stocks
WHERE quantity < 0;
```

### 8.5 Manual Audit Checklist

| Frequency | Procedure |
|---|---|
| **Daily** | Review all journal entries created today at `/journal` — verify debits = credits |
| **Daily** | Check `/invoices` for any OVERDUE invoices |
| **Weekly** | Run Order-to-Cash reconciliation query (section 8.1) |
| **Weekly** | Run Procurement reconciliation query (section 8.2) |
| **Monthly** | Run Payroll-to-Employee reconciliation (section 8.3) |
| **Monthly** | Run Stock integrity check (section 8.4) |
| **Monthly** | Download all `/payroll` records and verify net calculations: `netSalary = grossSalary - deductions` |
| **Monthly** | Review `/employees` list — verify all TERMINATED employees have final payroll processed |
| **Quarterly** | Full cross-service reconciliation — trace 5 random transactions end-to-end |

---

## 9. Audit Log Implementation Plan

To move from basic to production-grade audit trail, implement in priority order:

### Phase 1 — Immediate (Low Effort, High Impact)

| Change | Files to Modify |
|---|---|
| Add `updated_at` to all entities | Every entity class — add `private LocalDateTime updatedAt;` with `@PreUpdate` |
| Add `created_by` to all entities | Every entity class — add `private String createdBy;` |
| Add `updated_by` to all entities | Every entity class — add `private String updatedBy;` |
| Extract username from JWT in gateway | Pass `X-User-Name` header from gateway to downstream services |
| Add `stock_movements` table | Inventory service — new entity + repository |

### Phase 2 — Medium Term

| Change | Description |
|---|---|
| Status history tables | `order_status_history`, `invoice_status_history`, `po_status_history`, `leave_status_history` |
| Soft delete for all entities | Replace `DELETE` endpoints with `PATCH deactivated_at` |
| `audit_log` table | Centralized table: `id, entity_type, entity_id, action (CREATE/UPDATE/DELETE), before_json, after_json, performed_by, performed_at` |
| AOP Audit Aspect | Use Spring AOP `@Auditable` annotation on service methods to auto-log to `audit_log` |

### Phase 3 — Advanced

| Change | Description |
|---|---|
| Immutable journal entries | Enforce: no PUT/DELETE on posted journal entries; only reversing entries via new entry |
| Fiscal period locking | Prevent journal entries in closed accounting periods |
| Digital signatures | Hash-chaining of sequential audit log entries to prevent tampering |
| Audit dashboard | Frontend page `/audit` with searchable, filterable audit log viewer |

---

## Appendix: Entity Timestamp Summary

| Entity | Database | `createdAt` | `updatedAt` | `createdBy` | `updatedBy` |
|---|---|---|---|---|---|
| User | `erp_db` | ✅ | ❌ | ❌ | ❌ |
| Product | `product_db` | ❌ | ❌ | ❌ | ❌ |
| Stock | `inventory_db` | ❌ | ❌ | ❌ | ❌ |
| Order | `order_db` | ❌ | ❌ | ❌ | ❌ |
| Payment | `payment_db` | ✅ | ❌ | ❌ | ❌ |
| Customer | `sales_db` | ✅ | ❌ | ❌ | ❌ |
| Invoice | `sales_db` | ❌ (has `issuedAt`) | ❌ | ❌ | ❌ |
| Vendor | `procurement_db` | ✅ | ❌ | ❌ | ❌ |
| PurchaseOrder | `procurement_db` | ❌ (has `orderedAt`) | ❌ | ❌ | ❌ |
| GoodsReceivedNote | `procurement_db` | ❌ (has `receivedAt`) | ❌ | ❌ | ❌ |
| Employee | `hrm_db` | ✅ | ❌ | ❌ | ❌ |
| Attendance | `hrm_db` | ❌ | ❌ | ❌ | ❌ |
| Leave | `hrm_db` | ✅ | ❌ | ❌ | ❌ |
| Account | `finance_db` | ✅ | ❌ | ❌ | ❌ |
| JournalEntry | `finance_db` | ✅ | ❌ | ❌ | ❌ |
| PayrollRecord | `finance_db` | ✅ | ❌ | ❌ | ❌ |

---

*Document version 1.0 — July 2026*
