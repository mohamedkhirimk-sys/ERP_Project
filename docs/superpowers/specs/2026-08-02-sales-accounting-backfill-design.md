# Design — Backfill accounting entries for sales orders and payments

Date: 2026-08-02 · Status: approved

## Problem

The sales report shows 9 CONFIRMED orders (revenue 6,349.78 $) but only 2 of them have accounting entries (Vente). The other 7 — the 6 seeded orders and 1 early test order — were created before the accounting posting module was compiled/live in finance-service; their `ORDER_CONFIRMED` postings failed silently (try/catch + `log.error`, per `OrderService.java:84-94`). Payments have the same issue: 7 COMPLETED payments exist but only 2 have `PAYMENT_COMPLETED` (Encaissement) entries (the same silent-failure pattern in `PaymentService.java:60-69`).

## Decisions (user-approved 2026-08-02)

1. **Backfill sales** — new endpoint `POST /api/orders/accounting-backfill` in order-service: iterate all orders, filter status CONFIRMED, post `ORDER_CONFIRMED` (sourceId = orderNumber, customerName, totalAmount) for each. Idempotent on the finance side (`findBySourceTypeAndSourceId`) — the 2 orders with existing entries replay without duplication.
2. **Backfill payments** — new endpoint `POST /api/payments/accounting-backfill` in payment-service: iterate all payments, filter status COMPLETED, post `PAYMENT_COMPLETED` (sourceId = orderId, amount) for each. Same idempotency.
3. **Failure handling** — per-item try/catch + log; a failure on one order/payment does not abort the batch; counters count only successful posts.
4. Both existing creation hooks stay unchanged (they already post on confirm/complete).

## Scope

**order-service** (3 files):
- `service/OrderService.java` — add `public Map<String, Object> backfillAccounting()`
- `controller/OrderController.java` — add `POST /api/orders/accounting-backfill`
- `src/test/java/com/erp/system/order/service/OrderServiceTest.java` — add tests (TDD)

**payment-service** (3 files):
- `service/PaymentService.java` — add `public Map<String, Object> backfillAccounting()`
- `controller/PaymentController.java` — add `POST /api/payments/accounting-backfill`
- `src/test/java/com/erp/system/payment/service/PaymentServiceTest.java` — add tests (TDD)

## Interfaces

- Consumes: `OrderRepository.findAll()` (`List<OrderEntity>`; fields `orderNumber`, `customerName`, `totalAmount`, `status` String); `PaymentRepository.findAll()` (`List<PaymentEntity>`; fields `orderId`, `amount`, `status` `PaymentStatus`); existing `AccountingClient` in each service (Feign to finance `/api/accounting/postings`); finance idempotency (`findBySourceTypeAndSourceId`).
- Produces: `POST /api/orders/accounting-backfill` → `Map<String, Object>` `{processed, created}`; `POST /api/payments/accounting-backfill` → same shape. `processed` = matching records iterated, `created` = successful post calls (finance idempotency means replays create no new entries).
- No changes to finance-service, no new DTOs, no new Feign clients, no changes to the creation hooks.

## Verification

- `mvn.cmd clean compile` in `backend/order-service` and `backend/payment-service` — BUILD SUCCESS.
- `mvn.cmd test` in both services — all tests pass (existing + new backfill tests).
- E2E: restart order-service and payment-service; run both backfills via gateway (8082); verify:
  - `POST /api/orders/accounting-backfill` → processed 9, created 9; journal entries 21 → 28 (7 new Vente), total revenue credited 6,349.78 (2 replays no-op).
  - `POST /api/payments/accounting-backfill` → processed 7, created 7; journal entries 28 → 33 (5 new Encaissement, 2 replays no-op).
  - Re-run both → entry counts unchanged (idempotency).
