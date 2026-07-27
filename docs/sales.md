# Order-to-Cash (Sales Cycle)

**Services:** order-service (port 8093), sales-service (port 8095), payment-service (port 8094)  
**Databases:** `order_db`, `sales_db`, `payment_db`  
**Entities:** Customer → Order → Invoice → Payment

---

## 1. Business Procedures

### 1.1 Customer Management

#### Create a Customer

**Path:** `/customers`

1. Click **Customers** in the sidebar
2. Click **+ New Customer**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Name | Yes | "Acme Corporation" |
   | Email | Yes | "contact@acme.com" |
   | Phone | No | "+1 555-0100" |
   | Address | No | "123 Business Ave" |

4. Click **Save Customer**
5. Customer appears in the list

#### View & Delete Customers

- Table shows all customers with name, email, phone, address
- No edit or delete icons in the current UI (operations available via API)

### 1.2 Create an Order

**Path:** `/orders/new`

1. Click **Orders** in the sidebar
2. Click **+ New Order**
3. Fill in:

   | Section | Field | Required | Example |
   |---|---|---|---|
   | Header | Customer Name | Yes | "Acme Corporation" |
   | | Total Amount | Yes | 1500.00 |
   | | Status | Yes | PENDING |
   | | Payment Method | Yes | CREDIT_CARD |
   | Items | SKU | Yes | "CHAIR-001" |
   | | Quantity | Yes | 5 |

4. Click **Add Item** to add more lines
5. Click **Create Order**

> **Business Rule:** Creating an order automatically reduces stock quantities in the inventory service.

### 1.3 Create an Invoice

**Path:** `/invoices/new`

1. Click **Invoices** in the sidebar
2. Click **+ New Invoice**
3. Fill in:

   | Section | Field | Required | Example |
   |---|---|---|---|
   | Header | Customer | Yes | Select from dropdown |
   | | Total Amount | Yes | 1500.00 |
   | | Status | Yes | PENDING |
   | | Due Date | No | 2026-08-23 |
   | Items | SKU | Yes | "CHAIR-001" |
   | | Quantity | Yes | 5 |

4. Click **Add Item** to add more lines
5. Click **Create Invoice**

### 1.4 Invoice Status Lifecycle

```
PENDING ──→ PAID
    │           │
    ├──→ OVERDUE │
    └──→ CANCELLED
```

| Status | Meaning |
|---|---|
| PENDING | Issued, awaiting payment |
| PAID | Payment received |
| OVERDUE | Past due date, unpaid |
| CANCELLED | Voided / written off |

Status updates: `PATCH /api/invoices/{id}/status?status=PAID`

### 1.5 Payment Processing

- `POST /api/payments` with `orderId` and `amount`
- Status options: PENDING → COMPLETED / FAILED
- External webhook at `POST /api/webhooks/order-status` for external system updates

---

## 2. API Reference

### Customers

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/customers` | List all customers |
| GET | `/api/customers/{id}` | Get customer by ID |
| POST | `/api/customers` | Create customer |
| PUT | `/api/customers/{id}` | Update customer |
| DELETE | `/api/customers/{id}` | Delete customer |

### Orders

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/orders` | List all orders |
| POST | `/api/orders` | Create order |

### Invoices

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/invoices` | List all invoices |
| GET | `/api/invoices/{id}` | Get invoice by ID |
| GET | `/api/invoices/customer/{customerId}` | List by customer |
| POST | `/api/invoices` | Create invoice |
| PATCH | `/api/invoices/{id}/status` | Update status |

### Payments

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/payments` | Process payment |
| POST | `/api/webhooks/order-status` | External status webhook |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Customer created | `created_at` timestamp | Query `customers` by name |
| Customer deleted | ❌ Hard delete — record removed | No trace |
| Order created | `order_number` unique + auto-increment `id` | View at `/orders` |
| Order items | `order_items` table linked by `order_id` | Join `orders` + `order_items` |
| Invoice created | `invoice_number` unique + `issued_at` | View at `/invoices` |
| Invoice status change | ❌ Overwritten — no history | Can only see current status |
| Payment processed | `created_at` on `payments` | Check payment by `order_id` |

### 3.2 Full Order-to-Cash Trace

Cross-service query to trace a complete sale:

```sql
-- 1. Find the customer
SELECT id, name, email, created_at
FROM sales_db.public.customers
WHERE name = 'Acme Corporation';

-- 2. Find orders for this customer
SELECT id, order_number, customer_name, total_amount, status
FROM order_db.public.orders
WHERE customer_name = 'Acme Corporation'
ORDER BY id;

-- 3. Find invoices for this customer
SELECT id, invoice_number, total_amount, status, issued_at, due_date
FROM sales_db.public.invoices
WHERE customer_id = (SELECT id FROM sales_db.public.customers WHERE name = 'Acme Corporation');

-- 4. Find payments linked to the order
SELECT id, order_id, amount, status, payment_method, created_at
FROM payment_db.public.payments
WHERE order_id IN (
    SELECT id FROM order_db.public.orders WHERE customer_name = 'Acme Corporation'
);
```

### 3.3 Order Items Detail

```sql
-- What SKUs were sold in each order
SELECT o.order_number, oi.product_sku, oi.quantity, o.total_amount, o.status
FROM order_db.public.orders o
JOIN order_db.public.order_items oi ON oi.order_id = o.id
WHERE o.order_number = 'ORD-001';
```

### 3.4 Reconciliation Queries

```sql
-- Orders without any corresponding invoice
SELECT o.order_number, o.customer_name, o.total_amount, o.status
FROM order_db.public.orders o
LEFT JOIN sales_db.public.invoices i ON i.customer_id IS NOT NULL  -- approximated
WHERE o.status NOT IN ('CANCELLED');

-- Invoices with no matching payment
SELECT i.invoice_number, i.customer_name, i.total_amount, i.status, i.issued_at
FROM sales_db.public.invoices i
WHERE i.status = 'PENDING' AND i.issued_at < NOW() - INTERVAL '30 days';
```

### 3.5 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| No order status history | Cannot see lifecycle (PENDING→CONFIRMED→SHIPPED→DELIVERED) | Create `order_status_history` table |
| No invoice status history | Cannot see PENDING→OVERDUE→PAID transitions | Create `invoice_status_history` table: `id, invoice_id, from_status, to_status, changed_by, changed_at` |
| No `created_by` on orders/invoices | Cannot tell who created them | Add `created_by` to both entities |
| No `updated_at` on any entity | Cannot detect unauthorized modifications | Add `updated_at` to customers, invoices |
| Hard delete on customers | Orphaned invoice references | Implement soft delete (`active` boolean) |

---

## 4. Database Schemas

```sql
-- sales_db
CREATE TABLE customers (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    phone      VARCHAR(50),
    address    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE invoices (
    id             BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id    BIGINT NOT NULL REFERENCES customers(id),
    total_amount   DECIMAL(19,2) NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    issued_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    due_date       DATE
);

-- order_db
CREATE TABLE orders (
    id            BIGSERIAL PRIMARY KEY,
    order_number  VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    total_amount  DECIMAL(19,2) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50)
);

CREATE TABLE order_items (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    product_sku VARCHAR(50) NOT NULL,
    quantity    INTEGER NOT NULL
);

-- payment_db
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT,
    amount          DECIMAL(19,2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(50),
    transaction_id  VARCHAR(100),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

*Document version 1.0 — July 2026*
