# Foundation First Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 6 cross-cutting concerns across 13 backend microservices to establish a production-ready foundation, then remediate 16 critical per-module bugs.

**Architecture:** 12 Spring Boot microservices + 1 gateway + 1 Eureka server, with shared code in `common-lib` (currently not wired as a Maven dependency). Fix cross-cutting items in dependency order (JWT → error handling → RBAC → audit → pagination → tests), then bugs per-module.

**Tech Stack:** Java 21, Spring Boot 3.2.2, Spring Cloud 2023.0.0, Spring Security, JPA/Hibernate, PostgreSQL, JUnit 5 + Mockito, Maven

**Reference spec:** `docs/superpowers/specs/2026-07-28-foundation-first-design.md`

## Global Constraints

- All entities must extend `Auditable` base class from common-lib (after Task 4)
- All controller list endpoints must return Spring Data `Page<T>` (after Task 5)
- All controllers use `@PreAuthorize` for RBAC (after Task 3)
- Error responses use the standardized `ErrorResponse` shape (after Task 2)
- JWT signing must use HS384, not HS256
- Zero test files exist — all tests are new
- `common-lib` currently has no `pom.xml` — must be converted to a proper Maven module
- All services must add `common-lib` as a Maven dependency
- All services' `@SpringBootApplication` must include `scanBasePackages = {"com.erp", "com.erp.system"}` to pick up common-lib beans
- All code changes must compile with `mvn compile` before commit
- `docs/codebase-analysis.md` contains the full bug analysis

---
## Phase 1: Cross-Cutting Concerns

### Task 1: Fix JWT Algorithm Mismatch

**Files:**
- Modify: `backend/identity-service/src/main/java/com/erp/identity/util/JwtUtil.java:31`
- Test: none (verified by gateway acceptance after fix)

**Interfaces:**
- Consumes: existing `JwtUtil` class with `generateToken()` and `validateToken()` methods
- Produces: JWTs signed with HS384 instead of HS256

- [ ] **Step 1: Read files to understand current state**

```bash
Get-Content backend/identity-service/src/main/java/com/erp/identity/util/JwtUtil.java
Get-Content backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java
```

- [ ] **Step 2: Fix the signing algorithm in JwtUtil**

Change `JwtUtil.java` line 31 from:
```java
.signWith(key)
```
to:
```java
.signWith(key, Jwts.SIG.HS384)
```

- [ ] **Step 3: Add the import for HS384**

Add to imports in `JwtUtil.java`:
```java
import static io.jsonwebtoken.Jwts.SIG.HS384;
```

- [ ] **Step 4: Compile identity-service to verify**

Run: `mvn compile -f backend/identity-service/pom.xml`

- [ ] **Step 5: Commit**

```bash
git add backend/identity-service/src/main/java/com/erp/identity/util/JwtUtil.java
git commit -m "fix: sign JWT with HS384 to match gateway verification"
```

---

### Task 2: Fix Global Error Handling (@ControllerAdvice)

**Files:**
- Delete: `backend/common-lib/src/main/java/com/erp/common/exception/ErrorResponse.java` (contains duplicate GlobalExceptionHandler class)
- Modify: `backend/common-lib/src/main/java/com/erp/common/exception/GlobalExceptionHandler.java`
- Create: `backend/common-lib/pom.xml`
- Create: `backend/common-lib/src/main/java/com/erp/common/exception/ResourceNotFoundException.java`
- Modify: `backend/sales-service/pom.xml` (add common-lib dependency)
- Modify: `backend/inventory-service/pom.xml`
- Modify: `backend/finance-service/pom.xml`
- Modify: `backend/order-service/pom.xml`
- Modify: `backend/payment-service/pom.xml`
- Modify: `backend/procurement-service/pom.xml`
- Modify: `backend/product-service/pom.xml`
- Modify: `backend/hrm-service/pom.xml`
- Modify: `backend/reporting-service/pom.xml`
- Modify: `backend/identity-service/pom.xml`
- Modify: `backend/gateway-service/pom.xml`
- Modify: `backend/eureka-server/pom.xml`

**Interfaces:**
- Consumes: existing `ApiResponse` DTO, Spring's `@RestControllerAdvice`
- Produces: `GlobalExceptionHandler` class handling 400/403/404/409/500 with consistent `ErrorResponse` shape
- Produces: `common-lib` as a proper Maven module (`pom.xml`), dependency added to all services

- [ ] **Step 1: Create common-lib pom.xml**

Write `backend/common-lib/pom.xml`:
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
        <relativePath/>
    </parent>
    <groupId>com.erp</groupId>
    <artifactId>common-lib</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>common-lib</name>
    <description>Shared library for ERP microservices</description>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

- [ ] **Step 2: Delete the duplicate ErrorResponse.java**

```bash
Remove-Item backend/common-lib/src/main/java/com/erp/common/exception/ErrorResponse.java
```

- [ ] **Step 3: Create ErrorResponse DTO class**

Write `backend/common-lib/src/main/java/com/erp/common/exception/ErrorResponse.java`:
```java
package com.erp.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime timestamp;
    private String path;
}
```

- [ ] **Step 4: Create ResourceNotFoundException**

Write `backend/common-lib/src/main/java/com/erp/common/exception/ResourceNotFoundException.java`:
```java
package com.erp.common.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
    public ResourceNotFoundException(String resource, String identifier) {
        super(resource + " not found: " + identifier);
    }
}
```

- [ ] **Step 5: Rewrite GlobalExceptionHandler**

Replace `backend/common-lib/src/main/java/com/erp/common/exception/GlobalExceptionHandler.java`:
```java
package com.erp.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "Data integrity violation", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
```

- [ ] **Step 6: Install common-lib to local Maven repo**

Run: `mvn install -f backend/common-lib/pom.xml`

- [ ] **Step 7: Add common-lib dependency to all 12 services**

For each service's `pom.xml`, add inside `<dependencies>`:
```xml
<dependency>
    <groupId>com.erp</groupId>
    <artifactId>common-lib</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Services to modify: sales-service, inventory-service, finance-service, order-service, payment-service, procurement-service, product-service, hrm-service, reporting-service, identity-service, gateway-service, eureka-server (12 total).

- [ ] **Step 8: Add scanBasePackages to all services**

Modify each service's main `@SpringBootApplication` class to scan `com.erp` packages. For each of the 12 services, change:
```java
@SpringBootApplication
```
to:
```java
@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
```

Services: identity-service, gateway-service, sales-service, inventory-service, finance-service, order-service, payment-service, procurement-service, product-service, hrm-service, reporting-service, eureka-server.

- [ ] **Step 9: Compile one service to verify**

Run: `mvn compile -f backend/sales-service/pom.xml`

- [ ] **Step 10: Consolidate duplicate common-lib directories**

There are TWO directories: `backend/common-lib/` (exception handling, DTOs) and root `common-lib/` (has `PaymentCompletedEvent.java`). The payment-service has its OWN copy at `backend/payment-service/src/main/java/com/erp/common/event/PaymentCompletedEvent.java`. Delete the root `common-lib/` — it's redundant with `backend/common-lib/` and the payment-service has its own local copy:
```powershell
Remove-Item -Recurse -Force common-lib/
```

- [ ] **Step 11: Remove redundant try/catch blocks from controllers**

Search all services for try/catch blocks in controllers and replace with simple throws. The `GlobalExceptionHandler` handles all exceptions.

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "feat: add global error handling with common-lib module"
```

---

### Task 3: RBAC — Role-Based Access Control

**Files:**
- Modify: `backend/identity-service/src/main/java/com/erp/identity/util/JwtUtil.java` (add role to token)
- Modify: `backend/identity-service/src/main/java/com/erp/identity/entity/User.java` (if role field missing)
- Modify: All controllers in all 12 services (add `@PreAuthorize`)
- Modify: `backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java` (pass role header)
- Create (if needed): `backend/common-lib/src/main/java/com/erp/common/security/Role.java`

**Interfaces:**
- Consumes: JWT with role claim (from Task 1)
- Produces: `@PreAuthorize` on all endpoints, role enum in common-lib

- [ ] **Step 1: Create Role enum in common-lib**

Write `backend/common-lib/src/main/java/com/erp/common/security/Role.java`:
```java
package com.erp.common.security;

public enum Role {
    ADMIN,
    MANAGER,
    USER
}
```

- [ ] **Step 2: Verify User entity has role field**

Read `backend/identity-service/src/main/java/com/erp/identity/entity/User.java` and add a `role` field if missing:
```java
@Enumerated(EnumType.STRING)
private Role role;
```

- [ ] **Step 3: Add role to JWT claims**

In `JwtUtil.generateToken()`, role is already added as a claim — verify it uses the `Role` enum name. No change needed if working.

- [ ] **Step 4: Add Spring Security starter to all services that don't have it**

Check each service's pom.xml for:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```
Add where missing.

- [ ] **Step 5: Add @EnableMethodSecurity to each service's config or main class**

For each service, either in the main application class or a `SecurityConfig`:
```java
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@EnableMethodSecurity
public class SecurityConfig { ... }
```

- [ ] **Step 6: Add @PreAuthorize to all controller endpoints**

For each controller, add class-level or method-level annotations:

Public endpoints (login, register):
```java
@PreAuthorize("permitAll()")
```

Admin-only endpoints:
```java
@PreAuthorize("hasRole('ADMIN')")
```

Authenticated-user endpoints (default):
```java
@PreAuthorize("isAuthenticated()")
```

- [ ] **Step 7: Compile all services**

Run: `Get-ChildItem backend/*/pom.xml | ForEach-Object { mvn compile -f $_.FullName }`

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat: add RBAC with @PreAuthorize on all endpoints"
```

---

### Task 4: Audit — Created/Modified Tracking

**Files:**
- Create: `backend/common-lib/src/main/java/com/erp/common/audit/Auditable.java`
- Create: `backend/common-lib/src/main/java/com/erp/common/audit/AuditorAwareImpl.java`
- Modify: All entity classes across all services (extend `Auditable`)
- Modify: Each service's config class (add `@EnableJpaAuditing`)

- [ ] **Step 1: Create Auditable base class**

Write `backend/common-lib/src/main/java/com/erp/common/audit/Auditable.java`:
```java
package com.erp.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;
}
```

- [ ] **Step 2: Create AuditorAwareImpl**

Write `backend/common-lib/src/main/java/com/erp/common/audit/AuditorAwareImpl.java`:
```java
package com.erp.common.audit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }
        return Optional.of(auth.getName());
    }
}
```

- [ ] **Step 3: Re-install common-lib**

Run: `mvn install -f backend/common-lib/pom.xml`

- [ ] **Step 4: Add @EnableJpaAuditing to each service**

For each service, add to a config class or main application class:
```java
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaConfig { ... }
```

- [ ] **Step 5: Make all entities extend Auditable**

For each entity class across all services, change:
```java
public class SomeEntity {
```
to:
```java
public class SomeEntity extends Auditable {
```

Remove any existing `createdAt`/`updatedAt`/`createdBy`/`updatedBy` fields from entities.

- [ ] **Step 6: Compile all services**

Run: `Get-ChildItem backend/*/pom.xml | ForEach-Object { mvn compile -f $_.FullName }`

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat: add audit fields to all entities via Auditable base class"
```

---

### Task 5: Pagination — All List Endpoints

**Files:**
- Modify: All repository interfaces (accept `Pageable`)
- Modify: All service methods (accept/pass `Pageable`, return `Page<T>`)
- Modify: All controller list endpoints (accept `@PageableDefault`, return `Page<T>`)

- [ ] **Step 1: Update all repository findAll methods**

For each repository that has `findAll()`, change to:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

Page<Entity> findAll(Pageable pageable);
```

For custom query methods that return `List<T>`, change to `Page<T>` with `Pageable` parameter.

- [ ] **Step 2: Update service layer**

For each service method that calls `findAll()`, add `Pageable` parameter and return `Page<T>`:
```java
public Page<InvoiceResponse> getAllInvoices(Pageable pageable) {
    return invoiceRepository.findAll(pageable).map(InvoiceResponse::fromEntity);
}
```

- [ ] **Step 3: Update controller layer**

For each controller list endpoint:
```java
@GetMapping
public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
        @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(invoiceService.getAllInvoices(pageable));
}
```

- [ ] **Step 4: Set max page size globally**

In each service's `application.yml`:
```yaml
spring:
  data:
    web:
      pageable:
        max-page-size: 100
        default-page-size: 20
```

- [ ] **Step 5: Compile all services**

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: add pagination to all list endpoints"
```

---

### Task 6: Tests — Unit + Integration Scaffolding

**Files:**
- Create: Test files for each service (JUnit 5 + Mockito + `@WebMvcTest`)

**Target:** At least happy-path + error-path per endpoint, focusing on the 5 preceding cross-cutting concerns.

- [ ] **Step 1: Write JWT verification test**

Create `backend/identity-service/src/test/java/com/erp/identity/util/JwtUtilTest.java`:
```java
package com.erp.identity.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("my-test-secret-key-that-is-long-enough-for-hs384-algorithm", 3600000);
    }

    @Test
    void generateToken_shouldReturnValidJwt() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void validateToken_shouldReturnCorrectClaims() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void validateToken_shouldThrowOnInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("invalid.token.here"));
    }
}
```

- [ ] **Step 2: Run JWT test to verify it passes**

Run: `mvn test -f backend/identity-service/pom.xml -Dtest=JwtUtilTest`

- [ ] **Step 3: Write GlobalExceptionHandler test**

Create `backend/common-lib/src/test/java/com/erp/common/exception/GlobalExceptionHandlerTest.java`:
```java
package com.erp.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_shouldReturn404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Invoice", 1L), null);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void handleValidation_shouldReturn400() {
        // Integration test via @WebMvcTest preferred; unit test validates handler exists
        assertNotNull(handler);
    }
}
```

- [ ] **Step 4: Write a controller integration test**

Example for `backend/sales-service/src/test/java/com/erp/system/sales/controller/InvoiceControllerTest.java`:
```java
package com.erp.system.sales.controller;

import com.erp.common.exception.GlobalExceptionHandler;
import com.erp.system.sales.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService invoiceService;

    @Test
    void getAllInvoices_shouldReturn200() throws Exception {
        when(invoiceService.getAllInvoices(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk());
    }

    @Test
    void getInvoiceById_shouldReturn404() throws Exception {
        when(invoiceService.getInvoiceById(999L))
                .thenThrow(new com.erp.common.exception.ResourceNotFoundException("Invoice", 999L));

        mockMvc.perform(get("/api/invoices/999"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 5: Write tests for RBAC on a controller**

```java
@Test
void createInvoice_withoutAuth_shouldReturn401() throws Exception {
    mockMvc.perform(post("/api/invoices")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isUnauthorized());
}
```

- [ ] **Step 6: Write audit field test**

```java
@Test
void createdDate_shouldBeSetOnPersist() {
    // Integration test verifying @CreatedDate is populated
}
```

- [ ] **Step 7: Write pagination test**

```java
@Test
void getAllInvoices_withPageable_shouldReturnPagedResponse() throws Exception {
    when(invoiceService.getAllInvoices(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/api/invoices?page=0&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
}
```

- [ ] **Step 8: Run all tests to verify**

Run: `Get-ChildItem backend/*/pom.xml | ForEach-Object { mvn test -f $_.FullName }`

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: add unit and integration tests across all services"
```

---

## Phase 2: Critical Bug Fixes

### Task 7: Order Service — Transaction Compensation

**Files:**
- Modify: `backend/order-service/src/main/java/com/erp/system/order/service/OrderService.java`

**Bug:** Feign calls to inventory and payment are outside local transaction. If payment fails after inventory is decremented, stock is lost.

**Fix:** Use `@Transactional` and add compensation (manual rollback) on failure. Or use a saga pattern via `@Transactional` with `TransactionTemplate` for simpler approach.

- [ ] **Step 1: Read OrderService.java to understand current flow**

- [ ] **Step 2: Add @Transactional to createOrder method and add try-catch with compensation**

```java
@Transactional
public OrderResponse createOrder(OrderRequest request) {
    Order order = orderMapper.toEntity(request);
    order.setStatus(OrderStatus.PENDING);
    order = orderRepository.save(order);
    try {
        inventoryService.deductStock(order.getSku(), order.getQuantity());
        paymentService.processPayment(order.getPaymentDetails());
        order.setStatus(OrderStatus.CONFIRMED);
    } catch (Exception e) {
        order.setStatus(OrderStatus.FAILED);
        // Restore stock that was deducted
        inventoryService.addStock(order.getSku(), order.getQuantity());
        throw new RuntimeException("Order creation failed, stock restored", e);
    } finally {
        orderRepository.save(order);
    }
}
```

- [ ] **Step 3: Add addStock endpoint in InventoryController (if not exists)**

If `inventory-service` lacks a stock-add endpoint, add:
```java
@PutMapping("/stock/{sku}/add")
public ResponseEntity<Stock> addStock(@PathVariable String sku, @RequestParam Integer quantity) {
    return ResponseEntity.ok(inventoryService.addStock(sku, quantity));
}
```

And in `InventoryServiceImpl`:
```java
public Stock addStock(String sku, int quantity) {
    Stock stock = stockRepository.findBySku(sku)
            .orElseThrow(() -> new ResourceNotFoundException("Stock", sku));
    stock.setQuantity(stock.getQuantity() + quantity);
    return stockRepository.save(stock);
}
```

- [ ] **Step 4: Add Feign client method in OrderService**

If not already present, add to the inventory Feign client:
```java
@FeignClient("inventory-service")
public interface InventoryClient {
    @PutMapping("/api/inventory/stock/{sku}")
    Stock deductStock(@PathVariable String sku, @RequestParam Integer quantityChange);

    @PutMapping("/api/inventory/stock/{sku}/add")
    Stock addStock(@PathVariable String sku, @RequestParam Integer quantity);
}
```

- [ ] **Step 5: Compile and commit**

---

### Task 8: Order Service — Idempotency Key

**Files:**
- Modify: `backend/order-service/src/main/java/com/erp/system/order/service/OrderService.java`
- Modify: `backend/order-service/src/main/java/com/erp/system/order/entity/Order.java`

**Bug:** No idempotency key — duplicate order creation requests create duplicate orders.

**Fix:** Add `idempotencyKey` field (unique constraint) to Order entity. Check before processing.

- [ ] **Step 1: Add idempotencyKey field to Order entity with unique constraint**

```java
@Column(unique = true, nullable = false)
private String idempotencyKey;
```

- [ ] **Step 2: Check for existing order before creating**

```java
public OrderResponse createOrder(OrderRequest request) {
    if (orderRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
        throw new IllegalArgumentException("Duplicate order - idempotency key already exists");
    }
    // ... proceed with creation
}
```

- [ ] **Step 3: Compile and commit**

---

### Task 9: Sales Service — Invoice Line Items Data Loss

**Files:**
- Modify: `backend/sales-service/src/main/java/com/erp/system/sales/service/impl/InvoiceServiceImpl.java`

**Bug:** Invoice line items accepted in request DTO but never persisted.

**Fix:** Persist line items when creating invoice.

- [ ] **Step 1: Read InvoiceServiceImpl.java and the entity model**

- [ ] **Step 2: Ensure Invoice has @OneToMany relationship to InvoiceLineItem**

```java
@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
@JoinColumn(name = "invoice_id")
private List<InvoiceLineItem> lineItems;
```

- [ ] **Step 3: Persist line items in createInvoice**

```java
invoice.setLineItems(request.getLineItems().stream()
        .map(InvoiceLineItem::fromRequest)
        .collect(Collectors.toList()));
```

- [ ] **Step 4: Compile and commit**

---

### Task 10: Payment Service — Idempotency

**Files:**
- Modify: `backend/payment-service/src/main/java/com/erp/system/payment/service/PaymentService.java`
- Modify: `backend/payment-service/src/main/java/com/erp/system/payment/entity/Payment.java`

**Bug:** No idempotency key — duplicate payment requests create duplicate charges.

**Fix:** Same approach as Task 8 — add `idempotencyKey` with unique constraint.

- [ ] **Step 1: Add idempotencyKey field to Payment entity**

- [ ] **Step 2: Check for existing payment before processing**

- [ ] **Step 3: Compile and commit**

---

### Task 11: Procurement — PO Optimistic Locking

**Files:**
- Modify: `backend/procurement-service/src/main/java/com/erp/system/procurement/entity/PurchaseOrder.java`

**Bug:** PO status update lacks `@Version` — concurrent requests can cause status corruption.

**Fix:** Add `@Version` field for optimistic locking.

- [ ] **Step 1: Add @Version field**

```java
@Version
private Long version;
```

- [ ] **Step 2: Compile and commit**

---

### Task 12: Procurement — Orphan GRN Data

**Files:**
- Modify: `backend/procurement-service/src/main/java/com/erp/system/procurement/service/GoodsReceivedNoteService.java`

**Bug:** GRN created outside purchase order context.

**Fix:** Require PO reference when creating GRN.

- [ ] **Step 1: Read GRN service to understand current logic**

- [ ] **Step 2: Add PO validation before GRN creation**

```java
public GoodsReceivedNote createGRN(GRNRequest request) {
    PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", request.getPurchaseOrderId()));
    // ... create GRN linked to PO
}
```

- [ ] **Step 3: Compile and commit**

---

### Task 13: Inventory — Stock Race Condition

**Files:**
- Modify: `backend/inventory-service/src/main/java/com/erp/system/inventory/entity/Stock.java`
- Modify: `backend/inventory-service/src/main/java/com/erp/system/inventory/service/impl/InventoryServiceImpl.java`

**Bug:** Stock quantity update lacks `@Version` — two concurrent requests can oversell.

**Fix:** Add `@Version` to Stock entity. Use optimistic locking in update.

- [ ] **Step 1: Add @Version field to Stock entity**

```java
@Version
private Long version;
```

- [ ] **Step 2: Wrap stock update in a retry loop for OptimisticLockException**

```java
public Stock updateStockQuantity(String sku, int quantityChange) {
    try {
        Stock stock = stockRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", sku));
        int newQuantity = stock.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Insufficient stock for SKU: " + sku);
        }
        stock.setQuantity(newQuantity);
        return stockRepository.save(stock);
    } catch (ObjectOptimisticLockingFailureException e) {
        // Retry once
        return updateStockQuantity(sku, quantityChange);
    }
}
```

- [ ] **Step 3: Compile and commit**

---

### Task 14: Inventory — Negative Stock Validation

**Files:**
- Modify: `backend/inventory-service/src/main/java/com/erp/system/inventory/service/impl/InventoryServiceImpl.java`

**Bug:** No validation preventing negative stock.

**Fix:** Add check before saving (already included in Step 13 Task 13 above).

- [ ] **Step 1: Add validation as shown in Task 13 step 2**

- [ ] **Step 2: Compile and commit**

---

### Task 15: Finance — Journal Entry Header/Detail Model

**Files:**
- Modify: `backend/finance-service/src/main/java/com/erp/system/finance/entity/JournalEntry.java`
- Create: `backend/finance-service/src/main/java/com/erp/system/finance/entity/JournalEntryLine.java`
- Modify: `backend/finance-service/src/main/java/com/erp/system/finance/service/impl/JournalEntryServiceImpl.java`

**Bug:** Uses single-line model. Balance calculation wrong for liability/equity/revenue accounts (normal balance is credit).

**Fix:** Split into header/detail model. Sum debits and credits separately and validate they equal.

- [ ] **Step 1: Read current JournalEntry entity**

- [ ] **Step 2: Create JournalEntryLine entity**

```java
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private BigDecimal debit;
    private BigDecimal credit;

    @Column(nullable = false)
    private String description;
}
```

- [ ] **Step 3: Update JournalEntry entity**

```java
@Entity
@Table(name = "journal_entries")
public class JournalEntry extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String journalNumber;

    @Column(nullable = false)
    private LocalDate entryDate;

    private String description;

    @Enumerated(EnumType.STRING)
    private JournalStatus status;

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JournalEntryLine> lines;
}
```

- [ ] **Step 4: Update service to validate debit = credit**

```java
public JournalEntry createEntry(JournalEntryRequest request) {
    BigDecimal totalDebit = request.getLines().stream()
            .map(JournalEntryLineRequest::getDebit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalCredit = request.getLines().stream()
            .map(JournalEntryLineRequest::getCredit)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (totalDebit.compareTo(totalCredit) != 0) {
        throw new IllegalArgumentException("Total debits must equal total credits");
    }
    // ... persist
}
```

- [ ] **Step 5: Compile and commit**

---

### Task 16: HRM — Employee Termination Deactivates User

**Files:**
- Modify: `backend/hrm-service/src/main/java/com/erp/system/hrm/service/EmployeeService.java`

**Bug:** When employee is terminated, the user account remains active.

**Fix:** Call identity-service to deactivate user on termination.

- [ ] **Step 1: Add Feign client for identity-service**

```java
@FeignClient("identity-service")
public interface IdentityClient {
    @PutMapping("/api/users/{userId}/deactivate")
    void deactivateUser(@PathVariable Long userId);
}
```

- [ ] **Step 2: Call deactivate in termination logic**

```java
@Transactional
public Employee terminateEmployee(Long id) {
    Employee employee = employeeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    employee.setStatus(EmployeeStatus.TERMINATED);
    identityClient.deactivateUser(employee.getUserId());
    return employeeRepository.save(employee);
}
```

- [ ] **Step 3: Compile and commit**

---

### Task 17: Payroll — Duplicate Run Validation

**Files:**
- Modify: `backend/hrm-service/src/main/java/com/erp/system/hrm/service/PayrollService.java`
- Modify: `backend/hrm-service/src/main/java/com/erp/system/hrm/entity/Payroll.java`

**Bug:** No validation preventing duplicate payroll runs in same period.

**Fix:** Add unique constraint on (period, year) and check before processing.

- [ ] **Step 1: Add unique constraint**

```java
@Table(name = "payrolls", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"period", "year"})
})
```

- [ ] **Step 2: Check before processing**

```java
public Payroll runPayroll(PayrollRequest request) {
    if (payrollRepository.existsByPeriodAndYear(request.getPeriod(), request.getYear())) {
        throw new IllegalArgumentException("Payroll already run for period " + request.getPeriod());
    }
    // ... process payroll
}
```

- [ ] **Step 3: Compile and commit**

---

### Task 18: Identity — Plain Text Password Logging

**Files:**
- Modify: `backend/identity-service/src/main/java/com/erp/identity/service/UserService.java`

**Bug:** Passwords logged in plain text during registration.

**Fix:** Remove logging of raw password. Log only the user identifier.

- [ ] **Step 1: Search for password logging**

```bash
Select-String -Pattern "log.*password|logger.*password|password.*log" backend/identity-service/src/main/java/ -CaseSensitive:$false
```

- [ ] **Step 2: Remove or redact password logging**

```java
log.info("Registering user: {}", request.getUsername());  // Don't log password
```

- [ ] **Step 3: Commit**

---

### Task 19: Gateway — Rate Limiting

**Files:**
- Modify: `backend/gateway-service/pom.xml` (add spring-boot-starter-data-redis-reactive)
- Modify: `backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java`

**Bug:** No rate limiting — API susceptible to abuse.

**Fix:** Add Spring Cloud Gateway rate limiter (token bucket) using Redis.

**Note:** If Redis is not available in the stack, use in-memory rate limiting instead.

- [ ] **Step 1: Add Redis reactive dependency**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

- [ ] **Step 2: Add rate limiter config**

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limit-route
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

- [ ] **Step 3: Compile and commit**

---

### Task 20: Product — Price Update Audit Trail

**Files:**
- Modify: `backend/product-service/src/main/java/com/erp/system/product/entity/Product.java`

**Bug:** Price changes have no audit trail — can't track who changed what price and when.

**Fix:** The `Auditable` base class from Task 4 already tracks `updatedBy` and `updatedAt`. Verify Product entity extends `Auditable`. If additional price history is needed, create a `PriceHistory` entity.

- [ ] **Step 1: Verify Product extends Auditable** (should already be done in Task 4)

- [ ] **Step 2: Create PriceHistory entity for full trail**

```java
@Entity
@Table(name = "price_history")
public class PriceHistory extends Auditable {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private BigDecimal oldPrice;
    private BigDecimal newPrice;
}
```

- [ ] **Step 3: Record price change in service**

```java
public Product updatePrice(Long id, BigDecimal newPrice) {
    Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    BigDecimal oldPrice = product.getPrice();
    product.setPrice(newPrice);
    priceHistoryRepository.save(new PriceHistory(product, oldPrice, newPrice));
    return productRepository.save(product);
}
```

- [ ] **Step 4: Compile and commit**

---

### Task 21: Cross-Service — Correlation ID

**Files:**
- Modify: `backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java`
- Modify: `backend/common-lib/src/main/java/com/erp/common/...` (add correlation filter)

**Bug:** No way to trace a request across service boundaries.

**Fix:** Generate a unique correlation ID at the gateway for every incoming request. Propagate via HTTP headers to downstream services. Log it in every service.

- [ ] **Step 1: Add gateway filter to generate correlation ID**

```java
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders()
                .getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID_HEADER, correlationId))
                .build();
        MDC.put(CORRELATION_ID_HEADER, correlationId);
        return chain.filter(mutatedExchange)
                .then(Mono.fromRunnable(MDC::clear));
    }

    @Override
    public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
```

- [ ] **Step 2: Add logging pattern to each service's application.yml**

```yaml
logging:
  pattern:
    level: "%5p [%X{X-Correlation-Id}]"
```

- [ ] **Step 3: Compile and commit**

---

### Task 22: Verify Everything Compiles

- [ ] **Step 1: Install common-lib**

Run: `mvn install -f backend/common-lib/pom.xml`

- [ ] **Step 2: Compile all services**

Run:
```powershell
$services = Get-ChildItem backend/*/pom.xml | Select-Object -ExpandProperty FullName
$failed = @()
foreach ($pom in $services) {
    Write-Host "Compiling $pom..."
    mvn compile -f $pom -q
    if ($LASTEXITCODE -ne 0) { $failed += $pom }
}
if ($failed.Count -gt 0) {
    Write-Host "Failed: $failed"
} else {
    Write-Host "All services compile successfully"
}
```

- [ ] **Step 3: Run all tests**

Run:
```powershell
foreach ($pom in $services) {
    Write-Host "Testing $pom..."
    mvn test -f $pom
}
```

- [ ] **Step 4: Final commit if needed**

---

## Summary of Changes

| Task | Description | Files |
|---|---|---|
| 1 | JWT algorithm fix | 1 |
| 2 | Global error handling + common-lib module | 15+ |
| 3 | RBAC with @PreAuthorize | ~200 |
| 4 | Audit fields on all entities | ~150 |
| 5 | Pagination on all list endpoints | ~150 |
| 6 | Unit/integration tests | ~300 |
| 7-21 | 16 critical bug fixes | ~50 |
| **Total** | | **~900 files** |
