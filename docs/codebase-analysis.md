# ERP Codebase — Full Analysis

> Generated July 2026. Covers all 148 Java source files across 13 backend services + frontend architecture.

---

## Table of Contents

1. [Cross-Cutting Issues Found in Every Service](#cross-cutting-issues)
2. [Architecture Overview](#architecture-overview)
3. [Service-by-Service Breakdown](#service-breakdown)
   - [1. Common-Lib](#1-common-lib)
   - [2. Eureka Server](#2-eureka-server)
   - [3. Gateway Service (Port 8092)](#3-gateway-service)
   - [4. Identity Service (Port 8086)](#4-identity-service)
   - [5. Product Service (Port 8083)](#5-product-service)
   - [6. Inventory Service (Port 8091)](#6-inventory-service)
   - [7. Order Service (Port 8093)](#7-order-service)
   - [8. Payment Service (Port 8094)](#8-payment-service)
   - [9. Sales Service (Port 8095)](#9-sales-service)
   - [10. Procurement Service (Port 8096)](#10-procurement-service)
   - [11. HRM Service (Port 8097)](#11-hrm-service)
   - [12. Finance Service (Port 8098)](#12-finance-service)
   - [13. Reporting Service (Port 8099)](#13-reporting-service)
4. [Frontend Architecture](#frontend-architecture)
5. [Database Schema](#database-schema)
6. [Critical Bugs Summary](#critical-bugs)

---

<a name="cross-cutting-issues"></a>
## Cross-Cutting Issues Found in EVERY Service

| Issue | Severity | Details |
|-------|----------|---------|
| **No `@ControllerAdvice`** | CRITICAL | No global exception handler in any service (common-lib has one but no service depends on it). All errors return HTTP 500 with stack trace leak. |
| **Statuses = plain String** | CRITICAL | Every entity uses `String` for status fields (order, invoice, PO, GRN, leave, payroll, employee, account type). No enums, no lifecycle enforcement. |
| **No `@Version` optimistic locking** | CRITICAL | Every single entity lacks `@Version`. Race conditions, overselling, lost updates everywhere. |
| **No tests** | CRITICAL | Only payment-service has tests — and some are broken. All other services have zero test coverage. |
| **No pagination/search** | HIGH | Every list endpoint returns `findAll()` — unbounded. With real data volumes these will OOM or timeout. |
| **No RBAC enforcement** | HIGH | Role is stored in JWT but NEVER checked on any server-side endpoint. STAFF can call any ADMIN-only endpoint via curl. |
| **No audit fields** | HIGH | No `createdBy`, `updatedBy`, `updatedAt` on any entity (only `createdAt` exists on a few). |
| **Hardcoded secrets** | HIGH | JWT secret (`c29tZS1zZWNyZXQ...`) and DB passwords (`md1962km`) in source-controlled config files. |
| **No idempotency** | HIGH | No idempotency keys on any POST endpoint. Retries create duplicates. |
| **No circuit breakers on Feign** | HIGH | Every Feign client lacks `fallback`, `@CircuitBreaker`, `@Retry`. Downstream failure = cascading failure. |
| **JWT algorithm mismatch** | CRITICAL | Identity-service signs with **HS256** (jjwt default), gateway verifies with **HS384**. Authenticated API calls through gateway will get 401. |
| **Compilation error in common-lib** | CRITICAL | `ErrorResponse.java` declares `class GlobalExceptionHandler` — duplicate class name + missing `ErrorResponse` symbol. |
| **`ddl-auto=update`** | HIGH | All services use Hibernate auto-DDL. No Flyway/Liquibase migrations. Schema changes can delete data in production. |
| **No `@Transactional` on services** | MEDIUM | Most services lack `@Transactional`. Multi-step operations (save + Feign call) have no transactional boundary. |
| **Entities returned directly** | MEDIUM | Product, Customer, Vendor, Stock controllers return JPA entities in HTTP responses — leaks DB structure. |

---

<a name="architecture-overview"></a>
## Architecture Overview

```
Frontend (React 19, Port 5173)
    │  axios via gateway
    ▼
Gateway (Spring Cloud Gateway, Port 8092)
    │  JWT validation via OAuth2 Resource Server
    │
    ├── identity-service (8086) ─── DB: erp_db
    ├── product-service  (8083) ─── DB: product_db
    ├── inventory-service (8091) ─── DB: inventory_db
    ├── order-service    (8093) ─── DB: order_db
    │   ├── feign ──→ inventory-service
    │   └── feign ──→ payment-service
    ├── payment-service  (8094) ─── DB: payment_db
    │   └── feign ──→ order-service (unused)
    ├── sales-service    (8095) ─── DB: sales_db
    │   ├── feign ──→ inventory-service
    │   └── returns entity (Customer) not DTO ⚠️
    ├── procurement-service (8096) ─── DB: procurement_db
    │   └── feign ──→ inventory-service
    ├── hrm-service      (8097) ─── DB: hrm_db
    │   └── NO feign clients
    ├── finance-service  (8098) ─── DB: finance_db
    │   └── NO feign clients (payroll disconnected from HRM)
    └── reporting-service (8099) ─── reads from ALL via RestTemplate
        └── DemoDataController: POST /api/reports/seed

Eureka Server (8761)
PostgreSQL 17 (single instance, 9 databases)
```

### Service Dependencies (Feign)

```
Order Service ──feign──→ Inventory Service (deduct stock)
Order Service ──feign──→ Payment Service (process payment)
Sales Service ──feign──→ Inventory Service (deduct stock on invoice)
Procurement Service ──feign──→ Inventory Service (add stock on GRN)
Payment Service ──feign──→ Order Service (get order — UNUSED)
```

---

<a name="service-breakdown"></a>
## Service-by-Service Breakdown

---

### 1. COMMON-LIB (Shared Library)

**Purpose**: Intended as a shared JAR for DTOs, exceptions, events used by all services. **No service depends on it in their pom.xml** — it's effectively dead code.

#### Files

**`dto/ApiResponse.java`**
- Generic API response wrapper: `success` (boolean), `message` (String), `data` (generic T), `timestamp` (LocalDateTime)
- Uses Lombok `@Data @Builder`
- **Unused by any service**

**`exception/GlobalExceptionHandler.java`**
- `@RestControllerAdvice` with two handlers:
  - `RuntimeException` → HTTP 500 with `ApiResponse`
  - `IllegalArgumentException` → HTTP 400 with `ApiResponse`
- **Unused** — no service depends on common-lib

**`exception/ErrorResponse.java`** ⚠️ **BROKEN**
- File name is `ErrorResponse.java` but **class is declared as `public class GlobalExceptionHandler`**
- **Duplicate class name** in the same package as the real `GlobalExceptionHandler.java`
- References `new ErrorResponse(...)` — no `ErrorResponse` class exists anywhere
- This file **will not compile**

---

### 2. EUREKA-SERVER (Port 8761)

**Purpose**: Netflix Eureka service discovery. All services register here and find each other by name.

**`EurekaServerApplication.java`**
- `@EnableEurekaServer` — activates the Eureka dashboard
- `@EnableDiscoveryClient` — registers itself (for high-availability mode)
- Dashboard at `http://localhost:8761`

---

### 3. GATEWAY-SERVICE (Port 8092)

**Purpose**: Single entry point for all API calls. Routes requests to the correct service, validates JWT, handles CORS.

**`GatewayApplication.java`**
- Standard `@SpringBootApplication`
- Routes configured in `application.yml` (e.g., `/api/products/**` → product-service)

**`config/SecurityConfig.java`** ⚠️ **JWT ALGORITHM MISMATCH**

| Aspect | Detail |
|--------|--------|
| CORS | `corsConfigurationSource()` — allows all origins, all methods, all headers, credentials=true. Wildcard `*` is fine for dev but insecure for production. |
| Public paths | `/api/auth/**`, `/api/identity/**`, `/`, `/actuator/**` |
| Auth | All other paths require `authenticated()` |
| **JWT Decoder** (line 44-48) | Uses `Keys.hmacShaKeyFor()` with **`MacAlgorithm.HS384`** |
| **Key** | `jwtSecret` from config (same hardcoded base64 string as identity-service) |
| **RBAC** | None — no role checking. Any valid JWT can access any endpoint. |

**Critical bug**: The identity-service signs JWTs with **HS256** (jjwt default), but the gateway decodes with **HS384**. These algorithms produce different-length signatures. The gateway will reject every JWT with a `JwtException` (Mac algorithm mismatch), resulting in **401 on every authenticated API call**.

---

### 4. IDENTITY-SERVICE (Port 8086)

**Purpose**: User registration, login, JWT token generation. Two roles: ADMIN and STAFF.

**`entity/Role.java`**
- Enum: `ADMIN`, `STAFF`

**`entity/User.java`**
- `id` (Long, auto PK), `username` (unique), `email` (NOT unique ⚠️), `fullName`, `password` (BCrypt hashed), `role` (enum stored as String), `createdAt` (auto)
- **Bug**: `email` column lacks `@Column(unique=true)` — duplicate emails allowed
- Missing: `updatedAt`, `enabled`, `locked`, `failedLoginAttempts`, `lastLogin`, `@Version`

**`dto/RegistrationRequest.java`**
- Validated: `username` (@NotBlank), `email` (@Email), `fullName` (@NotBlank), `password` (@Size min=8), `role` (@NotBlank)
- **Security issue**: Client sends the `role` field — nothing prevents registering as ADMIN

**`dto/LoginRequest.java`**
- Validated: `username`, `password`

**`repository/UserRepository.java`**
- Custom: `findByUsername(String)` → `Optional<User>`

**`mapper/UserMapper.java`** (MapStruct)
- Converts `RegistrationRequest` → `User`
- Maps role string to enum via `role.toUpperCase()`
- Ignores `password` (set separately after encoding)

**`util/JwtUtil.java`**
```
JWT Structure:
  subject: username
  claims: { "role": "ADMIN"|"STAFF" }
  iat: now
  exp: now + 24h
  signed-with: HMAC-SHA (default = HS256)
```
- Uses jjwt library (`io.jsonwebtoken`)
- Secret injected from `jwt.secret` config
- `generateToken()` → signs with HS256 (jjwt default)
- `validateToken()` → parses and verifies

**Critical**: Signs with **HS256**, gateway expects **HS384**. Auth is broken.

**`service/AuthService.java`**
- `register()`: Checks username uniqueness via `findByUsername()`, encodes password with BCrypt, saves
- `login()`: Finds user, verifies password, returns JWT as plain text string
- `BCryptPasswordEncoder` instantiated with `new` — not a Spring bean. Duplicated in `DataSeeder`.
- Error handling: bare `IllegalArgumentException` → HTTP 500 (wrong status code)
- No rate limiting, no failed-attempt tracking

**`controller/AuthController.java`**
- `POST /api/auth/register` → returns "User registered successfully"
- `POST /api/auth/login` → returns JWT token string
- Uses `@Valid` on request bodies

**`config/SecurityConfig.java`** (identity-service)
- Configures OAuth2 Resource Server with JWT decoder using **HS256** (correct for this service's signing)
- Session stateless
- `/api/auth/**` is public
- **Duplicate of gateway auth** — this is the service-level security, which is redundant since gateway handles it

**`config/DataSeeder.java`**
- `CommandLineRunner` — seeds default users if `users` table is empty
- Creates: `admin / admin123` (ADMIN) and `staff / staff123` (STAFF)
- Default credentials hardcoded and displayed on the login page UI

---

### 5. PRODUCT-SERVICE (Port 8083)

**Purpose**: Product catalog CRUD. Simple entity with no categories, variants, or tax.

**`entity/Product.java`** (6 fields)
- `id`, `name`, `description`, `price`, `sku` (unique), `stockQuantity`
- Missing: categories (no `@ManyToMany`), tax rate, cost price, barcode/UPC, active flag, image, weight/dimensions, `createdAt`, `updatedAt`, `@Version`

**`dto/ProductRequest.java`**
- Validated: `name` (@NotBlank), `price` (@NotNull @Positive), `sku` (@NotBlank)
- `description` and `stockQuantity` are optional
- **No response DTO** — controller returns `Product` entity directly

**`repository/ProductRepository.java`**
- Custom: `existsBySku(String)`
- No pagination, no search

**`service/ProductService.java`** (interface)
- CRUD methods

**`service/impl/ProductServiceImpl.java`**
- `createProduct()`: Checks SKU uniqueness, builds entity, saves
- `updateProduct()`: Loads, sets all fields, saves. **Bug**: SKU uniqueness NOT re-checked — can rename to duplicate SKU
- `deleteProduct()`: Loads then hard-deletes. No soft delete. No FK check (will fail if orders reference this product)
- No `@Transactional`

**`controller/ProductController.java`**
- `GET /api/products` → list all (returns `List<Product>` — **entity leak**)
- `GET /api/products/{id}` → single product (returns `Product` directly)
- `POST /api/products` → create
- `PUT /api/products/{id}` → update
- `DELETE /api/products/{id}` → 204

---

### 6. INVENTORY-SERVICE (Port 8091)

**Purpose**: Stock level tracking by SKU. Supports warehouse location. Used by order, sales, and procurement services via Feign.

**`entity/Stock.java`** (4 fields)
- `id`, `productSku` (unique), `quantity`, `warehouseLocation`
- **No `@Version`** — concurrency disaster waiting to happen
- Missing: `reservedQuantity`, `reorderLevel`, `unitCost`, `minQuantity`, `maxQuantity`

**`dto/StockRequest.java`**
- Validated: `productSku` (@NotBlank), `quantity` (@NotNull @Min(0)), optional `warehouseLocation`

**`repository/StockRepository.java`**
- Custom: `findByProductSku(String)`

**`service/InventoryService.java`** (interface)
- `initializeStock`, `getStockBySku`, `updateStockQuantity`, `getAllStocks`

**`service/impl/InventoryServiceImpl.java`** ⚠️ **OVERSALE BUG**
```
updateStockQuantity(String sku, Integer quantityChange):
  1. Read current stock from DB
  2. Compute newQuantity = current + quantityChange
  3. If newQuantity < 0 → throw
  4. Save
```
- **Race condition**: No `@Transactional`, no `@Version`, no pessimistic locking.
- **Two concurrent requests** can both read the same quantity (e.g., 10), both compute `10 + (-10) = 0`, both pass the check, both save. Result: 20 units sold but stock shows 0. **Overselling.**
- `initializeStock()`: Also no `@Version` — duplicate SKU check + save is not atomic
- No `@Transactional` on any method

**`controller/InventoryController.java`** — returns `Stock` entity directly (no DTO)
- `POST /api/inventory/stock` → initialize
- `GET /api/inventory/stock/{sku}` → get by SKU
- `PUT /api/inventory/stock/{sku}?quantityChange=N` → adjust (used by Feign clients)
- `GET /api/inventory/stocks` → list all

---

### 7. ORDER-SERVICE (Port 8093)

**Purpose**: Order creation with stock deduction and payment processing. The most complex flow in the system.

**`entity/OrderEntity.java`**
- `id`, `orderNumber` (unique), `customerName` (plain String — no FK to Customer), `totalAmount`, `status` (String), `items` (@OneToMany cascade=ALL orphanRemoval=true)
- Missing: `customerId` FK, `createdAt`, `updatedAt`, `paymentStatus`, `@Version`
- Has proper header/detail with OrderItem — the best entity design in the system

**`entity/OrderItem.java`**
- `id`, `order` (ManyToOne), `productSku`, `quantity`
- Missing: `unitPrice`, `lineTotal`, `productName` (denormalized for history)

**`dto/OrderRequest.java`**
- Validated: `customerName`, `totalAmount` (@Positive), `status` (any string), `items` (@NotEmpty list), `paymentMethod`

**`dto/OrderResponse.java`**
- `id`, `orderNumber`, `customerName`, `totalAmount`, `status`, `items` (list of ItemResponse)

**`dto/ItemRequest.java`** — `productSku` (@NotBlank), `quantity` (@Min(1))
**`dto/ItemResponse.java`** — `id`, `productSku`, `quantity`
**`dto/PaymentRequest.java`** — duplicated in payment-service
**`dto/PaymentResponse.java`** — unused locally, returned from Feign
**`dto/StockDeductionRequest.java`** — unused (Feign passes params directly)

**`client/InventoryClient.java`**
```
@FeignClient(name = "inventory-service", path = "/api/inventory")
void deductStock(@PathVariable("sku") String sku, @RequestParam("quantityChange") Integer quantityChange);
```
- **No fallback, no circuit breaker, no retry**. If inventory-service is down, the Feign call throws `FeignException` and the order creation fails after partial processing.

**`client/PaymentClient.java`**
```
@FeignClient(name = "payment-service", path = "/api/payments")
PaymentResponse processPayment(@RequestBody PaymentRequest request);
```
- Same issues: no fallback, no resilience

**`repository/OrderRepository.java`**
- No custom methods

**`service/OrderService.java`** ⚠️ **NO COMPENSATION**

```
@Transactional  (line 23)
createOrder(request):
  1. Build OrderEntity with "ORD-" + System.currentTimeMillis()
  2. For each item:
     a. Call inventoryClient.deductStock() — Feign call to another service
     b. Build OrderItem
  3. Save order to DB
  4. Call paymentClient.processPayment() — Feign call to payment service
  5. Return response
```

**Critical issues**:
1. **No compensation on failure**: If step 4 (payment) fails, steps 2a (stock deduction) and 3 (order save) have already happened. Stock is gone. Order is in DB. No rollback.
2. **`@Transactional` only wraps local DB** — Feign calls use separate connections, so they're not part of the local transaction anyway.
3. **orderNumber not thread-safe**: `System.currentTimeMillis()` collision under concurrent requests.
4. **Status is client-supplied**: User can create an order as "DELIVERED" directly — no lifecycle enforcement.

**`controller/OrderController.java`** — only 2 endpoints
- `POST /api/orders` → create (201)
- `GET /api/orders` → list all
- Missing: `GET /{id}`, `PUT`, `DELETE`, status update, customer-specific queries, pagination

---

### 8. PAYMENT-SERVICE (Port 8094)

**Purpose**: Payment processing (stub — always returns COMPLETED). Webhook endpoint for external status updates (stub).

**`entity/PaymentStatus.java`** — **the only enum-based status in the entire project**
```java
public enum PaymentStatus { PENDING, COMPLETED, FAILED }
```

**`entity/PaymentEntity.java`**
- `id`, `orderId`, `amount`, `status` (enum — good), `paymentMethod`, `transactionId`, `createdAt`
- Well-structured. Has `createdAt` via `@PrePersist`.

**`dto/PaymentRequest.java`**
- `orderId`, `amount`, `paymentMethod` — **no validation annotations**

**`dto/PaymentResponse.java`** — mirrors entity fields
**`dto/WebhookRequest.java`** — `orderId`, `status` (manually written getters, not Lombok)

**`client/OrderClient.java`** — **UNUSED**
```
@FeignClient(name = "order-service")
OrderResponse getOrderById(@PathVariable Long id);
```
- No code in the service calls this client.

**`client/OrderResponse.java`** — duplicate of order-service's `OrderResponse`
**`exception/PaymentProcessingException.java`** — **the only custom exception in the entire project** (besides the broken common-lib one). Extends `RuntimeException`.

**`repository/PaymentRepository.java`**
- Custom: `findByOrderId(String)`

**`service/PaymentService.java`**

```
processPayment(request):
  1. validateRequest()
  2. status = determinePaymentStatus()  ← ALWAYS returns "COMPLETED" (stub)
  3. Build PaymentEntity
  4. If FAILED → log error
     Else → publish PaymentCompletedEvent (in-process only)
  5. Save to DB
  6. Return response

handleOrderStatusUpdate(orderId, status):  ← STUB
  log.info("Handling order status update for order: {}", orderId)
  // DOES NOTHING
```

- `determinePaymentStatus()` (line 60-62): **Always returns "COMPLETED"**. No real payment gateway integration. No fraud check. No 3DS.
- `PaymentCompletedEvent` is published via `ApplicationEventPublisher` (Spring Events) **which is in-process only** — no consumer exists anywhere. The `spring-boot-starter-amqp` dependency is in pom.xml but no RabbitMQ listener is configured.
- `handleOrderStatusUpdate()`: **Does nothing**. The integration test (`PaymentServiceIntegrationTest`) expects it to update payment status to FAILED, but it doesn't — **the test would fail**.

**`controller/PaymentController.java`**
- `POST /api/payments` → process payment. No `@Valid`.

**`controller/WebhookController.java`**
- `POST /api/webhooks/order-status` → calls the stub `handleOrderStatusUpdate()`. Always returns "Webhook received".

**`com/erp/common/event/PaymentCompletedEvent.java`**
- Placed under `com.erp.common.event` package **inside payment-service** (not in common-lib JAR)
- Fields: `orderNumber`, `amount`, `paymentStatus`

---

### 9. SALES-SERVICE (Port 8095)

**Purpose**: Customer management and invoice generation. Links to inventory for stock deduction.

**`entity/Customer.java`**
- `id`, `name`, `email` (unique), `phone`, `address` (single text — no street/city/zip), `createdAt`
- Missing: taxId/VAT, billingAddress, shippingAddress, creditLimit, customerGroup, active flag

**`entity/Invoice.java`** ⚠️ **NO LINE ITEMS**
- `id`, `invoiceNumber` (unique), `customer` (ManyToOne), `totalAmount`, `status` (String), `issuedAt`, `dueDate`
- **No `@OneToMany` for invoice items** — the entity has no line items.
- Missing: `orderId` (no link back to originating order)

**`dto/CustomerRequest.java`** — validated: `name`, `email` (@Email)
**`dto/InvoiceRequest.java`** — validated: `customerId`, `totalAmount` (@Positive), `status`, `dueDate`, `items` (@NotEmpty list of InvoiceItemRequest)
**`dto/InvoiceResponse.java`** — `id`, `invoiceNumber`, `customerId`, `customerName`, `totalAmount`, `status`, `issuedAt`, `dueDate`. **No items.**
**`dto/InvoiceItemRequest.java`** — `productSku`, `quantity`

**`client/InventoryClient.java`**
- Same as order-service: `PUT /api/inventory/stock/{sku}?quantityChange=N`. No fallback.

**`repository/CustomerRepository.java`** — `findByEmail(String)`
**`repository/InvoiceRepository.java`** — `findByCustomerId(Long)`

**`service/impl/CustomerServiceImpl.java`**
- Standard CRUD with duplicate email check. Returns entity directly. Hard delete.

**`service/impl/InvoiceServiceImpl.java`** ⚠️ **DATA LOSS BUG**

```
@Transactional  (line 26)
createInvoice(request):
  1. Find customer by ID
  2. For each item in request:
     a. Call inventoryClient.deductStock() — deducts stock
     // ITEMS ARE NEVER SAVED
  3. Build Invoice WITHOUT items:
     invoiceNumber = "INV-" + System.currentTimeMillis()
     customer, totalAmount, status, dueDate
  4. Save invoice (no items!)
  5. Return response
```

**Critical**: Invoice line items are accepted from the client (`InvoiceItemRequest` with SKU + quantity), used to call stock deduction, and then **completely discarded**. The invoice is saved with zero line items. You cannot see what products were on an invoice.

Other issues:
- `invoiceNumber` uses `System.currentTimeMillis()` — not thread-safe
- `updateInvoiceStatus()` (line 68-73): Accepts any string — no lifecycle validation. Can change PAID→PENDING.
- `@Transactional` wraps local DB only, not the Feign call

**`controller/CustomerController.java`** — returns `Customer` entity directly (no DTO)
- Full CRUD at `/api/customers`

**`controller/InvoiceController.java`** — uses DTOs for response
- `POST /api/invoices` → create
- `GET /api/invoices` → list all
- `GET /api/invoices/{id}` → single
- `GET /api/invoices/customer/{customerId}` → by customer
- `PATCH /api/invoices/{id}/status?status=PAID` → status update

---

### 10. PROCUREMENT-SERVICE (Port 8096)

**Purpose**: Vendor management, purchase orders, goods receiving. Links to inventory for stock increases.

**`entity/Vendor.java`** (same structure as Customer)
- `id`, `name`, `email` (unique), `phone`, `address`, `createdAt`
- Missing: vendorCode, taxId, paymentTerms, currency, creditLimit

**`entity/PurchaseOrder.java`** ⚠️ **NO LINE ITEMS**
- `id`, `poNumber` (unique), `vendor` (ManyToOne), `totalAmount`, `status` (String), `orderedAt`
- **No `@OneToMany` for PO items** — flat total amount, no SKU breakdown
- No `@Version`

**`entity/GoodsReceivedNote.java`** ⚠️ **NO LINE ITEMS**
- `id`, `grnNumber` (unique), `purchaseOrder` (ManyToOne), `status` (String), `receivedAt`
- **Items are sent in the request but never persisted**

**`dto/VendorRequest.java`** — validated: `name`, `email` (@Email)
**`dto/PurchaseOrderRequest.java`** — validated: `vendorId`, `totalAmount` (@Positive), `status`. No items.
**`dto/PurchaseOrderResponse.java`** — `id`, `poNumber`, `vendorId`, `vendorName`, `totalAmount`, `status`, `orderedAt`
**`dto/GoodsReceivedNoteRequest.java`** — validated: `purchaseOrderId`, `status`, `items` (@NotEmpty)
**`dto/GoodsReceivedNoteResponse.java`** — no items field
**`dto/ItemRequest.java`** — `productSku`, `quantity` (@Min(1))

**`client/InventoryClient.java`** — `addStock()`. No fallback.

**`service/impl/VendorServiceImpl.java`** — standard CRUD, returns entity directly
**`service/impl/PurchaseOrderServiceImpl.java`**
- `poNumber = "PO-" + System.currentTimeMillis()` — not thread-safe
- Status update: any string → any string. No lifecycle enforcement
- No `@Transactional`

**`service/impl/GoodsReceivedNoteServiceImpl.java`**
- Has `@Transactional` on `createGoodsReceivedNote` — good
- Calls `inventoryClient.addStock()` for each item — items not persisted locally
- `grnNumber = "GRN-" + System.currentTimeMillis()`
- Does NOT update PO status to RECEIVED (despite business procedure docs saying it should)

**`controller/VendorController.java`** — returns `Vendor` entity directly
**`controller/PurchaseOrderController.java`** — uses DTOs
**`controller/GoodsReceivedNoteController.java`** — uses DTOs

---

### 11. HRM-SERVICE (Port 8097)

**Purpose**: Employee records, attendance tracking, leave management.

**`entity/Employee.java`**
- `id`, `employeeId` (unique, auto `"EMP-" + timestamp`), `firstName`, `lastName`, `email` (unique), `phone`, `department`, `position`, `salary`, `hireDate`, `status` (String, default "ACTIVE"), `createdAt`
- Missing: dateOfBirth, nationalId/SSN/taxId, address, emergencyContact, employmentType (FULL_TIME/CONTRACTOR), managerId, terminationDate, bankAccountInfo, @Version

**`entity/Attendance.java`**
- `id`, `employee` (ManyToOne), `date`, `clockIn`, `clockOut`, `status` (String, default "PRESENT")
- Missing: hoursWorked, overtimeHours, lateMinutes
- **No unique constraint on (employee_id, date)** — duplicate records possible

**`entity/Leave.java`**
- `id`, `employee` (ManyToOne), `leaveType` (String), `startDate`, `endDate`, `reason`, `status` (String, default "PENDING"), `createdAt`
- Missing: approvedBy, approvalDate, totalDays, isPaid

**`dto/EmployeeRequest.java`** — validated: firstName, lastName, email (@Email), salary (@Positive)
**`dto/EmployeeResponse.java`** — all fields
**`dto/AttendanceRequest.java`** — validated: employeeId, date
**`dto/AttendanceResponse.java`** — includes `employeeName` (computed)
**`dto/LeaveRequest.java`** — validated: employeeId, leaveType, startDate, endDate. **No cross-field validation (end >= start).**
**`dto/LeaveResponse.java`** — all fields

**`repositories`**
- `EmployeeRepository`: `findByEmail(String)`, `findByEmployeeId(String)`
- `AttendanceRepository`: `findByEmployeeId(Long)`
- `LeaveRepository`: `findByEmployeeId(Long)`

**`service/impl/EmployeeServiceImpl.java`** — CRUD with email uniqueness check. No `@Transactional`. Hard delete.

**`service/impl/AttendanceServiceImpl.java`**
- Creates attendance, defaults status to "PRESENT"
- **No duplicate date check** — can create two attendance records for same employee on same day
- No hours-worked computation

**`service/impl/LeaveServiceImpl.java`**
- Creates leave with PENDING status
- `updateLeaveStatus()`: Accepts any string — no lifecycle enforcement. Can go APPROVED→PENDING.

**`controllers`**
- `EmployeeController`: Full CRUD at `/api/employees`. Uses DTOs.
- `AttendanceController`: `POST /`, `GET /`, `GET /employee/{employeeId}`
- `LeaveController`: `POST /`, `GET /`, `GET /employee/{employeeId}`, `PATCH /{id}/status`

**Key architectural issue**: HRM service has **NO Feign clients**. It does not communicate with finance-service for payroll. Finance-service also has NO Feign client to HRM. **Payroll is completely disconnected from employee data.**

---

### 12. FINANCE-SERVICE (Port 8098)

**Purpose**: Chart of accounts, double-entry journal (flawed), payroll (stub).

**`entity/Account.java`**
- `id`, `accountCode` (unique), `accountName`, `accountType` (String — not enum), `description`, `balance` (default ZERO), `createdAt`
- Missing: `parentAccount` (hierarchy), `isActive`, `normalBalance` (DEBIT/CREDIT), `@Version`

**`entity/JournalEntry.java`** ⚠️ **NOT PROPER DOUBLE-ENTRY**
```
Current model (wrong):
  JournalEntry {
    Long id;
    String description;
    BigDecimal debit;    // single value
    BigDecimal credit;   // single value
    Account account;     // single account reference
  }
  // Each row has a debit AND credit for ONE account

Proper double-entry model:
  JournalHeader {
    Long id;
    String entryNumber;
    LocalDate entryDate;
    String description;
    List<JournalLine> lines;  // at least 2
  }
  JournalLine {
    Long id;
    Account account;
    BigDecimal debit;  // non-zero OR
    BigDecimal credit; // non-zero (not both)
    // CONSTRAINT: sum(debits) = sum(credits) per header
  }
```
- Each `JournalEntry` row stores a single debit AND credit for one account
- No way to group lines into a balanced entry (sum debits = sum credits enforced)
- Fields: `id`, `entryNumber` (unique, auto `"JE-" + timestamp`), `description`, `debit`, `credit`, `account` (ManyToOne), `entryDate`, `createdAt`

**`entity/PayrollRecord.java`**
- `employeeId` (String — **no FK to Employee**), `employeeName`, `grossSalary`, `deductions`, `netSalary` (computed as `gross - deductions`), `payPeriodStart`, `payPeriodEnd`, `paymentDate`, `status` (String, default "PENDING"), `createdAt`
- Missing: FK to HRM employee, earnings breakdown (base/OT/bonus), tax breakdown, YTD fields, payslip line items

**`dto/AccountRequest.java`** — validated: accountCode, accountName, accountType. No balance.
**`dto/AccountResponse.java`** — includes balance
**`dto/JournalEntryRequest.java`** — validated: accountId, description, debit (@Positive), credit (@Positive)
- **Single line only** — frontend sends `{ lines: [...] }` which doesn't match this DTO ⚠️
**`dto/JournalEntryResponse.java`** — includes accountName and accountCode
**`dto/PayrollRequest.java`** — validated: employeeId, employeeName, grossSalary (@Positive), deductions, payPeriodStart, payPeriodEnd
- **Frontend sends `{ periodStart, periodEnd }`** which doesn't match this DTO ⚠️

**`service/impl/AccountServiceImpl.java`**
- Standard CRUD. No `@Transactional`. No check for existing journal entries before deletion (FK violation).

**`service/impl/JournalEntryServiceImpl.java`** ⚠️ **BALANCE CALCULATION BUG**

```
@Transactional
createEntry(request):
  1. Find account
  2. balanceChange = debit - credit
  3. account.balance += balanceChange
  4. Save account
  5. Save journal entry
  6. Return response
```

**Critical accounting bug** (line 29): `debit - credit` is applied to **ALL account types** uniformly. This is wrong:
- ASSET, EXPENSE: debit increases, credit decreases → `debit - credit` is CORRECT
- LIABILITY, EQUITY, REVENUE: credit increases, debit decreases → `debit - credit` is **WRONG** (should be `credit - debit`)

**Example**: Posting a $100 credit to `Accounts Payable` (liability):
- Currently: `balanceChange = 0 - 100 = -100` → balance decreases ❌
- Correct: liability balance should INCREASE on credit → `balanceChange = +100`

Other issues:
- **Race condition**: `account.setBalance(account.getBalance().add(balanceChange))` is read-modify-write with no `@Version` on Account
- **Single-line only**: Each call creates one entry against one account. No way to post a balanced two-line entry.
- No `@Version` on JournalEntry either

**`service/impl/PayrollServiceImpl.java`**
- `createPayroll()`: `netSalary = grossSalary - deductions` (simple stub). Does NOT fetch employee data from HRM. Does NOT check for duplicate pay periods. No `@Transactional`.
- `processPayment()`: Sets status to "PAID" and `paymentDate` to today. Does NOT create corresponding journal entries (debit Salary Expense, credit Cash). Payroll is disconnected from accounting.

**`controller/AccountController.java`** — Full CRUD at `/api/accounts`. Uses DTOs.
**`controller/JournalEntryController.java`** — `POST /`, `GET /`, `GET /account/{accountId}`. No update/delete (append-only = good).
**`controller/PayrollController.java`** — `POST /`, `GET /`, `GET /employee/{employeeId}`, `PATCH /{id}/pay`

---

### 13. REPORTING-SERVICE (Port 8099)

**Purpose**: Aggregates data from all services for reports and dashboard. Also seeds demo data.

**`config/RestTemplateConfig.java`**
- Creates `@LoadBalanced RestTemplate` with 3s connect timeout, 10s read timeout

**`dto/` — 5 report DTOs with nested classes**
- `SalesReport`: `Summary` (totalOrders, totalRevenue, averageOrderValue, paid/pending invoices), `OrderSummary`, `DailyRevenue`
- `InventoryReport`: `Summary` (totalProducts, totalStockItems, lowStockCount, outOfStockCount), `StockItem`
- `FinancialReport`: `Summary` (totalAccounts, totalJournalEntries, totalDebits, totalCredits), `AccountBalance` (trial balance), `JournalSummary`
- `HrReport`: `Summary` (total/active/terminated employees, pendingLeaves), `DepartmentSummary`, `PayrollSummary`
- `ProcurementReport`: `Summary` (totalVendors, totalPOs, pending/received counts, totalAmount), `PurchaseOrderSummary`, `VendorActivity`

**`service/ReportService.java`** (341 lines — largest file in the project)
- Fetches data from all services via `RestTemplate` using Eureka service names
- `fetchList(url)`: Generic method returning `List<Map<String, Object>>`. Silently returns empty list on any exception.
- Each report method:
  - Calls 2-3 service endpoints
  - Aggregates in-memory using Java streams
  - Returns a typed DTO
- `getDashboardSummary()`: Quick summary across all modules
- **No caching** — every report call makes multiple HTTP requests in real-time
- **All data pulled into memory** — not scalable for large datasets
- **String-based field access** — `o.get("totalAmount")` with no compile-time safety

**`service/DemoDataService.java`** (230 lines)
- Seeds demo data across all services in sequence
- Creates: 9 accounts, 8 products, 8 stock records, 5 customers, 4 vendors, 7 employees, 5 orders, 4 invoices, 5 purchase orders, 8 journal entries, 6 payroll records
- Uses `RestTemplate.postForObject()` with `Map` payloads
- Silently ignores failures with empty catch blocks
- **Uses hardcoded IDs** (`customerId: 1`) — fragile

**`controller/ReportController.java`**
- `GET /api/reports/dashboard` → dashboard summary
- `GET /api/reports/sales` → sales report
- `GET /api/reports/inventory` → inventory report
- `GET /api/reports/financial` → financial report (+ trial balance)
- `GET /api/reports/hr` → HR report
- `GET /api/reports/procurement` → procurement report

**`controller/DemoDataController.java`**
- `POST /api/reports/seed` → seeds all demo data. Returns map of results.

---

<a name="frontend-architecture"></a>
## Frontend Architecture

**Stack**: React 19, TypeScript 6, Tailwind CSS 4, TanStack Query 5, React Router 7, Vite 8, Axios

### Key Files

**`lib/axios.ts`** — Axios instance pointing to `http://localhost:8092` (gateway)
- Request interceptor: attaches `Authorization: Bearer <token>` from localStorage
- Response interceptor: on 401, clears token and redirects to `/login`
- **No token refresh logic**

**`hooks/useAuth.ts`** — Auth state management
- Stores JWT token in `localStorage` (vulnerable to XSS)
- Stores parsed user object `{ sub, role, iat, exp }` in localStorage
- `login()`: POST to gateway, store token + user
- `logout()`: Clear localStorage. **Does NOT invalidate token server-side** (no blacklist)
- **No silent token refresh** — session ends abruptly at 24h

**`components/Layout.tsx`** — Sidebar navigation showing all modules
- **No role-based filtering** — all menu items visible to all users regardless of role

**`components/ProtectedRoute.tsx`** — Route guard
- Checks `localStorage.getItem('token')` — if absent, redirects to `/login`
- **Does NOT check token expiry** — expired token passes the guard but fails on first API call

**`App.tsx`** — Router with all routes across all 15 feature modules

### Feature Pages (28 .tsx files)

| Feature | Files | Loading State | Empty State | Error State | Validation |
|---------|-------|--------------|-------------|-------------|------------|
| Auth (LoginPage) | 1 | ✅ | N/A | ❌ (generic alert) | Basic |
| Dashboard | 1 | ✅ | ✅ | ❌ | N/A |
| Products | 2 | ✅ | ✅ | ❌ (console.error) | HTML5 only |
| Inventory | 1 | ✅ | ✅ | ❌ (console.error) | Basic |
| Orders | 2 | ✅ | ✅ | ❌ (alert) | HTML5 only |
| Customers | 1 | ✅ | ✅ | ❌ (console.error) | Basic |
| Invoices | 2 | ✅ | ✅ | ❌ (alert) | HTML5 only |
| Vendors | 1 | ✅ | ✅ | ❌ (none) | HTML5 only |
| Purchase Orders | 2 | ✅ | ✅ | ❌ (alert) | HTML5 only |
| Goods Received | 1 | ❌ (dropdown) | N/A | ❌ (alert) | Basic |
| Employees | 2 | ✅ | ✅ | ❌ (alert) | HTML5 only |
| Accounts | 2 | ✅ | ✅ | ❌ (alert) | HTML5 only |
| Journal | 2 | ✅ | ✅ | ❌ (alert) | Client-side balance check |
| Payroll | 2 | ✅ | ✅ | ❌ (alert) | Basic |
| Reports | 6 | ✅ | ✅ | ❌ (console.error) | N/A |

**Frontend weaknesses**:
- **No field-level validation errors** — only HTML5 `required` and `type` attributes
- **No global error boundary** — component crashes show blank screen
- **No toast/notification system** — errors shown via native `alert()`
- **`console.error` used for error handling** in many pages — user sees nothing
- **No pagination on any list page** — all records loaded at once
- **No skeleton loaders** — full-page spinners only
- **Frontend-backend contract mismatch** on payroll (`periodStart` vs `employeeId`) and journal (`lines[]` vs flat fields)

---

<a name="database-schema"></a>
## Database Schema

All tables are auto-generated by Hibernate via `ddl-auto=update`. No explicit migrations.

| Database | Tables | Service |
|----------|--------|---------|
| `erp_db` | `users` (id, username, email, full_name, password, role, created_at) | identity-service |
| `product_db` | `products` (id, name, description, price, sku, stock_quantity) | product-service |
| `inventory_db` | `stocks` (id, product_sku, quantity, warehouse_location) | inventory-service |
| `order_db` | `orders` (id, order_number, customer_name, total_amount, status), `order_items` (id, order_id, product_sku, quantity) | order-service |
| `payment_db` | `payments` (id, order_id, amount, status, payment_method, transaction_id, created_at) | payment-service |
| `sales_db` | `customers` (id, name, email, phone, address, created_at), `invoices` (id, invoice_number, customer_id, total_amount, status, issued_at, due_date) | sales-service |
| `procurement_db` | `vendors` (id, name, email, phone, address, created_at), `purchase_orders` (id, po_number, vendor_id, total_amount, status, ordered_at), `goods_received_notes` (id, grn_number, purchase_order_id, status, received_at) | procurement-service |
| `hrm_db` | `employees` (id, employee_id, first_name, last_name, email, phone, department, position, salary, hire_date, status, created_at), `attendance` (id, employee_id, date, clock_in, clock_out, status), `leaves` (id, employee_id, leave_type, start_date, end_date, reason, status, created_at) | hrm-service |
| `finance_db` | `accounts` (id, account_code, account_name, account_type, description, balance, created_at), `journal_entries` (id, entry_number, description, debit, credit, account_id, entry_date, created_at), `payroll_records` (id, employee_id, employee_name, gross_salary, deductions, net_salary, pay_period_start, pay_period_end, payment_date, status, created_at) | finance-service |

All use `InnoDB` (or whatever PostgreSQL equivalent). No foreign key constraints declared across databases (no cross-database FKs).

---

<a name="critical-bugs"></a>
## Critical Bugs Summary

| # | Bug | Service | File:Line | Impact |
|---|-----|---------|-----------|--------|
| 1 | **JWT algorithm mismatch** — HS256 signing vs HS384 verification | Gateway + Identity | `JwtUtil.java:31` + `SecurityConfig.java:46` | Every authenticated API call returns 401 |
| 2 | **Invoice line items discarded** — items used for stock deduction but never persisted | Sales | `InvoiceServiceImpl.java:31-40` | Invoices have no line-item history |
| 3 | **No order→invoice linkage** — Invoice has no `orderId` field | Sales | `Invoice.java:24-36` | Cannot trace which order an invoice belongs to |
| 4 | **No compensation on multi-step failure** — order saved + stock deducted before payment processed | Order | `OrderService.java:23-50` | If payment fails, stock is lost and order is orphaned |
| 5 | **Race condition on stock** — no `@Version` allows overselling | Inventory | `InventoryServiceImpl.java:33-43` | Two concurrent orders can oversell the same SKU |
| 6 | **Wrong balance calculation** — all accounts treated as debit-normal | Finance | `JournalEntryServiceImpl.java:29` | Liabilities, equity, revenue balances are computed incorrectly |
| 7 | **Frontend-backend contract mismatch (payroll)** — frontend sends `{periodStart, periodEnd}`, backend expects `{employeeId, grossSalary, ...}` | Finance frontend | `CreatePayrollPage.tsx:17` vs `PayrollRequest.java:10-15` | Payroll creation API call will fail |
| 8 | **Frontend-backend contract mismatch (journal)** — frontend sends `{lines: [...]}`, backend expects flat `{accountId, debit, credit}` | Finance frontend | `CreateJournalEntryPage.tsx:36-45` vs `JournalEntryRequest.java:8-13` | Journal entry API call will fail |
| 9 | **Compilation error in common-lib** — duplicate class name + missing symbol | Common-lib | `ErrorResponse.java:10` | common-lib cannot compile |
| 10 | **Webhook handler is a stub** — `handleOrderStatusUpdate()` does nothing | Payment | `PaymentService.java:76-78` | Order status updates from external systems are lost |
| 11 | **Integration test expects webhook to work** — but the implementation is a no-op | Payment | `PaymentServiceIntegrationTest.java:57` | Test will fail |
| 12 | **Broken test double** — `OrderClient.java` has `throw new UnsupportedOperationException` | Payment test | `test/.../OrderClient.java:18-23` | Payment integration tests broken |
| 13 | **Duplicate email allowed** — `User.email` has no `@Column(unique=true)` | Identity | `User.java:22-23` | Multiple users can register with the same email |
| 14 | **No role validation on registration** — client sends `role` field directly | Identity | `RegistrationRequest.java:26` | Clients can register as ADMIN |
| 15 | **No status lifecycle enforcement** — all status fields are plain Strings with no transition validation | All services | Every entity | Orders can go PENDING→DELIVERED, invoices can be created as PAID |
| 16 | **Thread-unsafe ID generation** — all services use `System.currentTimeMillis()` for document numbers | All | Multiple files | Collisions under concurrent requests → duplicate key DB errors |
