# Foundation First — ERP Cross-Cutting Remediation

**Date:** 2026-07-28
**Status:** Draft

## Objective

Fix 6 cross-cutting concerns across all 13 backend microservices to establish a production-ready foundation, then remediate 16 critical per-module bugs.

## Approach

Fix items sequentially in dependency order. Each item enables cleaner implementation of the next.

## Phase 1: Cross-Cutting Concerns

### 1. JWT Algorithm Mismatch

**Problem:** `identity-service` signs JWTs with HS256, `gateway-service` verifies with HS384 → 401 on every authenticated request.

**Fix:** Change signing algorithm in `JwtUtil.java` from HS256 to HS384 to match gateway's verification config.

**Files affected:**
- `backend/identity-service/.../util/JwtUtil.java` — change algorithm constant
- `backend/gateway-service/.../config/SecurityConfig.java` — verify matches (no change needed, but confirm)

**Risk:** Near-zero. No schema changes, no new dependencies.

---

### 2. @ControllerAdvice — Global Error Handling

**Problem:** Every controller has ad-hoc try/catch blocks returning different error shapes. Some exceptions leak stack traces as 500.

**Solution:** Create a `GlobalExceptionHandler` in `common-lib` that all services inherit.

**Error response shape:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "...",
  "timestamp": "2026-07-28T12:00:00Z",
  "path": "/api/..."
}
```

**Handled exceptions:**
| Exception | HTTP Status |
|---|---|
| `MethodArgumentNotValidException` | 400 (with field-level errors) |
| `ResourceNotFoundException` | 404 |
| `DataIntegrityViolationException` | 409 |
| `AccessDeniedException` | 403 |
| `Exception` (catch-all) | 500 |

**Files affected:** 1 new class in `common-lib`, remove try/catch from ~50 controllers across all services.

---

### 3. RBAC — Role-Based Access Control

**Problem:** No authorization checks on any endpoint. Any authenticated user can call any API.

**Solution:**
- Define roles: `ADMIN`, `MANAGER`, `USER`
- Add `@PreAuthorize` annotations to all controller endpoints
- Store role on `User` entity in `identity-service`, include in JWT claims
- Gateway extracts role from JWT, propagates to downstream services via header
- 403 responses handled by `@ControllerAdvice` from Phase 1.2

**Files affected:** identity-service (User model, JWT claims), all 12 services (controller annotations), gateway (header propagation).

---

### 4. Audit — Who Created/Modified What

**Problem:** No entities track `createdBy`, `createdAt`, `updatedBy`, `updatedAt`.

**Solution:**
- Create `Auditable` base class in `common-lib` with:
  - `@CreatedDate @Column(updatable = false) LocalDateTime createdAt`
  - `@LastModifiedDate LocalDateTime updatedAt`
  - `@CreatedBy @Column(updatable = false) String createdBy`
  - `@LastModifiedBy String updatedBy`
- All entities extend `Auditable`
- `AuditorAware<String>` bean extracts username from `SecurityContextHolder`
- `@EnableJpaAuditing` on each service's config class

**Files affected:** common-lib (Auditable, AuditorAwareImpl), all 13 services (entity extends + config).

---

### 5. Pagination — All List Endpoints

**Problem:** All `findAll()` calls return unfiltered result sets. Will break with real data volumes.

**Solution:**
- All repository `findAll()` calls changed to accept `Pageable`
- All list endpoints return `Page<T>` with `page`, `size`, `totalElements`, `totalPages`
- Default page size = 20, max = 100 (configurable per service)
- Frontend passes `page` and `size` query params

**Files affected:** All 13 services — every controller list endpoint + service method + repository.

---

### 6. Tests — Unit + Integration Scaffolding

**Problem:** Zero tests across the entire codebase.

**Solution:**
- Structure: `src/test/java/com/erp/<service>/...` per service
- Unit tests for service layer (mocked repos) — core business logic
- Integration tests for controller layer (`@WebMvcTest`) — request/response shapes
- Focus on verifying the 5 preceding cross-cutting concerns work correctly
- JUnit 5 + Mockito + Spring Boot Test (already on classpath)
- Target: happy-path + error-path per endpoint

---

## Phase 2: Critical Bug Fixes (after Phase 1)

Fix the 16 critical bugs identified in `docs/codebase-analysis.md`:

### Order-to-Cash (O2C)
1. **Order service** — No transaction compensation on Feign call failures
2. **Order service** — No idempotency key on order creation
3. **Sales service** — Invoice line items accepted in request DTO but never persisted (data loss)
4. **Payment service** — No idempotency on payment processing

### Procure-to-Pay (P2P)
5. **Procurement service** — PO status update lacks optimistic locking
6. **Procurement service** — GRN created outside purchase order context (orphan data)

### Inventory
7. **Inventory service** — Stock quantity update lacks `@Version` (race condition → oversell)
8. **Inventory service** — Negative stock not validated

### Finance
9. **Finance service** — Journal entry uses single-line model instead of header/detail (balance calc wrong for liability/equity/revenue)
10. **Finance service** — No validation that debit = credit

### HR / Payroll
11. **HRM service** — Employee termination doesn't deactivate user account
12. **Payroll** — No validation preventing duplicate payroll runs in same period

### Identity / Gateway
13. **Identity service** — Passwords logged in plain text during registration
14. **Gateway** — No rate limiting

### Product
15. **Product service** — Price update has no audit trail

### Cross-Service
16. **Cross-service** — No correlation ID across service boundaries (can't trace a request through multiple services)

---

## File Manifest (Estimated)

| Concern | Files Changed |
|---|---|
| JWT fix | 2 |
| @ControllerAdvice | 1 new + ~50 modified |
| RBAC | ~200 (all controllers + configs) |
| Audit | 1 new base + ~150 entities + 13 configs |
| Pagination | ~150 (all repos/services/controllers) |
| Tests | ~300 new test files |
| Bug fixes | ~50 |
| **Total** | **~900 files** |
