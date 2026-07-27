# Payroll Processing

**Service:** finance-service (port 8098)  
**Database:** `finance_db`  
**Entity:** PayrollRecord

---

## 1. Business Procedures

### 1.1 Generate Payroll

**Path:** `/payroll/new`

1. Click **Payroll** in the sidebar
2. Click **+ Generate Payroll**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Period Start | Yes | 2026-07-01 |
   | Period End | Yes | 2026-07-31 |

4. Click **Generate Payroll**

> **Business Rule:** The system:
> 1. Fetches all **ACTIVE** employees from the HRM service
> 2. Creates a `PayrollRecord` for each one
> 3. Calculates: `Net Salary = Gross Salary - Deductions`
> 4. Sets status to **PENDING**

### 1.2 Process Payment

**Path:** `/payroll`

1. View the payroll list
2. Find records with **PENDING** status
3. Click the **Pay** button for each record
4. Status changes to **PAID**, `paymentDate` is set

**Payroll Status Lifecycle:**
```
PENDING → PAID
```

### 1.3 View Payroll Records

**Path:** `/payroll`

Table shows: Employee name, Gross Salary, Deductions, Net Salary, Pay Period (start → end), Status (color-coded).

---

## 2. API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/payroll` | List all payroll records |
| GET | `/api/payroll/employee/{employeeId}` | List by employee |
| POST | `/api/payroll` | Generate payroll for a period |
| PATCH | `/api/payroll/{id}/pay` | Process payment (mark PAID) |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Payroll generated | `created_at` on `payroll_records` | View at `/payroll` |
| Payroll paid | Status → PAID, `payment_date` set | Check record status + payment date |
| Net salary | Stored as `net_salary` | Verify: `net_salary = gross_salary - deductions` |

### 3.2 Audit Queries

```sql
-- Full payroll history ordered by creation
SELECT id, employee_id, employee_name, gross_salary, deductions, net_salary,
       pay_period_start, pay_period_end, payment_date, status, created_at
FROM finance_db.public.payroll_records
ORDER BY created_at DESC;

-- Payroll for a specific employee
SELECT id, employee_id, employee_name, gross_salary, net_salary,
       pay_period_start, pay_period_end, status, payment_date
FROM finance_db.public.payroll_records
WHERE employee_id = 'EMP-001'
ORDER BY pay_period_start DESC;

-- Unpaid payroll records (still PENDING)
SELECT id, employee_id, employee_name, net_salary, pay_period_start, pay_period_end, created_at
FROM finance_db.public.payroll_records
WHERE status = 'PENDING'
ORDER BY pay_period_start;
```

### 3.3 Payroll Accuracy Verification

Cross-reference payroll with employee salaries:

```sql
-- Verify every payroll gross_salary matches the employee's current salary
SELECT pr.id, pr.employee_id, pr.employee_name,
       pr.gross_salary AS payroll_salary,
       e.salary AS current_employee_salary,
       CASE
           WHEN pr.gross_salary = e.salary THEN 'MATCH'
           ELSE 'MISMATCH — INVESTIGATE'
       END AS verification
FROM finance_db.public.payroll_records pr
JOIN hrm_db.public.employees e ON pr.employee_id = e.employee_id
WHERE pr.status = 'PAID';
```

```sql
-- Verify net salary calculation
SELECT id, employee_id, gross_salary, deductions, net_salary,
       (gross_salary - COALESCE(deductions, 0)) AS calculated_net,
       CASE
           WHEN net_salary = (gross_salary - COALESCE(deductions, 0)) THEN 'CORRECT'
           ELSE 'ERROR — RECALCULATE'
       END AS verification
FROM finance_db.public.payroll_records;
```

### 3.4 Reconciliation Queries

```sql
-- Payroll for employees who are no longer ACTIVE (should not happen)
SELECT pr.id, pr.employee_id, pr.employee_name, pr.pay_period_start, pr.pay_period_end, pr.status
FROM finance_db.public.payroll_records pr
LEFT JOIN hrm_db.public.employees e ON pr.employee_id = e.employee_id
WHERE e.status IS DISTINCT FROM 'ACTIVE';

-- Duplicate payroll for the same employee in the same period
SELECT employee_id, employee_name, pay_period_start, pay_period_end, COUNT(*) as count
FROM finance_db.public.payroll_records
GROUP BY employee_id, employee_name, pay_period_start, pay_period_end
HAVING COUNT(*) > 1;

-- Total payroll cost by month
SELECT DATE_TRUNC('month', pay_period_start) AS month,
       SUM(gross_salary) AS total_gross,
       SUM(net_salary) AS total_net,
       COUNT(*) AS employee_count
FROM finance_db.public.payroll_records
WHERE status = 'PAID'
GROUP BY DATE_TRUNC('month', pay_period_start)
ORDER BY month DESC;
```

### 3.5 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| No `created_by` on payroll | Cannot tell who generated payroll | Add `generated_by` to `payroll_records` |
| No `paid_by` on payroll | Cannot tell who processed payment | Add `paid_by` to `payroll_records` |
| No `updated_at` | Cannot see when payroll was modified | `payment_date` partially covers this; add full `updated_at` |
| No payroll run log | Cannot see that payroll for period X was generated | Use a `payroll_run` header table with metadata |
| No pre/post salary snapshot | Salary used at generation time vs current salary may differ | Store a `salary_snapshot` field at generation time |

### 3.6 Manual Audit Checklist

| Frequency | Action |
|---|---|
| **Each payroll run** | Verify net calculation for 3 random records |
| **Each payroll run** | Check all PAID records have a non-null `payment_date` |
| **Monthly** | Run salary-match verification (section 3.3) |
| **Monthly** | Check for unpaid PENDING records older than the current period |
| **Quarterly** | Run total payroll cost by month (section 3.4) |
| **Quarterly** | Verify no duplicate payments per employee per period |

---

## 4. Database Schema

```sql
CREATE TABLE payroll_records (
    id                BIGSERIAL PRIMARY KEY,
    employee_id       VARCHAR(50) NOT NULL,
    employee_name     VARCHAR(255) NOT NULL,
    gross_salary      DECIMAL(19,2) NOT NULL,
    deductions        DECIMAL(19,2) DEFAULT 0,
    net_salary        DECIMAL(19,2) NOT NULL,
    pay_period_start  DATE NOT NULL,
    pay_period_end    DATE NOT NULL,
    payment_date      DATE,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

*Document version 1.0 — July 2026*
