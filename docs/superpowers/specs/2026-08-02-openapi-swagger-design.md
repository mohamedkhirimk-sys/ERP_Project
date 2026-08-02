# OpenAPI / Swagger UI — Design

**Date:** 2026-08-02
**Status:** Implemented and committed (009ca2a, bb134d6)

## Context

The ERP is a microservices platform: 10 web services (identity, inventory, order, payment,
product, sales, hrm, finance, procurement, reporting), one Spring Cloud Gateway (8082), one
Eureka server, and a Vue frontend. There was no API documentation tooling. The user asked to
"introduce Swagger to the project".

## Design

### Library and versions

- `springdoc-openapi-starter-webmvc-ui` **2.3.0** on each of the 10 services (Spring Boot 3.2.2
  compatible line; 3.x of springdoc targets newer Spring Boot).
- `springdoc-openapi-starter-webflux-ui` **2.3.0** on the gateway (reactive).
- `org.webjars:webjars-locator-core` (version managed by Boot: 0.55) on the gateway — required
  so the versionless webjar path `/webjars/swagger-ui/index.html` (the redirect target of
  `/swagger-ui.html`) resolves to `swagger-ui-5.10.3`.

### Per-service OpenAPI endpoints

Every service exposes its OpenAPI JSON at `/v3/api-docs` and its UI at `/swagger-ui.html`
(redirects to `/webjars/swagger-ui/index.html`).

### Gateway aggregation (single entry point)

The gateway is the single public endpoint. Two pieces:

1. **Routes** (`RateLimitingConfig.customRoutes`): for each service, a `{service}-docs-route`
   mapping `/{service}-service/v3/api-docs` → rewrite path to `/v3/api-docs` → `lb://{service}-service`.
   E.g. `/sales-service/v3/api-docs` → `lb://sales-service/v3/api-docs`.
2. **Swagger UI config** (`application.yml`):
   ```yaml
   springdoc:
     swagger-ui:
       path: /swagger-ui.html
       urls:
         - url: /identity-service/v3/api-docs
           name: identity-service
         # ... 10 services total
   ```
   This gives one Swagger UI at `http://localhost:8082/swagger-ui.html` with a dropdown to
   switch between all 10 service specs.

### 404 fix (done alongside, commit bf98836)

While testing `/swagger-ui.html` on the gateway, unknown paths returned HTTP 500:

- Services: `GlobalExceptionHandler` (common-lib) catch-all `@ExceptionHandler(Exception.class)`
  turned Spring's `NoResourceFoundException` (404 for unknown paths) into 500. Fixed with a
  dedicated `@ExceptionHandler(NoResourceFoundException.class)` returning 404.
- Gateway: `@ComponentScan` of package `com.erp` picked up the servlet-based
  `GlobalExceptionHandler` in the reactive context, failing at request time with
  `IllegalStateException: Could not resolve parameter [1] ... No suitable resolver` → 500.
  Fixed by narrowing `scanBasePackages` to `{"com.erp.gateway", "com.erp.system"}`.

## Scope

- 10 service poms + gateway pom: springdoc dependency
- `RateLimitingConfig.java`: 10 docs routes
- `application.yml` (gateway): swagger-ui path + 10 urls
- `GatewayApplication.java`: scanBasePackages narrowed
- common-lib `GlobalExceptionHandler`: NoResourceFoundException → 404 (+ test)

## Verified (E2E, all through gateway 8082)

- `/swagger-ui.html` → 302 → `/webjars/swagger-ui/index.html` → 200 (Swagger UI HTML)
- `/v3/api-docs/swagger-config` lists all 10 services with `{service}-service/v3/api-docs` urls
- `/x-service/v3/api-docs` → 200 with valid OpenAPI JSON for all 10 services
- Unknown path → 404 (was 500)
- Existing routes unaffected (e.g. `/api/invoices/9/pdf` → 200)

## Out of scope

- Auth on swagger endpoints, custom OpenAPI info/tags/descriptions, splitting specs per controller
