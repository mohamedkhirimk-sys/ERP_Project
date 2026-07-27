# Product & Inventory Management

**Services:** product-service (port 8083), inventory-service (port 8091)  
**Databases:** `product_db`, `inventory_db`  
**Entities:** Product, Stock

---

## 1. Business Procedures

### 1.1 Create a Product

**Path:** `/products/new`

1. Click **Products** in the sidebar
2. Click **+ New Product**
3. Fill in the form:

   | Field | Required | Example |
   |---|---|---|
   | Product Name | Yes | "Ergonomic Office Chair" |
   | SKU | Yes | "CHAIR-001" (must be unique) |
   | Price | Yes | 299.99 |
   | Stock Quantity | Yes | 50 |

4. Click **Save Product**
5. Product appears in the product list at `/products`

### 1.2 Edit a Product

**Path:** `/products/{id}/edit`

1. In the product list, click the **edit (pencil)** icon
2. Modify fields
3. Click **Save Product**

### 1.3 Delete a Product

1. In the product list, click the **delete (trash)** icon
2. Confirm deletion
3. **Note:** Deletion is permanent (hard delete)

### 1.4 Initialize Stock

**Path:** `/inventory`

1. Click **Stock** in the sidebar
2. Click **Initialize Stock**
3. Fill in:

   | Field | Required | Example |
   |---|---|---|
   | Product SKU | Yes | "CHAIR-001" |
   | Quantity | Yes | 50 |
   | Warehouse | No | "Warehouse A" |

4. Click **Save**
5. Stock record appears in the stock table

### 1.5 Stock Adjustments

Stock quantities change automatically when:

| Event | Effect |
|---|---|
| Order created | Stock decreases by order quantity |
| Goods Received Note created | Stock increases by received quantity |

Manual adjustment: `PUT /api/inventory/stock/{sku}?quantityChange=N`

### 1.6 View Stock Levels

**Path:** `/inventory`

The table shows each SKU with color-coded quantity:

| Color | Meaning |
|---|---|
| Green | Stock > 10 |
| Yellow | Stock 1–10 (low stock warning) |
| Red | Stock = 0 (out of stock) |

---

## 2. API Reference

### Products

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/products` | List all products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create a product |
| PUT | `/api/products/{id}` | Update a product |
| DELETE | `/api/products/{id}` | Delete a product |

### Inventory

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/inventory/stocks` | List all stock records |
| GET | `/api/inventory/stock/{sku}` | Get stock by SKU |
| POST | `/api/inventory/stock` | Initialize stock |
| PUT | `/api/inventory/stock/{sku}` | Adjust stock (query param: `quantityChange`) |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| Product created | Auto-increment `id` (sequential) | Products are ordered by `id` in list |
| Product edited | ❌ Overwritten — no `updated_at` | Can only see current values |
| Product deleted | ❌ Hard delete — record removed | Only `id` gap in sequence remains |
| Stock initialized | Stock record appears in `/inventory` | Query `stocks` table |
| Stock adjusted | Quantity changes (cumulative) | No per-adjustment log |

### 3.2 Audit Queries

```sql
-- All products ordered by creation
SELECT id, sku, name, price, description
FROM product_db.public.products
ORDER BY id;

-- All stock levels
SELECT id, product_sku, quantity, warehouse_location
FROM inventory_db.public.stocks
ORDER BY product_sku;

-- Negative stock (impossible — investigate)
SELECT product_sku, quantity
FROM inventory_db.public.stocks
WHERE quantity < 0;

-- Low stock alert
SELECT product_sku, quantity
FROM inventory_db.public.stocks
WHERE quantity <= 10
ORDER BY quantity;
```

### 3.3 Cross-Service Trace: Stock Change

If stock went from 50 to 47, trace the cause:

```sql
-- 1. Current stock
SELECT product_sku, quantity FROM inventory_db.public.stocks WHERE product_sku = 'CHAIR-001';

-- 2. Find orders that used this SKU (in order-service)
SELECT o.order_number, o.customer_name, oi.quantity, o.status, o.id
FROM order_db.public.orders o
JOIN order_db.public.order_items oi ON oi.order_id = o.id
WHERE oi.product_sku = 'CHAIR-001'
ORDER BY o.id DESC;

-- 3. Find GRNs that received this SKU (in procurement-service)
-- Note: GRN items are stored as JSON/hardcoded in current implementation
```

### 3.4 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| No stock movement log | Cannot trace individual stock changes | Create `stock_movements` table: `id, sku, before_qty, after_qty, reference_type (ORDER/GRN/ADJUSTMENT), reference_id, changed_by, changed_at` |
| No `updated_at` on product | Cannot tell when last edited | Add `updated_at` to `products` |
| No `updated_by` on product | Cannot tell who edited | Add `updated_by` to `products` |
| No `created_at` on product or stock | Missing creation timeline | Add `created_at` to both entities |
| Hard delete on products | Orphaned references in orders | Implement soft delete (`active` boolean) |

---

## 4. Database Schemas

```sql
-- product_db
CREATE TABLE products (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    price          DECIMAL(19,2) NOT NULL,
    sku            VARCHAR(50)  NOT NULL UNIQUE,
    stock_quantity INTEGER      NOT NULL DEFAULT 0
);

-- inventory_db
CREATE TABLE stocks (
    id                BIGSERIAL PRIMARY KEY,
    product_sku       VARCHAR(50) NOT NULL UNIQUE,
    quantity          INTEGER     NOT NULL DEFAULT 0,
    warehouse_location VARCHAR(100)
);
```

---

*Document version 1.0 — July 2026*
