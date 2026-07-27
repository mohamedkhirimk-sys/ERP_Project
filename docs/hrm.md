# Human Resources Management

**Service:** hrm-service (port 8097)  
**Database:** `hrm_db`  
**Entities:** Employee, Attendance, Leave

---

## 1. Business Procedures

### 1.1 Employee Management

#### Create an Employee

**Path:** `/employees/new`

1. Click **Employees** in the sidebar
2. Click **+ New Employee**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Employee ID | Yes | "EMP-001" |
   | First Name | Yes | "John" |
   | Last Name | Yes | "Doe" |
   | Email | Yes | "john@company.com" |
   | Phone | No | "+1 555-0300" |
   | Department | No | "Engineering" |
   | Position | No | "Software Engineer" |
   | Salary | No | 75000 |
   | Status | Yes | ACTIVE |

4. Click **Save Employee**

#### Edit an Employee

**Path:** `/employees/{id}/edit`

1. In the employee list, click the **edit (pencil)** icon
2. Modify fields
3. Click **Save Employee**

#### Delete an Employee

1. Click the **delete (trash)** icon in the employee list
2. Confirm deletion
3. **Note:** Consider setting status to **TERMINATED** instead of deleting to preserve records

### 1.2 Attendance Tracking

**Not available in the frontend UI** — API only.

- `POST /api/attendance` — record clock-in/clock-out

  ```json
  {
    "employeeId": "EMP-001",
    "date": "2026-07-23",
    "clockIn": "08:00",
    "clockOut": "17:00"
  }
  ```

- `GET /api/attendance/employee/{employeeId}` — view history

### 1.3 Leave Management

**Not available in the frontend UI** — API only.

#### Submit a Leave Request

`POST /api/leaves`

```json
{
  "employeeId": "EMP-001",
  "leaveType": "ANNUAL",
  "startDate": "2026-08-01",
  "endDate": "2026-08-05",
  "reason": "Family vacation"
}
```

Status defaults to **PENDING**.

#### Approve / Reject Leave

`PATCH /api/leaves/{id}/status?status=APPROVED`

Status lifecycle: `PENDING → APPROVED / REJECTED`

---

## 2. API Reference

### Employees

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/employees` | List all employees |
| GET | `/api/employees/{id}` | Get employee by ID |
| POST | `/api/employees` | Create employee |
| PUT | `/api/employees/{id}` | Update employee |
| DELETE | `/api/employees/{id}` | Delete employee |

### Attendance

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/attendance` | List all attendance |
| GET | `/api/attendance/employee/{employeeId}` | List by employee |
| POST | `/api/attendance` | Record attendance |

### Leaves

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/leaves` | List all leaves |
| GET | `/api/leaves/employee/{employeeId}` | List by employee |
| POST | `/api/leaves` | Submit leave request |
| PATCH | `/api/leaves/{id}/status` | Approve/reject leave |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Employee created | `employee_id` unique + `created_at` + `hire_date` | View at `/employees` |
| Employee edited | ❌ Overwritten — no `updated_at` | Cannot see what or when |
| Employee deleted | ❌ Hard delete — record removed | No trace |
| Salary changed | ❌ Old salary overwritten | Only current salary visible |
| Attendance recorded | `date`, `clock_in`, `clock_out` | Query by employee |
| Leave submitted | `created_at` + status PENDING | View leave by employee |
| Leave approved/rejected | ❌ Status overwritten — no history | Only current status visible |

### 3.2 Audit Queries

```sql
-- Employee creation timeline
SELECT id, employee_id, first_name, last_name, department, position,
       salary, status, hire_date, created_at
FROM hrm_db.public.employees
ORDER BY created_at DESC;

-- Attendance for an employee
SELECT id, date, clock_in, clock_out, status
FROM hrm_db.public.attendance
WHERE employee_id = (SELECT id FROM hrm_db.public.employees WHERE employee_id = 'EMP-001')
ORDER BY date DESC;

-- Leave requests for an employee
SELECT id, employee_id, leave_type, start_date, end_date, status, reason, created_at
FROM hrm_db.public.leaves
WHERE employee_id = (SELECT id FROM hrm_db.public.employees WHERE employee_id = 'EMP-001')
ORDER BY created_at DESC;

-- All pending leaves (needing approval)
SELECT l.id, e.first_name, e.last_name, l.leave_type, l.start_date, l.end_date, l.created_at
FROM hrm_db.public.leaves l
JOIN hrm_db.public.employees e ON l.employee_id = e.employee_id
WHERE l.status = 'PENDING'
ORDER BY l.created_at;
```

### 3.3 Reconciliation Queries

```sql
-- Employees with no attendance records (possible ghost employees)
SELECT e.employee_id, e.first_name, e.last_name, e.department, e.hire_date
FROM hrm_db.public.employees e
LEFT JOIN hrm_db.public.attendance a ON a.employee_id = e.id
WHERE a.id IS NULL AND e.status = 'ACTIVE';

-- Employees on leave during payroll period (no pay due?)
SELECT e.employee_id, e.first_name, e.last_name, l.start_date, l.end_date, l.leave_type
FROM hrm_db.public.employees e
JOIN hrm_db.public.leaves l ON l.employee_id = e.id
WHERE l.status = 'APPROVED'
  AND l.start_date <= '2026-07-31'
  AND l.end_date >= '2026-07-01';

-- Attendance for terminated employees (should not happen)
SELECT a.date, a.clock_in, a.clock_out
FROM hrm_db.public.attendance a
JOIN hrm_db.public.employees e ON a.employee_id = e.id
WHERE e.status = 'TERMINATED';
```

### 3.4 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| Salary change history | Payroll uses current salary — silent overpayments | Create `salary_history` table: `employee_id, old_salary, new_salary, changed_by, changed_at, reason` |
| No `updated_at` on employee | Unauthorized profile edits invisible | Add `updated_at` to `employees` |
| No leave status history | Cannot see who approved/rejected | Create `leave_status_history` with `changed_by` |
| No `approved_by` on leave | Manager approval not recorded | Add `approved_by` + `approved_at` to `leaves` |
| No `created_by` on employee | Cannot tell who onboarded | Add `created_by` to `employees` |
| Hard delete on employees | Payroll/attendance orphaned | Use status=TERMINATED instead of delete |

---

## 4. Database Schemas

```sql
CREATE TABLE employees (
    id            BIGSERIAL PRIMARY KEY,
    employee_id   VARCHAR(50) NOT NULL UNIQUE,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(50),
    department    VARCHAR(100),
    position      VARCHAR(100),
    salary        DECIMAL(19,2),
    hire_date     DATE,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE attendance (
    id         BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    date       DATE NOT NULL,
    clock_in   TIME,
    clock_out  TIME,
    status     VARCHAR(20) NOT NULL DEFAULT 'PRESENT'
);

CREATE TABLE leaves (
    id          BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL REFERENCES employees(id),
    leave_type  VARCHAR(50) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    reason      TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

*Document version 1.0 — July 2026*
