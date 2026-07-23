# Project Specification: Enterprise Resource Planning (ERP) System

## 📍 CURRENT STATUS
- **Phase:** All backend services built and running (local & Docker)
- **Authentication:** JWT login working via gateway (`admin / admin123`, `staff / staff123`)
- **Frontend:** React app connects to gateway at `localhost:8092`; login works
- **Infrastructure:** 11 microservices + PostgreSQL, Docker-ready
- **Known Gaps:** No HTTPS, no DB migrations (ddl-auto=update), no monitoring, no tests

---

## 🚀 PROJECT ROADMAP
### Phase 1: Infrastructure & Auth (COMPLETED ✅)
- ✅ API Gateway (Spring Cloud Gateway on port 8092)
- ✅ Service Discovery (Eureka Server on port 8761)
- ✅ Identity Service (register, login, JWT, RBAC roles: ADMIN/STAFF)
- ✅ Security Config with OAuth2 Resource Server (JWT) on gateway
- ✅ Default users seeded on first run
- ✅ All 11 services containerized with Dockerfiles
- ✅ Docker Compose orchestration with `erp-network`
- ✅ Env vars extracted to `.env` for DB credentials

### Phase 2: Inventory Core (COMPLETED ✅)
- **Product Service** (port 8083)
  - Product CRUD APIs
  - Database: `product_db`
- **Inventory Service** (port 8091)
  - Stock management
  - Database: `inventory_db`

### Phase 3: Sales & Procurement (COMPLETED ✅)
- **Order Service** (port 8093) — Order processing with Feign clients
- **Payment Service** (port 8094) — Payment processing
- **Sales Service** (port 8095) — Customers & invoices
- **Procurement Service** (port 8096) — Vendors, purchase orders, goods received
- **Integration:** Sales linked to inventory/stock via Eureka + Feign
- Datasets: `order_db`, `payment_db`, `sales_db`, `procurement_db`

### Phase 4: HR & Finance (COMPLETED ✅)
- **HRM Service** (port 8097) — Employees, attendance, leaves
  - Database: `hrm_db`
- **Finance Service** (port 8098) — Accounts, journal entries, payroll
  - Database: `finance_db`

### Phase 5: Intelligence & Polish (NOT STARTED ❌)
- ❌ Reporting / Analytics dashboard
- ❌ Notifications (email/SMS via RabbitMQ/Kafka)
- ❌ Final QA & hardening

---

## 🛠 Tech Stack
- **Frontend:** React 19, TypeScript, Tailwind CSS 4, TanStack Query, React Router 7, Vite 8
- **Backend:** Java 21, Spring Boot 3.2.2, Spring Cloud 2023.0.0
- **API Gateway:** Spring Cloud Gateway (reactive, Netty)
- **Service Discovery:** Netflix Eureka Server
- **Database:** PostgreSQL 17 (single instance, multiple databases)
- **Security:** Spring Security + OAuth2 Resource Server, JWT (HMAC-SHA384), RBAC
- **Messaging:** Not yet implemented
- **Containerization:** Docker, Docker Compose (multi-stage builds)
- **Build:** Maven (no parent POM — each service builds independently)

## 📂 Folder Structure (Monorepo)
```
/backend
  /common-lib           # Shared DTOs, exceptions (not yet a Maven module)
  /eureka-server        # Port 8761
  /gateway-service      # Port 8092
  /identity-service     # Port 8086 (auth, users)
  /product-service      # Port 8083
  /inventory-service    # Port 8091
  /order-service        # Port 8093
  /payment-service      # Port 8094
  /sales-service        # Port 8095
  /procurement-service  # Port 8096
  /hrm-service          # Port 8097
  /finance-service      # Port 8098
  (each has its own Dockerfile)
/frontend
  /src/features         # Module-specific (products, orders, inventory, etc.)
  /src/lib              # Axios instance pointing to localhost:8092
  /src/hooks            # Custom React hooks
```

## 🐳 Running Modes

### Docker (all containers)
```powershell
docker compose up -d
# Gateway: http://localhost:8092
# Eureka:  http://localhost:8761
```

### Local Maven (via run-all.ps1)
```powershell
.\run-all.ps1
# Requires PostgreSQL running (Docker or local install)
```

### Frontend
```powershell
cd frontend && npm run dev
# Opens at http://localhost:5173
```

## 🔑 Default Credentials
| Username | Password | Role  |
|----------|----------|-------|
| `admin`  | `admin123` | ADMIN |
| `staff`  | `staff123` | STAFF |

## ⚠️ Production Gaps
- **Secrets:** JWT secret and DB passwords in config files (not in Vault/env)
- **HTTPS:** No TLS termination (gateway or services)
- **DB Migrations:** Using `ddl-auto=update` (use Flyway/Liquibase for prod)
- **Monitoring:** No metrics, tracing, or centralized logging
- **Circuit Breakers:** Feign clients have no fallback configurations
- **Resilience:** No retry/timeout policies on inter-service calls
- **Caching:** Redis listed in tech stack but not implemented
- **Tests:** No unit/integration tests visible
- **Config Server:** Spring Cloud Config not implemented (configs per-service)
- **Dockerfiles:** Fat JAR approach (no layered caching; slow rebuilds)
