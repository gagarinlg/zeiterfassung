# Changelog

All notable changes to the Zeiterfassung project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Planned
- Phase 4: Vacation Management
- Phase 5: Dashboard & Reporting

---

## [Phase 3] - 2026-02-21

### Added
- TimeTrackingService with clock-in/out, break management, conflict detection
- TimeTrackingController with all REST endpoints and @PreAuthorize
- ArbZGComplianceService (max 10h/day, mandatory breaks, 11h rest)
- Daily summary auto-calculation
- Time sheet views (monthly and date-range)
- Real-time tracking status endpoint
- Manager endpoints for team management
- Employee configuration CRUD
- All time tracking DTOs

---

## [Phase 2] - 2026-02-21

### Added
- JPA entities: UserEntity, RoleEntity, PermissionEntity, RefreshTokenEntity, AuditLogEntity
- JWT-based authentication (HS256, 15min access, 7day refresh tokens)
- Refresh token rotation with reuse detection
- Account lockout (5 attempts → 15min lock)
- Full user CRUD with soft delete
- Role assignment, RFID management, password change/reset
- AuditService with @Async logging
- DataSeeder for initial admin user
- Frontend auth: apiClient with refresh queue, AuthContext, LoginPage, ProtectedRoute
- i18n strings for auth/users in German and English
- RFC 7807 error handling with custom exceptions

### Fixed
- Terminal Rust files formatted with cargo fmt (CI was blocking)

---

## [Phase 1] - 2026-02-21

### Added
- Spring Boot 3.2 / Kotlin backend with full package structure
- 5 Flyway database migrations with seeded data
- React 18 + TypeScript + Vite + TailwindCSS frontend
- Rust terminal application for Raspberry Pi
- Android (Kotlin/Compose) mobile app scaffold
- iOS (Swift/SwiftUI) mobile app scaffold
- Docker multi-stage build + docker-compose with PostgreSQL 16
- Nginx configuration with HTTPS, HSTS, CSP headers
- GitHub Actions: CI, security, build-deploy, docs workflows
- Dependabot + Renovate for dependency management
- Project documentation (setup, architecture, adding-languages, quick-start)
- ArbZG compliance constants and seeded system defaults
- German federal public holidays seeded
