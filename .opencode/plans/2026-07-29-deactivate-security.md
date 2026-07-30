# Deactivate All Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove all Spring Security authentication/authorization from every microservice so the project runs without username/password.

**Architecture:** Delete the 3 custom `SecurityConfig` files (common-lib, gateway, identity) which configure JWT-based auth, then exclude Spring Security auto-configuration in each service's `@SpringBootApplication` annotation to prevent default HTTP Basic auth from kicking in.

**Tech Stack:** Spring Boot 3.2.2, Spring Security 6, Maven

## Global Constraints

- No security filter chains should be active in any service
- All HTTP endpoints must be accessible without any authentication
- The approach must be easily reversible (restore deleted files from git, remove exclude entries)
- Do NOT remove `spring-boot-starter-security` from any `pom.xml` — just disable it via exclusions
- All services must still compile and start after changes

---

### Task 1: Delete the 3 Security Configuration Files

**Files:**
- Delete: `backend/common-lib/src/main/java/com/erp/common/config/ResourceServerConfig.java`
- Delete: `backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java`
- Delete: `backend/identity-service/src/main/java/com/erp/identity/config/SecurityConfig.java`

**Interfaces:**
- Consumes: None
- Produces: No custom SecurityFilterChain beans exist anymore

- [ ] **Step 1: Delete ResourceServerConfig.java (common-lib)**

```bash
Remove-Item -LiteralPath "backend/common-lib/src/main/java/com/erp/common/config/ResourceServerConfig.java"
```

This removes the default JWT-based security config shared by all servlet services (payment, order, inventory, etc.).

- [ ] **Step 2: Delete gateway SecurityConfig.java**

```bash
Remove-Item -LiteralPath "backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java"
```

This removes the reactive security config for the API gateway.

- [ ] **Step 3: Delete identity SecurityConfig.java**

```bash
Remove-Item -LiteralPath "backend/identity-service/src/main/java/com/erp/identity/config/SecurityConfig.java"
```

This removes the identity service's security config (already permitted all, but still configured JWT decoding).

---

### Task 2: Add Security Auto-Config Exclusions to All Servlet Services (9 services)

**Files:**
- Modify: `backend/identity-service/src/main/java/com/erp/identity/IdentityApplication.java`
- Modify: `backend/payment-service/src/main/java/com/erp/system/payment/PaymentServiceApplication.java`
- Modify: `backend/order-service/src/main/java/com/erp/system/order/OrderServiceApplication.java`
- Modify: `backend/inventory-service/src/main/java/com/erp/system/inventory/InventoryApplication.java`
- Modify: `backend/procurement-service/src/main/java/com/erp/system/procurement/ProcureApplication.java`
- Modify: `backend/hrm-service/src/main/java/com/erp/system/hrm/HrmApplication.java`
- Modify: `backend/finance-service/src/main/java/com/erp/system/finance/FinanceApplication.java`
- Modify: `backend/product-service/src/main/java/com/erp/system/product/ProductApplication.java`
- Modify: `backend/sales-service/src/main/java/com/erp/system/sales/SalesApplication.java`

All 9 services above currently have the same annotation:
```java
@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})
```

Must change to:
```java
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = {"com.erp", "com.erp.system"},
    exclude = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
    })
```

**Interfaces:**
- Consumes: No SecurityConfig beans exist after Task 1
- Produces: No auto-configured security beans either

- [ ] **Step 1: Update IdentityApplication.java**

Edit `backend/identity-service/src/main/java/com/erp/identity/IdentityApplication.java` — replace the import-less `@SpringBootApplication(scanBasePackages = {"com.erp", "com.erp.system"})` with the version above including all security exclusions and required imports.

- [ ] **Step 2: Update PaymentServiceApplication.java**

Same edit on `backend/payment-service/src/main/java/com/erp/system/payment/PaymentServiceApplication.java`.

- [ ] **Step 3: Update OrderServiceApplication.java**

Same edit on `backend/order-service/src/main/java/com/erp/system/order/OrderServiceApplication.java`.

- [ ] **Step 4: Update InventoryApplication.java**

Same edit on `backend/inventory-service/src/main/java/com/erp/system/inventory/InventoryApplication.java`.

- [ ] **Step 5: Update ProcureApplication.java**

Same edit on `backend/procurement-service/src/main/java/com/erp/system/procurement/ProcureApplication.java`.

- [ ] **Step 6: Update HrmApplication.java**

Same edit on `backend/hrm-service/src/main/java/com/erp/system/hrm/HrmApplication.java`.

- [ ] **Step 7: Update FinanceApplication.java**

Same edit on `backend/finance-service/src/main/java/com/erp/system/finance/FinanceApplication.java`.

- [ ] **Step 8: Update ProductApplication.java**

Same edit on `backend/product-service/src/main/java/com/erp/system/product/ProductApplication.java`.

- [ ] **Step 9: Update SalesApplication.java**

Same edit on `backend/sales-service/src/main/java/com/erp/system/sales/SalesApplication.java`.

---

### Task 3: Add Security Exclusions to Reporting Service (already has other exclusions)

**Files:**
- Modify: `backend/reporting-service/src/main/java/com/erp/reporting/ReportingApplication.java`

**Interfaces:**
- Consumes: No SecurityConfig beans exist after Task 1
- Produces: No auto-configured security beans

**Current annotation:**
```java
@SpringBootApplication(
    scanBasePackages = {"com.erp", "com.erp.system"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    })
```

**Must change to:**
```java
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;

@SpringBootApplication(
    scanBasePackages = {"com.erp", "com.erp.system"},
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
    })
```

- [ ] **Step 1: Update ReportingApplication.java** with the new `@SpringBootApplication` annotation

---

### Task 4: Verify Compilation

**Files:** None (verification only)

- [ ] **Step 1: Run Maven compile on the full project**

```bash
mvn.cmd clean compile -q
```

- [ ] **Step 2: Verify no compilation errors**

If there are compilation errors, they will be from missing imports in the modified application classes. Fix any missing imports.

**To re-enable security later:**
```bash
git restore backend/common-lib/src/main/java/com/erp/common/config/ResourceServerConfig.java
git restore backend/gateway-service/src/main/java/com/erp/gateway/config/SecurityConfig.java
git restore backend/identity-service/src/main/java/com/erp/identity/config/SecurityConfig.java
```
Then manually remove the `exclude` entries from each Application.java.
