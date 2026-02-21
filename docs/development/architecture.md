# System Architecture

## Overview

Zeiterfassung is a multi-component time tracking system consisting of:

1. **Web Application** — React SPA + Spring Boot REST API + PostgreSQL
2. **Mobile Apps** — Native Android (Kotlin/Compose) and iOS (Swift/SwiftUI)
3. **Terminal** — Raspberry Pi 5 kiosk app (Rust) for NFC/RFID clock-in/out

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                      │
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐│
│  │  React Frontend │  │  Android / iOS  │  │  Rust Terminal   ││
│  │  (TypeScript)   │  │  Mobile Apps    │  │  (Raspberry Pi)  ││
│  └────────┬────────┘  └────────┬────────┘  └────────┬─────────┘│
└───────────┼─────────────────────────────────────────┼──────────┘
            │               HTTPS REST API            │
            ▼                                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                         Application Layer                       │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │               Spring Boot 3 + Kotlin Backend            │   │
│  │                                                          │   │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────┐ │   │
│  │  │ Security │  │Controller│  │ Service  │  │  Repo  │ │   │
│  │  │JWT+RBAC  │→ │REST API  │→ │Business  │→ │  JPA   │ │   │
│  │  └──────────┘  └──────────┘  │Logic     │  └────┬───┘ │   │
│  │                               │ArbZG     │       │     │   │
│  │                               └──────────┘       │     │   │
│  └──────────────────────────────────────────────────┼─────┘   │
└────────────────────────────────────────────────────┬┼──────────┘
                                                     ││
                                                     ▼│
                                          ┌──────────────────┐
                                          │   PostgreSQL 16  │
                                          │  Flyway Migrate  │
                                          └──────────────────┘
```

## Backend Package Structure

```
com.zeiterfassung/
├── config/          # Spring Security, CORS, JWT, OpenAPI configs
├── controller/      # REST controllers (@RestController)
├── service/         # Business logic (@Service)
├── repository/      # JPA repositories (@Repository)
├── model/
│   ├── entity/      # JPA entities (@Entity)
│   ├── dto/         # Request/Response DTOs
│   └── enums/       # Domain enums (Role, Permission, etc.)
├── security/        # JWT filter, auth provider, UserDetailsService
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── util/            # German labor law calculations
└── audit/           # Audit trail (@EntityListener)
```

## Database Schema

See Flyway migrations in `backend/src/main/resources/db/migration/`:

| Migration | Tables |
|-----------|--------|
| V1 | users, roles, permissions, user_roles, role_permissions |
| V2 | time_entries, daily_summaries |
| V3 | vacation_requests, vacation_balances, public_holidays |
| V4 | employee_configs |
| V5 | audit_log, system_settings, refresh_tokens |

## Security Architecture

```
Request → JWT Filter → SecurityContext → Controller → Service → Repository
              ↓                              ↓
         Validate JWT                Check Permissions
         Load UserDetails             (Method Security)
```

**Layers of security:**
1. **Frontend:** Hide UI elements based on permissions (UX only)
2. **JWT Filter:** Validate token on every request
3. **Method Security:** `@PreAuthorize` on service methods
4. **Business Logic:** Permission checks in service layer

## API Design

- Base URL: `/api`
- Authentication: `Bearer <jwt_token>` header
- Error format: RFC 7807 Problem Details
- Documentation: OpenAPI 3.0 at `/api/swagger-ui.html`

## Key API Endpoints (Planned)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/login | Authenticate user |
| POST | /api/auth/refresh | Refresh access token |
| POST | /api/auth/logout | Revoke refresh token |
| GET | /api/users | List users (admin) |
| GET | /api/time-entries | Get time entries |
| POST | /api/time-entries/clock-in | Clock in |
| POST | /api/time-entries/clock-out | Clock out |
| GET | /api/vacation-requests | Get vacation requests |
| POST | /api/vacation-requests | Create vacation request |
| POST | /api/terminal/clock | Terminal clock in/out via RFID |
