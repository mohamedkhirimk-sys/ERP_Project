# Project Specification: Enterprise Resource Planning (ERP) System

## 📍 CURRENT STATUS (AI Context)
- **Current Phase:** Phase 1 (Infrastructure & Auth)
- **Current Week:** Week 2 (Identity Service)
- **Active Task:** [Update this manually as you progress, e.g., "Database Schema Design"]
- **Next Milestone:** Complete JWT Authentication and RBAC.

---

## 🚀 PROJECT ROADMAP
### Phase 1: Infrastructure & Auth (The Backbone)
- **Week 1: Core Infrastructure** (COMPLETED ✅)
  - API Gateway, Eureka Server, Config Server setup.
- **Week 2: Identity Service** (CURRENT 🎯)
  - User Registration, Login, JWT Generation, RBAC (Roles).
- **Week 3: Security Integration**
  - Implementing Global Security Filters and Interceptors across all services.

### Phase 2: Inventory Core (The Foundation)
- **Week 4: Product & Catalog Management**
  - Database schema for Products, Categories, SKUs.
  - CRUD APIs for inventory management.
- **Week 5: Warehouse & Stock Logic**
  - Multi-warehouse support, stock movement tracking (In/Out).

### Phase 3: Sales & Procurement (The Flow)
- **Week 6: Procurement Service**
  - Vendor Management, Purchase Orders (PO), Goods Received Notes (GRN).
- **Week 7: Sales & Order Management**
  - Customer profiles, Order processing, Invoicing.
- **Week 8: Integration Layer**
  - Linking Sales to Inventory (Stock deduction) and Finance (Revenue).

### Phase 4: HR & Finance (The Operations)
- **Week 9: HRM Service**
  - Employee records, Attendance, Leave Management.
- **Week 10: Payroll & Accounting**
  - Salary calculation, General Ledger, Accounts Payable/Receivable.

### Phase 5: Intelligence & Polish
- **Week 11: Reporting & Analytics**
  - Dashboard for Profit/Loss, Stock Trends, and HR metrics.
- **Week 12: Notifications & Final QA**
  - Email/SMS alerts via RabbitMQ/Kafka, final bug fixing.

---

## 🛠 Tech Stack
- **Frontend:** React.js, Tailwind CSS, TanStack Query (Data Fetching), Shadcn UI.
- **Backend:** Java 17+, Spring Boot 3.x, Spring Cloud (Gateway, Config Server, Eureka).
- **Database:** PostgreSQL (Primary RDBMS), Redis (Caching/Session).
- **Messaging:** RabbitMQ or Kafka (Asynchronous event-driven communication).
- **Security:** Spring Security with JWT & RBAC (Role-Based Access Control).

## 📂 Folder Structure (Monorepo Style)
- `/backend`
  - `/identity-service`
  - `/inventory-service`
  - `/sales-service`
  - `/procurement-service`
  - `/hrm-service`
  - `/finance-service`
  - `/gateway-service`
  - `/common-lib` (Shared DTOs, Exceptions, Utils)
- `/frontend`
  - `/src/api` (Axios instances & API definitions)
  - `/src/components` (Reusable UI components)
  - `/src/features` (Module-specific logic: /inventory, /sales, /hr)
  - `/src/hooks` (Custom React hooks)

## 📜 Coding Standards & Rules
- **Backend:** Use MapStruct for DTO mapping, Lombok for boilerplate reduction, and Global Exception Handling. Follow RESTful naming conventions.
- **Frontend:** Functional components only. Use "Feature-based" folder structure. State management via TanStack Query or Zustand.
- **Communication:** Synchronous (REST) for immediate actions; Asynchronous (RabbitMQ/Kafka) for side effects (e.g., updating inventory after a sale).
