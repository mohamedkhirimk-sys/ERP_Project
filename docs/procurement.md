# Procurement-to-Pay (Purchase Cycle)

**Service:** procurement-service (port 8096)  
**Database:** `procurement_db`  
**Entities:** Vendor → Purchase Order → Goods Received Note

---

## 1. Business Procedures

### 1.1 Vendor Management

#### Create a Vendor

**Path:** `/vendors`

1. Click **Vendors** in the sidebar
2. Click **+ New Vendor**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Name | Yes | "Office Supplies Inc." |
   | Email | Yes | "orders@officesupplies.com" |
   | Phone | No | "+1 555-0200" |
   | Address | No | "456 Industrial Blvd" |

4. Click **Save Vendor**
5. Vendor appears in the list

#### View & Delete Vendors

- Table shows name, email, phone, address
- No edit or delete in current UI (available via API)

### 1.2 Create a Purchase Order

**Path:** `/purchase-orders/new`

1. Click **Purchase Orders** in the sidebar
2. Click **+ New PO**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Vendor | Yes | Select from dropdown |
   | Total Amount | Yes | 5000.00 |
   | Status | Yes | PENDING |

4. Click **Create PO**

**PO Status Lifecycle:**
```
PENDING ──→ APPROVED ──→ RECEIVED (via GRN)
    │                       │
    └──→ CANCELLED          └──→ PARTIAL (via GRN)
```

### 1.3 Receive Goods (GRN)

**Path:** `/goods-received/new`

1. Click **Purchase Orders** in the sidebar
2. Click **Goods Received**
3. Fill in:

   | Section | Field | Required | Example |
   |---|---|---|---|
   | Header | Purchase Order | Yes | Select from dropdown |
   | | Status | Yes | RECEIVED / PARTIAL |
   | Items | SKU | Yes | "CHAIR-001" |
   | | Quantity | Yes | 20 |

4. Click **Add Item** for more lines
5. Click **Create GRN**

> **Business Rules:**
> - Creating a GRN updates the PO status to RECEIVED (or PARTIAL)
> - Stock quantities in the inventory service are increased

### 1.4 View Purchase Orders

**Path:** `/purchase-orders`

Table shows: PO number, vendor name, amount, status (color-coded), order date.

---

## 2. API Reference

### Vendors

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/vendors` | List all vendors |
| GET | `/api/vendors/{id}` | Get vendor by ID |
| POST | `/api/vendors` | Create vendor |
| PUT | `/api/vendors/{id}` | Update vendor |
| DELETE | `/api/vendors/{id}` | Delete vendor |

### Purchase Orders

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/purchase-orders` | List all POs |
| GET | `/api/purchase-orders/{id}` | Get PO by ID |
| GET | `/api/purchase-orders/vendor/{vendorId}` | List by vendor |
| POST | `/api/purchase-orders` | Create PO |
| PATCH | `/api/purchase-orders/{id}/status` | Update status |

### Goods Received Notes

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/goods-received` | List all GRNs |
| GET | `/api/goods-received/{id}` | Get GRN by ID |
| GET | `/api/goods-received/purchase-order/{poId}` | List by PO |
| POST | `/api/goods-received` | Create GRN |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Vendor created | `created_at` timestamp | Query `vendors` by name |
| PO created | `po_number` unique + `ordered_at` | View at `/purchase-orders` |
| PO status changed | ❌ Overwritten — no history | Only current status visible |
| GRN created | `grn_number` unique + `received_at` | Query `goods_received_notes` |
| Stock increased | Quantity change in inventory | Cross-reference GRN with `stocks` |

### 3.2 Full Procurement Trace

```sql
-- 1. Find the vendor
SELECT id, name, email, created_at
FROM procurement_db.public.vendors
WHERE name = 'Office Supplies Inc.';

-- 2. Find all POs for this vendor
SELECT id, po_number, total_amount, status, ordered_at
FROM procurement_db.public.purchase_orders
WHERE vendor_id = (SELECT id FROM procurement_db.public.vendors WHERE name = 'Office Supplies Inc.')
ORDER BY ordered_at DESC;

-- 3. Find GRNs for a specific PO
SELECT id, grn_number, status, received_at
FROM procurement_db.public.goods_received_notes
WHERE purchase_order_id = (
    SELECT id FROM procurement_db.public.purchase_orders WHERE po_number = 'PO-001'
);

-- 4. Verify stock impact
SELECT product_sku, quantity, warehouse_location
FROM inventory_db.public.stocks
WHERE product_sku IN (
    -- You'd need the GRN items; currently not stored in a separate table
    'CHAIR-001'
);
```

### 3.3 Reconciliation Queries

```sql
-- POs marked RECEIVED but without a GRN (data integrity issue)
SELECT po.po_number, po.vendor_id, po.status, po.ordered_at
FROM procurement_db.public.purchase_orders po
LEFT JOIN procurement_db.public.goods_received_notes grn
    ON grn.purchase_order_id = po.id
WHERE po.status = 'RECEIVED' AND grn.id IS NULL;

-- POs stuck in PENDING for more than 30 days
SELECT po_number, vendor_id, total_amount, ordered_at
FROM procurement_db.public.purchase_orders
WHERE status = 'PENDING' AND ordered_at < NOW() - INTERVAL '30 days';

-- Vendors with no PO activity
SELECT v.name, v.email
FROM procurement_db.public.vendors v
LEFT JOIN procurement_db.public.purchase_orders po ON po.vendor_id = v.id
WHERE po.id IS NULL;
```

### 3.4 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| No PO status history | Cannot see PENDING→APPROVED→RECEIVED lifecycle | Create `po_status_history` table |
| No GRN line items table | Cannot tell which SKUs were received | Create `grn_items` table (like `order_items`) |
| No `created_by` on PO/GRN | Cannot tell who placed the order or received goods | Add `created_by` to both entities |
| No `updated_at` on vendor | Cannot see vendor detail changes | Add `updated_at` to `vendors` |
| No `approved_by` on PO | Cannot tell who approved the PO | Add `approved_by` + `approved_at` to `purchase_orders` |

---

## 4. Database Schemas

```sql
CREATE TABLE vendors (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(100) NOT NULL UNIQUE,
    phone      VARCHAR(50),
    address    TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE purchase_orders (
    id           BIGSERIAL PRIMARY KEY,
    po_number    VARCHAR(50) NOT NULL UNIQUE,
    vendor_id    BIGINT NOT NULL REFERENCES vendors(id),
    total_amount DECIMAL(19,2) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ordered_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE goods_received_notes (
    id                BIGSERIAL PRIMARY KEY,
    grn_number        VARCHAR(50) NOT NULL UNIQUE,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    received_at       TIMESTAMP NOT NULL DEFAULT NOW()
);
```

---

*Document version 1.0 — July 2026*
