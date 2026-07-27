# User & Access Management

**Service:** identity-service (port 8086)  
**Database:** `erp_db`  
**Entity:** User  
**Roles:** ADMIN, STAFF

---

## 1. Business Procedures

### 1.1 Login

**Path:** `/login`

1. Open the ERP application in a browser
2. Enter your **username** and **password**
3. Click **Sign in**
4. System validates credentials, returns a JWT token (stored in localStorage)
5. Browser redirects to the dashboard

**Default accounts:**

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN — full access |
| `staff` | `staff123` | STAFF — limited access |

### 1.2 Role-Based Access Control

| Role | Permissions |
|---|---|
| **ADMIN** | All modules — create, read, update, delete products, orders, employees, accounts, users |
| **STAFF** | Restricted — typically view-only or limited operations |

### 1.3 Logout

- Click **Logout** in the sidebar footer
- JWT token is cleared from localStorage
- Browser redirects to the login page

---

## 2. API Reference

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Authenticate, returns JWT |

---

## 3. Audit Trail

### 3.1 Traceable Events

| Event | Evidence | How to Verify |
|---|---|---|
| User registration | `created_at` timestamp | Query `users` table by username |
| User login | JWT `iat` (issued-at) claim | Decode token at jwt.io |
| Password change | ❌ Not logged | Password hash is overwritten |

### 3.2 Audit Queries

```sql
-- List all users with creation timestamps
SELECT id, username, email, role, created_at
FROM erp_db.public.users
ORDER BY created_at DESC;

-- Find when a specific user was created
SELECT id, username, email, role, created_at
FROM erp_db.public.users
WHERE username = 'admin';
```

### 3.3 JWT Decoding (Login Evidence)

Every JWT contains:

```json
{
  "sub": "admin",
  "role": "ADMIN",
  "iat": 1720000000,
  "exp": 1720086400
}
```

- `iat` = login timestamp (Unix epoch)
- `exp` = token expiry (24 hours later)
- Decode at `https://jwt.io` to verify who logged in and when

### 3.4 Audit Gaps

| Gap | Risk | Fix |
|---|---|---|
| No `last_login` field | Cannot track account usage | Add `last_login` timestamp to `users` table |
| No failed login counter | Cannot detect brute force attacks | Add `login_attempts` and `locked_until` fields |
| No password change history | Password resets invisible | Create `password_history` table |
| No `created_by` on user | Cannot tell which admin created a user | Add `created_by` foreign key |

---

## 4. Database Schema

```sql
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    full_name   VARCHAR(100),
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);
```

---

*Document version 1.0 — July 2026*
