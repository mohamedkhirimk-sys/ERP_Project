# PO Receipt Accounting (ACH) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate ACH journal entries when a purchase order is received (status RECEIVED), and backfill entries for existing RECEIVED POs.

**Architecture:** finance-service gains a `GOODS_RECEIVED` posting source (journal ACH: DR 1200 Inventory net + DR 2200 TVA 20% + CR 2000 Accounts Payable gross), idempotent per sourceType+sourceId. procurement-service posts to it via a new Feign client when a PO transitions to RECEIVED, plus a backfill endpoint for existing RECEIVED POs.

**Tech Stack:** Java 21, Spring Boot, Spring Cloud OpenFeign, Lombok, JUnit 5 + Mockito + AssertJ (finance tests), Maven.

## Global Constraints

- Follow the order-service posting pattern: try/catch around the Feign call, log the error, never fail the business operation (see `OrderService.java:84-94`).
- Idempotency: finance `findExisting(request)` via `findBySourceTypeAndSourceId(sourceType, sourceId)` — replays must not duplicate entries.
- VAT: 20% (`VAT_RATE`), net from request, `vat = net * 20%` scaled 2 decimals HALF_UP, `gross = net + vat`.
- Journal code `ACH`, description `Achat - <sourceId>`.
- Commit messages: `feat: ...` per task below. Java verification: `mvn.cmd clean compile` (AGENTS.md).
- Accounts must exist in finance `DataSeeder`: 1200 Inventory, 2200 Tax Payable, 2000 Accounts Payable (all present).

---

### Task 1: finance-service — GOODS_RECEIVED posting

**Files:**
- Modify: `backend/finance-service/src/main/java/com/erp/system/finance/service/impl/PostingSource.java`
- Modify: `backend/finance-service/src/main/java/com/erp/system/finance/service/impl/AccountingPostingServiceImpl.java`
- Test: `backend/finance-service/src/test/java/com/erp/system/finance/service/AccountingPostingServiceImplTest.java`

**Interfaces:**
- Consumes: `AccountingPostingRequest` (`sourceType`, `sourceId`, `totalAmount`) via `POST /api/accounting/postings` (existing controller, unchanged); `AccountRepository.findByAccountCode`, `JournalEntryRepository.findBySourceTypeAndSourceId`, existing private helpers `findExisting`, `account`, `line`, `buildAndSave`, `toResponse`.
- Produces: `PostingSource.GOODS_RECEIVED`; `AccountingPostingServiceImpl.post()` handles `GOODS_RECEIVED` → `postGoodsReceived(request)` returning `JournalEntryResponse`. Consumed by Task 2's Feign call and by E2E.

- [ ] **Step 1: Write the failing test**

Append to `AccountingPostingServiceImplTest.java` (before the final closing brace; keep existing fields — `taxPayable` is reused, add local `inventory` and `payable` accounts):

```java
    @Test
    void postsBalancedPurchaseEntryForGoodsReceived() {
        Account inventory = account("1200", "Inventory", "ASSET");
        Account payable = account("2000", "Accounts Payable", "LIABILITY");
        when(journalEntryRepository.findBySourceTypeAndSourceId("GOODS_RECEIVED", "PO-1"))
                .thenReturn(Optional.empty());
        when(accountRepository.findByAccountCode("1200")).thenReturn(Optional.of(inventory));
        when(accountRepository.findByAccountCode("2200")).thenReturn(Optional.of(taxPayable));
        when(accountRepository.findByAccountCode("2000")).thenReturn(Optional.of(payable));
        when(journalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.post(request("GOODS_RECEIVED", "PO-1", null, "100.00"));

        ArgumentCaptor<JournalEntry> captor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalEntryRepository).save(captor.capture());
        JournalEntry entry = captor.getValue();

        assertThat(entry.getSourceType()).isEqualTo("GOODS_RECEIVED");
        assertThat(entry.getSourceId()).isEqualTo("PO-1");
        assertThat(entry.getJournalCode()).isEqualTo("ACH");
        assertThat(entry.getDescription()).contains("PO-1");
        assertThat(entry.getLines()).hasSize(3);

        JournalEntryLine invLine = line(entry, "1200");
        assertThat(invLine.getDebit()).isEqualByComparingTo("100.00");
        assertThat(invLine.getCredit()).isZero();

        JournalEntryLine taxLine = line(entry, "2200");
        assertThat(taxLine.getDebit()).isEqualByComparingTo("20.00");
        assertThat(taxLine.getCredit()).isZero();

        JournalEntryLine payableLine = line(entry, "2000");
        assertThat(payableLine.getCredit()).isEqualByComparingTo("120.00");
        assertThat(payableLine.getDebit()).isZero();

        BigDecimal totalDebits = entry.getLines().stream()
                .map(JournalEntryLine::getDebit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCredits = entry.getLines().stream()
                .map(JournalEntryLine::getCredit).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalDebits).isEqualByComparingTo(totalCredits);

        assertThat(inventory.getBalance()).isEqualByComparingTo("100.00");
        assertThat(taxPayable.getBalance()).isEqualByComparingTo("20.00");
        assertThat(payable.getBalance()).isEqualByComparingTo("-120.00");
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run in `C:\Users\MED\Documents\ERP_Project\backend\finance-service`:
`mvn.cmd test -Dtest=AccountingPostingServiceImplTest`
Expected: FAIL — the new test errors because `GOODS_RECEIVED` is not in the `PostingSource` enum (`IllegalArgumentException: Unknown posting source`).

- [ ] **Step 3: Add GOODS_RECEIVED to the enum**

In `PostingSource.java`, change:

```java
public enum PostingSource {
    ORDER_CONFIRMED,
    PAYMENT_COMPLETED;
```

to:

```java
public enum PostingSource {
    ORDER_CONFIRMED,
    PAYMENT_COMPLETED,
    GOODS_RECEIVED;
```

- [ ] **Step 4: Implement postGoodsReceived**

In `AccountingPostingServiceImpl.java`:
(a) in the `post()` switch, add the case:

```java
            case GOODS_RECEIVED -> postGoodsReceived(request);
```

(b) add the method after `postPaymentCompleted` (before `findExisting`):

```java
    private JournalEntryResponse postGoodsReceived(AccountingPostingRequest request) {
        JournalEntry existing = findExisting(request);
        if (existing != null) {
            return toResponse(existing);
        }

        BigDecimal net = request.getTotalAmount();
        BigDecimal vat = net.multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gross = net.add(vat);

        return buildAndSave(JournalEntry.builder()
                        .sourceType(request.getSourceType())
                        .sourceId(request.getSourceId())
                        .journalCode("ACH")
                        .description("Achat - " + request.getSourceId())
                        .build(),
                List.of(
                        line(account("1200"), net, BigDecimal.ZERO),
                        line(account("2200"), vat, BigDecimal.ZERO),
                        line(account("2000"), BigDecimal.ZERO, gross)));
    }
```

- [ ] **Step 5: Run the test to verify it passes**

Run in `C:\Users\MED\Documents\ERP_Project\backend\finance-service`:
`mvn.cmd test -Dtest=AccountingPostingServiceImplTest`
Expected: PASS — all tests in the class green.

- [ ] **Step 6: Run the full test suite**

Run in `C:\Users\MED\Documents\ERP_Project\backend\finance-service`:
`mvn.cmd test`
Expected: `BUILD SUCCESS` — all finance tests pass (existing suite, including TreasuryServiceImplTest, JournalServiceImplTest, etc.).

- [ ] **Step 7: Commit**

```bash
git add backend/finance-service/src/main/java/com/erp/system/finance/service/impl/PostingSource.java backend/finance-service/src/main/java/com/erp/system/finance/service/impl/AccountingPostingServiceImpl.java backend/finance-service/src/test/java/com/erp/system/finance/service/AccountingPostingServiceImplTest.java
git commit -m "feat: post ACH journal entry on goods received (GOODS_RECEIVED)"
```

---

### Task 2: procurement-service — posting hook + backfill endpoint

**Files:**
- Create: `backend/procurement-service/src/main/java/com/erp/system/procurement/dto/AccountingPostingRequest.java`
- Create: `backend/procurement-service/src/main/java/com/erp/system/procurement/client/AccountingClient.java`
- Modify: `backend/procurement-service/src/main/java/com/erp/system/procurement/service/PurchaseOrderService.java` (interface — add `Map<String, Object> backfillAccounting();` and its import)
- Modify: `backend/procurement-service/src/main/java/com/erp/system/procurement/service/impl/PurchaseOrderServiceImpl.java`
- Modify: `backend/procurement-service/src/main/java/com/erp/system/procurement/controller/PurchaseOrderController.java`

**Interfaces:**
- Consumes: Task 1's `GOODS_RECEIVED` posting via `POST /api/accounting/postings` (finance, Eureka name `finance-service`); `PurchaseOrderRepository.findAll()`; `PurchaseOrder` entity (`getStatus()`, `getPoNumber()`, `getTotalAmount()`).
- Produces: `POST /api/purchase-orders/accounting-backfill` → `Map<String, Object>` with `processed` (RECEIVED POs iterated) and `created` (posting calls made; finance idempotency means re-runs create no new entries). Feign client method `AccountingClient.post(AccountingPostingRequest)`.

- [ ] **Step 1: Create the DTO**

Create `backend/procurement-service/src/main/java/com/erp/system/procurement/dto/AccountingPostingRequest.java`:

```java
package com.erp.system.procurement.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountingPostingRequest {
    private String sourceType;
    private String sourceId;
    private BigDecimal totalAmount;
}
```

- [ ] **Step 2: Create the Feign client**

Create `backend/procurement-service/src/main/java/com/erp/system/procurement/client/AccountingClient.java` (mirror of order-service's `AccountingClient`):

```java
package com.erp.system.procurement.client;

import com.erp.system.procurement.dto.AccountingPostingRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "finance-service", path = "/api/accounting/postings")
public interface AccountingClient {

    @PostMapping
    void post(@RequestBody AccountingPostingRequest request);
}
```

- [ ] **Step 3: Extend the service interface**

In `PurchaseOrderService.java`, add the import `import java.util.Map;` and the method:

```java
    Map<String, Object> backfillAccounting();
```

- [ ] **Step 4: Implement the hook and backfill**

In `PurchaseOrderServiceImpl.java`:
(a) add imports `import com.erp.system.procurement.client.AccountingClient;`, `import com.erp.system.procurement.dto.AccountingPostingRequest;`, `import org.slf4j.Logger;`, `import org.slf4j.LoggerFactory;`, `import java.util.Map;` (keep existing imports);
(b) add the field after `vendorRepository`:

```java
    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    private final AccountingClient accountingClient;
```

(c) replace the `updatePurchaseOrderStatus` body with:

```java
    @Override
    public PurchaseOrderResponse updatePurchaseOrderStatus(Long id, String status) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PurchaseOrder not found with id: " + id));
        po.setStatus(status);
        PurchaseOrderResponse response = toResponse(purchaseOrderRepository.save(po));

        if ("RECEIVED".equalsIgnoreCase(status)) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("GOODS_RECEIVED")
                        .sourceId(po.getPoNumber())
                        .totalAmount(po.getTotalAmount())
                        .build());
            } catch (Exception e) {
                log.error("Failed to post accounting entry for PO {} (amount {}): {}",
                        po.getPoNumber(), po.getTotalAmount(), e.getMessage());
            }
        }
        return response;
    }
```

(d) add the backfill method after `updatePurchaseOrderStatus`:

```java
    @Override
    public Map<String, Object> backfillAccounting() {
        List<PurchaseOrder> received = purchaseOrderRepository.findAll().stream()
                .filter(po -> "RECEIVED".equalsIgnoreCase(po.getStatus()))
                .toList();
        int created = 0;
        for (PurchaseOrder po : received) {
            try {
                accountingClient.post(AccountingPostingRequest.builder()
                        .sourceType("GOODS_RECEIVED")
                        .sourceId(po.getPoNumber())
                        .totalAmount(po.getTotalAmount())
                        .build());
                created++;
            } catch (Exception e) {
                log.error("Failed to post accounting entry for PO {} (amount {}): {}",
                        po.getPoNumber(), po.getTotalAmount(), e.getMessage());
            }
        }
        return Map.of("processed", received.size(), "created", created);
    }
```

(needs `import java.util.List;` — NOT present in the file; add it with the other imports in 4(a).)

- [ ] **Step 5: Add the controller endpoint**

In `PurchaseOrderController.java`, add the import `import java.util.Map;` and the endpoint after `updatePurchaseOrderStatus`:

```java
    @PostMapping("/accounting-backfill")
    public ResponseEntity<Map<String, Object>> backfillAccounting() {
        return ResponseEntity.ok(purchaseOrderService.backfillAccounting());
    }
```

- [ ] **Step 6: Compile**

Run in `C:\Users\MED\Documents\ERP_Project\backend\procurement-service`:
`mvn.cmd clean compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add backend/procurement-service/src/main/java/com/erp/system/procurement/dto/AccountingPostingRequest.java backend/procurement-service/src/main/java/com/erp/system/procurement/client/AccountingClient.java backend/procurement-service/src/main/java/com/erp/system/procurement/service/PurchaseOrderService.java backend/procurement-service/src/main/java/com/erp/system/procurement/service/impl/PurchaseOrderServiceImpl.java backend/procurement-service/src/main/java/com/erp/system/procurement/controller/PurchaseOrderController.java
git commit -m "feat: post accounting entry on PO receipt + backfill endpoint"
```

---

### Task 3: Restart + E2E verification

**Files:** none (operations task)

**Interfaces:**
- Consumes: running finance-service and procurement-service (old build on disk) → must be restarted with the newly compiled classes.

- [ ] **Step 1: Restart finance-service and procurement-service**

Find instances: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -like '*FinanceApplication*' -or $_.CommandLine -like '*ProcureApplication*' }` → take ProcessIds and argfile paths (`@C:\...\cp_*.argfile`). Stop each, then relaunch (same argfile, main classes `com.erp.system.finance.FinanceApplication` and `com.erp.system.procurement.ProcureApplication`), redirecting output to `C:\Users\MED\AppData\Local\Temp\opencode\restart_logs\` (finance.log / procure.log). Wait ~20s, confirm both `java.exe` processes are running.

- [ ] **Step 2: Run the backfill**

```powershell
Invoke-RestMethod -Method Post -Uri "http://localhost:8082/api/purchase-orders/accounting-backfill" | ConvertTo-Json
```

Expected on current data: `processed` ≥ 2 (the seeded RECEIVED POs), `created` = same count (posting calls made).

- [ ] **Step 3: Verify the ACH entries**

```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/journal-entries?page=0&size=50" | ConvertTo-Json -Depth 5
```

Expected: 2 new entries with journalCode ACH, descriptions `Achat - <poNumber>`, each with 3 lines (1200 net / 2200 20% VAT / 2000 gross, balanced). Seeded amounts: 2500.00 → 2500/500/3000; 1800.00 → 1800/360/2160. If values differ, capture actual JSON and report — do not fix silently.

- [ ] **Step 4: Verify idempotency**

Run the backfill POST again, then re-count ACH journal entries. Expected: entry count unchanged (no duplicates).

- [ ] **Step 5: Report results**

Write the E2E results to `.superpowers/sdd/2026-08-02-po-receipt-accounting/task-3-report.md` (create the directory), including both backfill JSON payloads, the journal entries count before/after re-run, and the ACH entry lines.

---

## Self-Review Notes

- Spec coverage: PostingSource GOODS_RECEIVED (T1 S3), postGoodsReceived ACH shape DR 1200+DR 2200/CR 2000 (T1 S4, tested T1 S1), trigger on RECEIVED with try/catch log pattern (T2 S4c), backfill endpoint (T2 S4d+S5), idempotency (finance findExisting reused — T1 S4a; verified T3 S4), E2E restart+verify (T3).
- No placeholders: every code step contains full code.
- Type consistency: `GOODS_RECEIVED` string used in T1 (enum + test) and T2 (Feign body) identically; `AccountingPostingRequest` field names `sourceType`/`sourceId`/`totalAmount` match the finance DTO; `Map<String, Object>` return type consistent between interface (T2 S3) and impl (T2 S4d) and controller (T2 S5).
- Amount math for seeded POs: 2500 × 20% = 500, gross 3000; 1800 × 20% = 360, gross 2160.
- procurement-service has no test directory — verification for T2 is compile + E2E (noted in plan; no test infrastructure exists to extend).
