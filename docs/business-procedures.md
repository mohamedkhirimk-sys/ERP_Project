# ERP Business Procedures

## Table of Contents
1. [User & Access Management](#1-user--access-management)
2. [Product & Inventory Management](#2-product--inventory-management)
3. [Order-to-Cash (Sales Cycle)](#3-order-to-cash-sales-cycle)
4. [Procurement-to-Pay (Purchase Cycle)](#4-procurement-to-pay-purchase-cycle)
5. [Human Resources Management](#5-human-resources-management)
6. [Payroll Processing](#6-payroll-processing)
7. [Financial Accounting](#7-financial-accounting)
8. [End-to-End Process Flows](#8-end-to-end-process-flows)

---

## 1. User & Access Management

**Module:** Identity Service  
**Entities:** User (ADMIN / STAFF roles)

### Purpose
Control who can access the ERP system and what they can do.

### Procedures

#### 1.1 Login
- Navigate to `/login`
- Enter username and password
- System validates credentials against the database, returns a JWT token
- Token is stored in browser localStorage and sent with every API request
- Default accounts: `admin / admin123` (full access), `staff / staff123` (limited access)

#### 1.2 Role-Based Access
- **ADMIN**: Can access all modules — create/edit/delete products, orders, employees, accounts, users
- **STAFF**: Restricted access — typically view-only or limited operations

---

## 2. Product & Inventory Management

**Module:** Product Service + Inventory Service  
**Entities:** Product, Stock

### Purpose
Maintain product catalog and track stock quantities in warehouses.

### Procedures

#### 2.1 Create a Product
- Navigate to `/products/new`
- Fill in: Name, SKU (unique product code), Price, Stock Quantity
- Submit — product is created in the database

#### 2.2 Initialize Stock
- Navigate to `/inventory`
- Click "Initialize Stock"
- Enter: Product SKU, Quantity, Warehouse Location (optional)
- Submit — stock record created for that SKU

#### 2.3 Adjust Stock Levels
- System automatically adjusts stock when:
  - An order is placed (stock decreases)
  - A Goods Received Note is created (stock increases)
- Manual adjustments can be made via the API (PUT `/api/inventory/stock/{sku}`)

#### 2.4 View Stock Levels
- Navigate to `/inventory`
- Table shows: SKU, quantity (color-coded: green >10, yellow >0, red 0), warehouse

---

## 3. Order-to-Cash (Sales Cycle)

**Module:** Sales Service + Order Service + Payment Service  
**Entities:** Customer → Order → Invoice → Payment

### Purpose
Manage the complete sales cycle from customer order to payment collection.

### Procedures

#### 3.1 Customer Management

**Create a Customer:**
- Navigate to `/customers`
- Click "New Customer"
- Enter: Name, Email, Phone, Address
- Submit — customer saved

**View/Manage Customers:**
- Table shows all customers
- Click × to delete a customer

#### 3.2 Create an Order
- Navigate to `/orders/new`
- Enter: Customer Name, Total Amount
- Select: Status (PENDING / CONFIRMED / SHIPPED / DELIVERED)
- Select: Payment Method (CREDIT_CARD / BANK_TRANSFER / CASH)
- Add line items: SKU + Quantity
- Click "Create Order"

> **Business Rule:** When an order is created, stock quantities are reduced in the inventory service.

#### 3.3 Create an Invoice
- Navigate to `/invoices/new`
- Select: Customer (from the customer list)
- Enter: Total Amount, Due Date
- Select: Status (PENDING / PAID / CANCELLED)
- Add line items: SKU + Quantity
- Click "Create Invoice"

#### 3.4 Invoice Lifecycle
| Status | Meaning |
|---|---|
| PENDING | Invoice issued, awaiting payment |
| PAID | Payment received |
| OVERDUE | Past due date, not yet paid |
| CANCELLED | Invoice voided |

- To update status: use PATCH `/api/invoices/{id}/status?status=PAID`

#### 3.5 Payment Processing
- POST `/api/payments` with orderId and amount
- System processes the payment, updates payment status
- External webhook at `/api/webhooks/order-status` can update order status from external systems

---

## 4. Procurement-to-Pay (Purchase Cycle)

**Module:** Procurement Service  
**Entities:** Vendor → Purchase Order → Goods Received Note

### Purpose
Manage purchasing from vendors, from ordering to receiving goods.

### Procedures

#### 4.1 Vendor Management

**Create a Vendor:**
- Navigate to `/vendors`
- Click "New Vendor"
- Enter: Name, Email, Phone, Address
- Submit

#### 4.2 Create a Purchase Order
- Navigate to `/purchase-orders/new`
- Select: Vendor
- Enter: Total Amount
- Select: Status (PENDING / APPROVED / CANCELLED)
- Click "Create PO"

**PO Status Lifecycle:**
```
PENDING → APPROVED → RECEIVED (via GRN)
                    → CANCELLED
```

#### 4.3 Receive Goods (GRN)
- Navigate to `/goods-received/new`
- Select: Purchase Order (from the list of POs)
- Select: Status (RECEIVED / PARTIAL / CANCELLED)
- Add items: SKU + Quantity received
- Click "Create GRN"

> **Business Rule:** Creating a GRN:
> 1. Updates the PO status to RECEIVED
> 2. Increases stock quantity in the inventory service

#### 4.4 View Purchase Orders
- Navigate to `/purchase-orders`
- Table shows: PO number, vendor, amount, status (color-coded), order date

---

## 5. Human Resources Management

**Module:** HRM Service  
**Entities:** Employee, Attendance, Leave

### Purpose
Manage employee records, track attendance, and handle leave requests.

### Procedures

#### 5.1 Employee Management

**Create an Employee:**
- Navigate to `/employees/new`
- Fill in: Employee ID, First Name, Last Name, Email, Phone, Department, Position, Salary
- Select: Status (ACTIVE / INACTIVE / TERMINATED)
- Submit

**Edit an Employee:**
- In the employee list, click the edit (pencil) icon on any row
- Update fields and save

**Delete an Employee:**
- In the employee list, click the delete (trash) icon
- Confirm deletion

#### 5.2 Attendance Tracking
- POST `/api/attendance` with employeeId, clockIn/clockOut times
- Attendance status defaults to PRESENT
- View attendance history: GET `/api/attendance/employee/{employeeId}`

#### 5.3 Leave Management

**Submit a Leave Request:**
- POST `/api/leaves` with employeeId, leaveType, startDate, endDate, reason
- Status defaults to PENDING

**Approve/Reject Leave:**
- PATCH `/api/leaves/{id}/status?status=APPROVED`
- Possible statuses: PENDING → APPROVED / REJECTED

---

## 6. Payroll Processing

**Module:** Finance Service  
**Entities:** PayrollRecord

### Purpose
Generate and process employee payroll for a given pay period.

### Procedures

#### 6.1 Generate Payroll
- Navigate to `/payroll/new`
- Enter: Period Start, Period End
- Click "Generate Payroll"

> **Business Rule:** Generating payroll:
> 1. Fetches all ACTIVE employees from the HRM service
> 2. Creates a PayrollRecord for each employee
> 3. Auto-calculates: Net Salary = Gross Salary - Deductions
> 4. Default status: PENDING

#### 6.2 Process Payment
- Navigate to `/payroll`
- Find PENDING records
- Click "Pay" button for each record
- Status changes to PAID

**Payroll Status Lifecycle:**
```
PENDING → PAID
```

#### 6.3 View Payroll Records
- Table shows: Employee name, Gross Salary, Deductions, Net Salary, Pay Period, Status

---

## 7. Financial Accounting

**Module:** Finance Service  
**Entities:** Account (Chart of Accounts), JournalEntry

### Purpose
Maintain the chart of accounts and record double-entry bookkeeping transactions.

### Procedures

#### 7.1 Chart of Accounts

**Create an Account:**
- Navigate to `/accounts/new`
- Enter: Account Code (e.g., 1000), Account Name, Description
- Select: Type (ASSET / LIABILITY / EQUITY / REVENUE / EXPENSE)
- Enter: Opening Balance
- Submit

**Account Types (Standard Classification):**
| Type | Examples |
|---|---|
| ASSET | Cash, Bank, Accounts Receivable, Inventory |
| LIABILITY | Accounts Payable, Loans |
| EQUITY | Owner's Capital, Retained Earnings |
| REVENUE | Sales Revenue, Service Income |
| EXPENSE | Salaries, Rent, Utilities |

**Manage Accounts:**
- View all accounts at `/accounts`
- Edit (pencil icon) or delete (trash icon) from the list

#### 7.2 Journal Entries (Double-Entry Bookkeeping)

**Create a Journal Entry:**
- Navigate to `/journal/new`
- Enter: Entry Date, Description
- Add lines — each line has:
  - **Account Code** (must exist in chart of accounts)
  - **Description** of the line
  - **Debit** amount OR **Credit** amount (not both on the same line)
- Verify the totals: **Total Debits must equal Total Credits** (the form shows a balance indicator — green when balanced, red when unbalanced)

**Rules:**
- Every journal entry must have at least 2 lines
- Sum of all Debits = Sum of all Credits
- Each line is either a Debit or a Credit to a specific account
- Red indicator warns when the entry is unbalanced

**Common Journal Entry Examples:**

| Transaction | Account | Debit | Credit |
|---|---|---|---|
| Cash Sale | Cash (1000) | $1,000 | |
| | Sales Revenue (4000) | | $1,000 |
| Pay Rent | Rent Expense (5000) | $2,000 | |
| | Cash (1000) | | $2,000 |
| Purchase on Credit | Inventory (1300) | $500 | |
| | Accounts Payable (2000) | | $500 |

#### 7.3 Track Financial Activity
- Navigate to `/journal` to view all journal entries
- Each entry shows: entry number, account, description, debit/credit amounts, date

---

## 8. End-to-End Process Flows

### 8.1 Order-to-Cash Flow
```
Customer Created → Order Placed → Stock Reduced →
Invoice Issued → Payment Received → Invoice Marked PAID
```

**Step-by-step:**
1. **Create Customer** at `/customers`
2. **Create Order** at `/orders/new` — reduces stock
3. **Create Invoice** at `/invoices/new` against the customer
4. **Process Payment** via API (POST `/api/payments`)
5. **Mark Invoice PAID** via API (PATCH `/api/invoices/{id}/status`)

### 8.2 Procurement-to-Pay Flow
```
Vendor Created → Purchase Order Issued → PO Approved →
Goods Received (GRN) → Stock Increased → PO Marked RECEIVED
```

**Step-by-step:**
1. **Create Vendor** at `/vendors`
2. **Create Purchase Order** at `/purchase-orders/new`
3. **Approve PO** via API (PATCH `/api/purchase-orders/{id}/status`)
4. **Receive Goods** at `/goods-received/new` — increases stock
5. PO auto-marked RECEIVED

### 8.3 Hire-to-Payroll Flow
```
Employee Created → Attendance Tracked → Leave Managed →
Payroll Generated → Payroll Paid
```

**Step-by-step:**
1. **Create Employee** at `/employees/new`
2. **Track Attendance** daily via API
3. **Submit Leave Requests** via API
4. **Generate Payroll** at `/payroll/new` for a pay period
5. **Pay employees** by clicking "Pay" on each PENDING record

### 8.4 Accounting Month-End Flow
```
Chart of Accounts Maintained → Daily Journal Entries →
Review Entries in Journal → Generate Financial Reports
```

**Step-by-step:**
1. **Set up accounts** at `/accounts/new` (once, at setup)
2. **Record transactions daily** at `/journal/new`
3. **Review entries** at `/journal`
4. (Future:) Generate trial balance and financial statements

---

## Summary: Module Cross-References

| Module | Depends On | Data Flow |
|---|---|---|
| Orders | Products, Customers | Reads products; references customer |
| Invoices | Customers | Invoice belongs to a customer |
| Goods Received (GRN) | Purchase Orders, Inventory | GRN references PO; updates stock |
| Payroll | Employees | Reads employee list and salaries |
| Journal Entries | Accounts | Each line references an account code |

---

*Document version 1.0 — July 2026*
