# Sales Orders + Payments Accounting Backfill Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate the missing accounting entries (Vente + Encaissement) for pre-existing confirmed orders and completed payments via idempotent backfill endpoints.

**Architecture:** order-service gains `POST /api/orders/accounting-backfill` (iterate CONFIRMED orders → post ORDER_CONFIRMED) and payment-service gains `POST /api/payments/accounting-backfill` (iterate COMPLETED payments → post PAYMENT_COMPLETED). Both reuse the existing Feign AccountingClients; finance-side idempotency (`findBySourceTypeAndSourceId`) makes re-runs safe. No finance-service changes.

**Tech Stack:** Java 21, Spring Boot, Spring Cloud OpenFeign, Lombok, JUnit 5 + Mockito + AssertJ (both test files exist).

## Global Constraints

- Per-item try/catch + `log.error` (mirror `OrderService.java:91-94` / `PaymentService.java:66-69`); a failure never aborts the batch and never throws.
- Response shape: `Map.of("processed", <records iterated>, "created", <successful post calls>)` in both endpoints.
- Idempotency: finance `findBySourceTypeAndSourceId` — replays return the existing entry without saving.
- TDD: write the failing test first, verify it fails, implement, verify it passes, run the full service test suite.
- Java verification: `mvn.cmd clean compile`; tests: `mvn.cmd test` (AGENTS.md).
- Commit messages: `feat: ...` per task below.

---

### Task 1: order-service — backfill for confirmed orders

**Files:**
- Modify: `backend/order-service/src/main/java/com/erp/system/order/service/OrderService.java`
- Modify: `backend/order-service/src/main/java/com/erp/system/order/controller/OrderController.java`
- Test: `backend/order-service/src/test/java/com/erp/system/order/service/OrderServiceTest.java`

**Interfaces:**
- Consumes: `orderRepository.findAll()` (`List<OrderEntity>` — fields `orderNumber`, `customerName`, `totalAmount`, `status` String); existing `accountingClient` (Feign `ORDER_CONFIRMED` posting); `@Slf4j` log (already present — `OrderService.java` uses `log.error` at line 92).
- Produces: `OrderService.backfillAccounting()` returning `Map<String, Object>`; controller endpoint `POST /api/orders/accounting-backfill`. Consumed by Task 3 E2E.

- [ ] **Step 1: Write the failing tests**

Append to `OrderServiceTest.java` (before the final closing brace; existing imports cover `List`/`Optional`/`Map` — add `import java.util.Map;`):

```java
    @Test
    void backfillsAccountingEntriesForConfirmedOrders() {
        OrderEntity confirmed = OrderEntity.builder()
                .orderNumber("ORD-1")
                .customerName("Acme")
                .totalAmount(new BigDecimal("100.00"))
                .status("CONFIRMED")
                .build();
        OrderEntity pending = OrderEntity.builder()
                .orderNumber("ORD-2")
                .customerName("Globex")
                .totalAmount(new BigDecimal("50.00"))
                .status("PENDING")
                .build();
        when(orderRepository.findAll()).thenReturn(List.of(confirmed, pending));

        OrderService service = new OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        ArgumentCaptor<AccountingPostingRequest> captor = ArgumentCaptor.forClass(AccountingPostingRequest.class);
        verify(accountingClient).post(captor.capture());
        AccountingPostingRequest posting = captor.getValue();

        assertThat(posting.getSourceType()).isEqualTo("ORDER_CONFIRMED");
        assertThat(posting.getSourceId()).isEqualTo("ORD-1");
        assertThat(posting.getCustomerName()).isEqualTo("Acme");
        assertThat(posting.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result).containsEntry("processed", 1).containsEntry("created", 1);
    }

    @Test
    void doesNotAbortBackfillWhenPostingFails() {
        OrderEntity confirmed = OrderEntity.builder()
                .orderNumber("ORD-1")
                .customerName("Acme")
                .totalAmount(new BigDecimal("100.00"))
                .status("CONFIRMED")
                .build();
        when(orderRepository.findAll()).thenReturn(List.of(confirmed));
        doThrow(new RuntimeException("finance-service down")).when(accountingClient).post(any());

        OrderService service = new OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        assertThat(result).containsEntry("processed", 1).containsEntry("created", 0);
        verify(accountingClient).post(any());
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run in `C:\Users\MED\Documents\ERP_Project\backend\order-service`:
`mvn.cmd test -Dtest=OrderServiceTest`
Expected: FAIL — `backfillAccounting()` does not exist (compile error).

- [ ] **Step 3: Implement backfillAccounting**

In `OrderService.java`:
(a) add import `import java.util.Map;` (keep existing imports);
(b) add the method after `getAllOrders` (before `toResponse`):

```java
    public Map<String, Object> backfillAccounting() {
        List<OrderEntity> confirmed = orderRepository.findAll().stream()
                .filter(o -> "CONFIRMED".equalsIgnoreCase(o.getStatus()))
                .toList();
        int created = 0;
        for (OrderEntity o : confirmed) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("ORDER_CONFIRMED")
                        .sourceId(o.getOrderNumber())
                        .customerName(o.getCustomerName())
                        .totalAmount(o.getTotalAmount())
                        .build());
                created++;
            } catch (Exception e) {
                log.error("Failed to post accounting entry for order {} (customer {}, amount {}): {}",
                        o.getOrderNumber(), o.getCustomerName(), o.getTotalAmount(), e.getMessage());
            }
        }
        return Map.of("processed", confirmed.size(), "created", created);
    }
```

(needs `import java.util.List;` — add it in (a) if not already present.)

- [ ] **Step 4: Add the controller endpoint**

In `OrderController.java`, add `import java.util.Map;` and:

```java
    @PostMapping("/accounting-backfill")
    public ResponseEntity<Map<String, Object>> backfillAccounting() {
        return ResponseEntity.ok(orderService.backfillAccounting());
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run in `C:\Users\MED\Documents\ERP_Project\backend\order-service`:
`mvn.cmd test -Dtest=OrderServiceTest`
Expected: PASS.

- [ ] **Step 6: Run the full test suite + compile**

Run in `C:\Users\MED\Documents\ERP_Project\backend\order-service`:
`mvn.cmd test`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/order-service/src/main/java/com/erp/system/order/service/OrderService.java backend/order-service/src/main/java/com/erp/system/order/controller/OrderController.java backend/order-service/src/test/java/com/erp/system/order/service/OrderServiceTest.java
git commit -m "feat: backfill accounting entries for confirmed orders"
```

---

### Task 2: payment-service — backfill for completed payments

**Files:**
- Modify: `backend/payment-service/src/main/java/com/erp/system/payment/service/PaymentService.java`
- Modify: `backend/payment-service/src/main/java/com/erp/system/payment/controller/PaymentController.java`
- Test: `backend/payment-service/src/test/java/com/erp/system/payment/service/PaymentServiceTest.java`

**Interfaces:**
- Consumes: `paymentRepository.findAll()` (`List<PaymentEntity>` — fields `orderId`, `amount`, `status` `PaymentStatus`); existing `accountingClient` (Feign `PAYMENT_COMPLETED` posting); `@Slf4j` log (present, `PaymentService.java:23`).
- Produces: `PaymentService.backfillAccounting()` returning `Map<String, Object>`; controller endpoint `POST /api/payments/accounting-backfill`. Consumed by Task 3 E2E.

- [ ] **Step 1: Write the failing tests**

Append to `PaymentServiceTest.java` (before the final closing brace; existing imports cover `Optional`/`any`/`doThrow`/`verify`/`when` — add `import java.util.List;` and `import java.util.Map;`):

```java
    @Test
    void backfillsAccountingEntriesForCompletedPayments() {
        PaymentEntity completed = PaymentEntity.builder()
                .id(1L)
                .orderId("ORD-1")
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .build();
        PaymentEntity failed = PaymentEntity.builder()
                .id(2L)
                .orderId("ORD-2")
                .amount(new BigDecimal("50.00"))
                .status(PaymentStatus.FAILED)
                .build();
        when(paymentRepository.findAll()).thenReturn(List.of(completed, failed));

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        ArgumentCaptor<AccountingPostingRequest> captor = ArgumentCaptor.forClass(AccountingPostingRequest.class);
        verify(accountingClient).post(captor.capture());
        AccountingPostingRequest posting = captor.getValue();

        assertThat(posting.getSourceType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(posting.getSourceId()).isEqualTo("ORD-1");
        assertThat(posting.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(result).containsEntry("processed", 1).containsEntry("created", 1);
    }

    @Test
    void doesNotAbortPaymentBackfillWhenPostingFails() {
        PaymentEntity completed = PaymentEntity.builder()
                .id(1L)
                .orderId("ORD-1")
                .amount(new BigDecimal("100.00"))
                .status(PaymentStatus.COMPLETED)
                .build();
        when(paymentRepository.findAll()).thenReturn(List.of(completed));
        doThrow(new RuntimeException("finance-service down")).when(accountingClient).post(any());

        PaymentService service = new PaymentService(paymentRepository, eventPublisher, accountingClient);
        Map<String, Object> result = service.backfillAccounting();

        assertThat(result).containsEntry("processed", 1).containsEntry("created", 0);
        verify(accountingClient).post(any());
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run in `C:\Users\MED\Documents\ERP_Project\backend\payment-service`:
`mvn.cmd test -Dtest=PaymentServiceTest`
Expected: FAIL — `backfillAccounting()` does not exist (compile error).

- [ ] **Step 3: Implement backfillAccounting**

In `PaymentService.java`:
(a) add imports `import java.util.List;` and `import java.util.Map;` (keep existing imports);
(b) add the method after `processPayment` (before `validateRequest`):

```java
    public Map<String, Object> backfillAccounting() {
        List<PaymentEntity> completed = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .toList();
        int created = 0;
        for (PaymentEntity p : completed) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("PAYMENT_COMPLETED")
                        .sourceId(p.getOrderId())
                        .totalAmount(p.getAmount())
                        .build());
                created++;
            } catch (Exception e) {
                log.error("Failed to post accounting entry for payment (order {}, amount {}): {}",
                        p.getOrderId(), p.getAmount(), e.getMessage());
            }
        }
        return Map.of("processed", completed.size(), "created", created);
    }
```

- [ ] **Step 4: Add the controller endpoint**

In `PaymentController.java`, add `import java.util.Map;` and:

```java
    @PostMapping("/accounting-backfill")
    public ResponseEntity<Map<String, Object>> backfillAccounting() {
        return ResponseEntity.ok(paymentService.backfillAccounting());
    }
```

- [ ] **Step 5: Run the tests to verify they pass**

Run in `C:\Users\MED\Documents\ERP_Project\backend\payment-service`:
`mvn.cmd test -Dtest=PaymentServiceTest`
Expected: PASS.

- [ ] **Step 6: Run the full test suite + compile**

Run in `C:\Users\MED\Documents\ERP_Project\backend\payment-service`:
`mvn.cmd test`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/payment-service/src/main/java/com/erp/system/payment/service/PaymentService.java backend/payment-service/src/main/java/com/erp/system/payment/controller/PaymentController.java backend/payment-service/src/test/java/com/erp/system/payment/service/PaymentServiceTest.java
git commit -m "feat: backfill accounting entries for completed payments"
```

---

### Task 3: Restart + E2E verification

**Files:** none (operations task)

**Interfaces:**
- Consumes: running order-service and payment-service (old build on disk) → must be restarted with the newly compiled classes.

- [ ] **Step 1: Restart order-service and payment-service**

Find instances: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*OrderServiceApplication*' -or $_.CommandLine -like '*PaymentServiceApplication*' }` → take ProcessIds and argfile paths (`@C:\...\cp_*.argfile`). Stop each, then relaunch (same argfile, main classes `com.erp.system.order.OrderServiceApplication` and `com.erp.system.payment.PaymentServiceApplication`), redirecting output to `C:\Users\MED\AppData\Local\Temp\opencode\restart_logs\` (order.log / payment.log). Wait ~20s, confirm both `java.exe` processes are running.

- [ ] **Step 2: Run the order backfill**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/api/orders/accounting-backfill" | ConvertTo-Json
```

Expected: `processed` 9, `created` 9 (7 new Vente entries, 2 idempotent replays).

- [ ] **Step 3: Verify journal entries after the order backfill**

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/journal-entries?page=0&size=100" | ConvertTo-Json -Depth 5
```

Expected: total 28 (was 21) — 7 new entries with descriptions `Vente <customer> - Commande ORD-*`, each with lines 1100 DR (net+20%) / 4000 CR (net) / 2200 CR (VAT). Total revenue credited across the 9 Vente entries = 6,349.78. If values differ, capture actual JSON and report — do not fix silently.

- [ ] **Step 4: Run the payment backfill + verify**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/api/payments/accounting-backfill" | ConvertTo-Json
```

Expected: `processed` 7, `created` 7 (5 new Encaissement, 2 replays). Then re-count journal entries: total 33 (was 28).

- [ ] **Step 5: Verify idempotency**

Re-run both backfills, then re-count journal entries. Expected: counts unchanged (28 then 33 — no growth on re-runs).

- [ ] **Step 6: Report results**

Write the E2E results to `.superpowers/sdd/2026-08-02-sales-accounting-backfill/task-3-report.md` (create the directory), including both backfill JSON payloads, the entry counts at each step, and the total revenue credited.

---

## Self-Review Notes

- Spec coverage: order backfill endpoint (T1), payment backfill endpoint (T2), idempotency via finance guard (both — verified T3 S5), per-item try/catch (both tests), creation hooks unchanged (no touch), no finance changes (none).
- No placeholders: every code step contains full code.
- Type consistency: `Map<String, Object>` return + `Map.of("processed", ..., "created", ...)` identical in both services and both controllers; `sourceType` strings `ORDER_CONFIRMED`/`PAYMENT_COMPLETED` match the existing finance enum; filter predicates match entity types (`String.equalsIgnoreCase` for OrderEntity.status, `== PaymentStatus.COMPLETED` for PaymentEntity.status).
- Constructor signatures in tests match existing files: `OrderService(orderRepository, inventoryClient, paymentClient, salesClient, accountingClient)`; `PaymentService(paymentRepository, eventPublisher, accountingClient)`.
- Existing test files are currently untracked in git — they will be committed with these tasks (as noted in the previous plan's final review).
- E2E math: 9 CONFIRMED orders (total 6,349.78), 7 lacking entries; 7 COMPLETED payments, 5 lacking entries. 21 + 7 + 5 = 33 entries after both backfills.
