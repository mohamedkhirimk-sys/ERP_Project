# Design — Accounting entries on PO receipt (journal ACH)

Date: 2026-08-02 · Status: approved

## Problem

Purchase orders with status RECEIVED generate no accounting entries:

1. procurement-service has no accounting integration — `GoodsReceivedNoteServiceImpl.createGoodsReceivedNote` only adds stock and saves the GRN; PO status updates (`PurchaseOrderServiceImpl.updatePurchaseOrderStatus`) are plain DB updates.
2. finance-service `PostingSource` only supports `ORDER_CONFIRMED` and `PAYMENT_COMPLETED`.
3. The 2 seeded RECEIVED POs (reporting `DemoDataService.java:182,185`, 2500.00 and 1800.00) bypass any receive flow and have no entries; no backfill mechanism exists.

## Decisions (user-approved 2026-08-02)

1. **Posting shape** — mirror of the sales entry (symmetric):
   - journal code **ACH**, description `Achat - <poNumber>`
   - DR 1200 Inventory (net) + DR 2200 Tax Payable (TVA déductible, 20%) + CR 2000 Accounts Payable (gross)
2. **Trigger** — PO status transition to RECEIVED in `updatePurchaseOrderStatus`; idempotent via existing `findBySourceTypeAndSourceId` guard (sourceType `GOODS_RECEIVED`, sourceId = poNumber). Posting failure is logged, status transition still succeeds (same pattern as order-service).
3. **Backfill** — new endpoint `POST /api/purchase-orders/accounting-backfill` that iterates all RECEIVED POs and posts each one (idempotent, safe to re-run) → fixes the 2 seeded POs.

## Scope

**finance-service** (3 files):
- `service/impl/PostingSource.java` — add `GOODS_RECEIVED`
- `service/impl/AccountingPostingServiceImpl.java` — add `postGoodsReceived()` (same structure as `postOrderConfirmed`)
- `src/test/java/com/erp/system/finance/service/AccountingPostingServiceImplTest.java` — add test (TDD)

**procurement-service** (5 files):
- `client/AccountingClient.java` — Feign client to finance `/api/accounting/postings` (pattern: order-service `AccountingClient`)
- `dto/AccountingPostingRequest.java` — `sourceType`, `sourceId`, `totalAmount` (fields matching finance contract)
- `service/impl/PurchaseOrderServiceImpl.java` — hook in `updatePurchaseOrderStatus` (status RECEIVED → post) + `backfillAccounting()` method
- `service/PurchaseOrderService.java` — interface method `backfillAccounting()`
- `controller/PurchaseOrderController.java` — `POST /api/purchase-orders/accounting-backfill`

## Interfaces

- Consumes: `POST /api/accounting/postings` (finance, exists) with body `{sourceType: "GOODS_RECEIVED", sourceId: "<poNumber>", totalAmount: <number>}` → `JournalEntryResponse`. `AccountingPostingRequest` on the finance side: `sourceType`/`sourceId` `@NotBlank`, `totalAmount` `@NotNull @Positive`.
- Produces: `POST /api/purchase-orders/accounting-backfill` → `Map<String, Object>` with `processed` (count of RECEIVED POs iterated) and `created` (count of new entries — difference against re-runs is observable via finance idempotency).
- Idempotency: finance `AccountingPostingServiceImpl.findExisting` returns the existing entry when `sourceType`+`sourceId` already posted; replay returns it without saving.

## Verification

- `mvn.cmd clean compile` in `backend/finance-service` and `backend/procurement-service` — BUILD SUCCESS.
- `mvn.cmd test` in `backend/finance-service` — all tests pass (existing + new GOODS_RECEIVED test).
- E2E: restart finance-service and procurement-service; call `POST /api/purchase-orders/accounting-backfill` via gateway (8082); verify via `GET /api/journal-entries` that 2 new ACH entries exist (POs PO-* seeded RECEIVED) with lines 1200/2200/2000 balanced (net + 20% VAT = gross); re-run backfill → no duplicate entries (idempotency).
