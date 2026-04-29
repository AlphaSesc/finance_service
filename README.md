# Finance Service

The **Finance Service** handles all financial operations for the University Study Circle (USC) system: student finance accounts, invoice generation, payment processing, and outstanding balance checks.

---

## Overview

This service is responsible for:

- **Finance account creation** — one account per student (created by Student Portal during first enrollment)
- **Invoice generation** — supports two invoice types:
    - `COURSE_ENROLLMENT` — created by Student Portal when a student enrolls
    - `LIBRARY_FINE` — created by Library Service when a student returns a book late
- **Payment processing** — students pay invoices using the unique invoice reference
- **Outstanding balance checks** — used by Student Portal for graduation eligibility verification
- **Invoice history** — all invoices for a given student, sorted by most recent first

This service is **internal** — it has no authentication and is intended to be called only by other microservices in the USC system.

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.4 |
| Database | MySQL 8.x |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| Authentication | None (internal service) |

---

## Architecture

```
┌────────────────────┐         ┌────────────────────┐
│  Student Portal    │         │  Library Service   │
│   (Port 8081)      │         │    (Port 8082)     │
└────────────────────┘         └────────────────────┘
          │                              │
          │  Create account              │  Late return
          │  Create invoice              │  fine invoice
          │  Pay invoice                 │
          │  Check balance               │
          ▼                              ▼
┌──────────────────────────────────────────────────┐
│             Finance Service                      │
│           (Port 8080 — main API)                 │
└──────────────────────────────────────────────────┘
```

The Finance Service is the **source of truth** for all financial data. Both Student Portal and Library Service call it via REST.

---

## API Endpoints

All endpoints are **public** (no authentication required) since this service is intended for internal microservice use only.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/accounts` | Create a finance account for a student |
| `POST` | `/api/invoices` | Create an invoice (course enrollment or library fine) |
| `GET` | `/api/invoices/outstanding/{studentId}` | Check if a student has unpaid invoices |
| `PUT` | `/api/invoices/pay` | Pay an invoice using `studentId` + reference |
| `GET` | `/api/invoices/student/{studentId}` | Get all invoices for a student (sorted, newest first) |

### Example Requests

**Create finance account:**
```json
POST /api/accounts
{
  "studentId": "STU-001",
  "email": "student@university.edu"
}
```

**Create course enrollment invoice:**
```json
POST /api/invoices
{
  "studentId": "STU-001",
  "courseCode": "CS101",
  "amount": 1500.00,
  "invoiceType": "COURSE_ENROLLMENT"
}
```

**Create library fine invoice:**
```json
POST /api/invoices
{
  "studentId": "STU-001",
  "amount": 500.00,
  "invoiceType": "LIBRARY_FINE"
}
```

**Pay invoice:**
```json
PUT /api/invoices/pay
{
  "studentId": "STU-001",
  "reference": "INV-A1B2C3D4"
}
```

---

## Configuration

Configuration is split between two files in `src/main/resources/`:

### `application.yaml`
```yaml
server:
  port: 8080

spring:
  application:
    name: finance_service
  profiles:
    active: dev
```

### `application-dev.yaml`
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/finance_service_db
    username: root
    password: <your-password>
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
```

> **Note:** Update `username` and `password` to match your local MySQL setup.

---

## Prerequisites

1. **Java 21** ([Adoptium](https://adoptium.net/))
2. **Maven** (or use included `mvnw` wrapper)
3. **MySQL Server** running on `localhost:3306`
4. **Database created**:
   ```sql
   CREATE DATABASE finance_service_db;
   ```

---

## Running the Service

### Using Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Using Maven
```bash
mvn spring-boot:run
```

### From IntelliJ IDEA
- Open the project as a Maven project
- Run `FinanceServiceApplication.java` directly (right-click → **Run**)

The service will start on **http://localhost:8080**.

---

## Project Structure

```
src/main/java/com/example/finance_service/
├── controller/          # REST controller (FinanceController)
├── dto/                 # Request/response DTOs
├── entity/              # JPA entities (FinanceAccount, Invoice)
├── exception/           # Custom exceptions + global handler
├── repository/          # JPA repositories
├── service/             # Business logic (FinanceService)
└── util/                # Helpers (InvoiceReferenceGenerator)
```

---

## Business Rules

### Account Creation
- One finance account per student (`studentId` is unique)
- Email is denormalized from the Student Portal for record-keeping

### Invoice Creation
- Student must have a finance account before any invoice can be created
- Each invoice gets a unique reference like `INV-A1B2C3D4`
- New invoices start with status `PENDING`

#### Invoice Type Validation
- `COURSE_ENROLLMENT` — `courseCode` is **required**
- `LIBRARY_FINE` — `courseCode` must **not** be provided

### Payment
- Lookup uses **both** `studentId` and `reference` to prevent students from paying others' invoices
- An invoice can only be paid once (status changes from `PENDING` → `PAID`)
- Attempting to pay an already-paid invoice returns an error

### Outstanding Balance Check
- Returns `true` if **any** invoice for the student is still in `PENDING` status
- Used by Student Portal to determine graduation eligibility

---

## Invoice Reference Format

Invoice references are auto-generated using `InvoiceReferenceGenerator`:

```
INV-XXXXXXXX
```

where `XXXXXXXX` is the first 8 characters of a UUID (uppercased). Example: `INV-A1B2C3D4`.

---

## Cross-Service Communication

The Finance Service is a **leaf service** — it does not call any other service. It only **receives** calls from:

- **Student Portal** — for account creation, course enrollment invoices, payment, balance checks, and history
- **Library Service** — for late-return fine invoices

If you stop or restart the Finance Service, both Student Portal and Library Service will fail their cross-service operations until it's back up.

---

## Note on Security

This service intentionally has **no authentication**. It assumes:
- It runs in a trusted internal network (not exposed to the public internet)
- Other services (Student Portal, Library Service) handle authentication on their end
- In a production deployment, this service should be placed behind an API gateway or service mesh that enforces internal-only access (e.g., mTLS, VPC-internal networking)

---
