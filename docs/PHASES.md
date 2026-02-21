# Zeiterfassung — Project Phases & Roadmap

> **Last updated:** 2026-02-21
> **Current phase:** Phase 5 (Dashboard & Reporting) — NEXT UP

---

## Phase 1: Project Foundation ✅
- **Status**: COMPLETE
- **PR**: #1
- **Merged**: 2026-02-21
- **Branch**: `copilot/setup-project-foundation`

### What was delivered
- Spring Boot 3.2 / Kotlin backend with full package skeleton
- 5 Flyway migrations (users, roles, permissions, time entries, vacation, audit, system settings)
- React 18 + TypeScript + Vite frontend with TailwindCSS and react-i18next
- Rust terminal app for Raspberry Pi with iced GUI, reqwest, rusqlite, rodio, fluent i18n
- Android (Kotlin/Compose, Hilt, Retrofit) and iOS (Swift/SwiftUI) mobile app scaffolding
- Docker + docker-compose setup (PostgreSQL 16, Nginx with HTTPS/HSTS/CSP)
- GitHub Actions CI (ktlint, JaCoCo, ESLint, Vitest, rustfmt, clippy, cargo test)
- Security workflow (CodeQL, dependency review, OWASP ZAP placeholder)
- Build/deploy workflow (Docker → GHCR, ARM64 cross-compile, GitHub Releases)
- Docs workflow (GitHub Pages)
- Dependabot + Renovate configuration
- Full documentation structure (setup, architecture, adding-languages, quick-start)

### Key files/directories created
- `backend/` — Spring Boot app with Kotlin, Gradle Kotlin DSL, Java 21
- `frontend/` — Vite + React + TypeScript + TailwindCSS
- `terminal/` — Rust app with iced, reqwest, rusqlite
- `mobile/android/` — Kotlin + Jetpack Compose
- `mobile/ios/` — Swift + SwiftUI
- `.github/workflows/` — ci.yml, security.yml, build-deploy.yml, docs.yml
- `docs/` — development setup, architecture, adding languages, quick start
- `Dockerfile`, `docker-compose.yml`, `docker/nginx.conf`

### Database migrations
- V1: users, roles, permissions, junction tables; seeded RBAC
- V2: time_entries, daily_summaries with ArbZG compliance fields
- V3: vacation_requests, vacation_balances, public_holidays (German federal)
- V4: employee_configs (JSONB work days, contract dates)
- V5: audit_log, system_settings, refresh_tokens; seeded ArbZG defaults

---

## Phase 2: Authentication, Authorization & User Management ✅
- **Status**: COMPLETE
- **PR**: #36
- **Merged**: 2026-02-21
- **Branch**: `copilot/build-auth-user-management-system`

### What was delivered
- **JPA Entities**: UserEntity, RoleEntity, PermissionEntity, RefreshTokenEntity, AuditLogEntity (H2/PostgreSQL compatible)
- **JWT Security**: JwtService (HS256, 15min access / 7day refresh), JwtAuthenticationFilter, CustomUserDetailsService, SecurityConfig (stateless, CORS, CSP, HSTS)
- **AuthService + AuthController** (`/api/auth`): login with 5-attempt lockout → 15min lock; refresh token rotation with reuse detection; logout/logout-all/me
- **UserService + UserController** (`/api/users`): full CRUD with soft delete, role assignment, RFID management, password change/reset — all endpoints with @PreAuthorize
- **AuditService**: @Async logging for login, logout, data changes, permission changes
- **DataSeeder**: seeds admin@zeiterfassung.local on first startup (password from SEED_ADMIN_PASSWORD env var)
- **Exceptions**: DuplicateResourceException (409), AccountLockedException (423), RateLimitExceededException (429); RFC 7807 throughout
- **Frontend Auth**: apiClient with 401→refresh→retry queue, AuthContext with auto-refresh, LoginPage with zod validation, ProtectedRoute with inline 403
- **i18n**: all new auth/user/rate-limit strings in de + en
- **CI Fix**: cargo fmt applied to terminal files

### Key API endpoints implemented
- POST /api/auth/login, /refresh, /logout, /logout-all
- GET /api/auth/me
- CRUD /api/users (POST, GET paginated, GET/{id}, PUT/{id}, DELETE/{id})
- PUT /api/users/{id}/roles, /rfid, /password, /password/reset
- GET /api/users/{id}/team

### Backend additions (57 files changed, +10,650 lines)
- `backend/src/main/kotlin/com/zeiterfassung/model/entity/` — 5 JPA entities
- `backend/src/main/kotlin/com/zeiterfassung/repository/` — 5 repositories
- `backend/src/main/kotlin/com/zeiterfassung/security/` — JwtService, JwtAuthenticationFilter, CustomUserDetailsService, SecurityConfig
- `backend/src/main/kotlin/com/zeiterfassung/service/` — AuthService, UserService, AuditService
- `backend/src/main/kotlin/com/zeiterfassung/controller/` — AuthController, UserController
- `backend/src/main/kotlin/com/zeiterfassung/config/` — DataSeeder

### Frontend additions
- `frontend/src/services/apiClient.ts` — 401 interceptor with refresh queue
- `frontend/src/context/AuthContext.tsx` — token management, auto-refresh
- `frontend/src/pages/LoginPage.tsx` — zod validation, error handling, language switcher
- `frontend/src/components/ProtectedRoute.tsx` — auth + permission checks

---

## Phase 3: Time Tracking Core ✅
- **Status**: COMPLETE
- **PR**: (merged into main — check TimeTrackingService, TimeTrackingController)
- **Merged**: 2026-02-21

### What was delivered
- **TimeTrackingService**: clock-in, clock-out, break-start, break-end with conflict detection
- **TimeTrackingController** (`/api/time`): all endpoints with @PreAuthorize permissions
- **ArbZGComplianceService**: max 10h/day, mandatory breaks (30min after 6h, 45min after 9h), 11h rest
- **Daily summaries**: automatic calculation of work/break/overtime minutes
- **Time sheets**: monthly and date-range views
- **Tracking status**: real-time status endpoint (CLOCKED_OUT, CLOCKED_IN, ON_BREAK)
- **Manager endpoints**: team entry viewing, manual entry creation, entry editing
- **Employee config**: CRUD for per-employee work hours, days, vacation config
- **DTOs**: ClockInRequest, ClockOutRequest, BreakStartRequest, BreakEndRequest, ManualTimeEntryRequest, EditTimeEntryRequest, TimeEntryResponse, DailySummaryResponse, TimeSheetResponse, TrackingStatusResponse, EmployeeConfigRequest

### Key API endpoints implemented
- POST /api/time/clock-in, /clock-out, /break/start, /break/end
- GET /api/time/status, /today, /entries, /summary, /timesheet
- GET /api/time/manage/team/entries
- POST /api/time/manage/entries (manual entry)
- PUT /api/time/manage/entries/{id} (edit entry)
- GET/PUT /api/time/manage/employee-config/{userId}

---

## Phase 4: Vacation Management ✅
- **Status**: COMPLETE
- **PR**: #41 (copilot/build-vacation-management-system)
- **Merged**: 2026-02-21
- **Branch**: `copilot/build-vacation-management-system`

### What was delivered

#### Backend
- **V6 Flyway migration**: fix `work_days` column type (`jsonb` → `text`) — resolves Hibernate schema validation error on startup
- **JPA Entities**: `VacationRequestEntity`, `VacationBalanceEntity`, `PublicHolidayEntity` (all fields from existing DB tables, proper @ManyToOne relations)
- **Repositories**:
  - `VacationRequestRepository` — findByUserId (paged), findOverlapping, findByStatus, findByUserIdAndYear
  - `VacationBalanceRepository` — findByUserIdAndYear, findByUserId, findByYear
  - `PublicHolidayRepository` — findByDateBetween, findApplicableForYear (fixed JPQL `EXTRACT(YEAR FROM ...)`)
- **DTOs**: CreateVacationRequest, UpdateVacationRequest, ApproveVacationRequest, RejectVacationRequest, VacationRequestResponse, VacationBalanceResponse, PublicHolidayResponse
- **VacationService** (`/api/vacation`):
  - BUrlG-compliant working-day calculation (excludes weekends, public holidays, per-employee work_days)
  - Half-day support (0.5 day increments)
  - Balance management with annual carry-over (capped by `vacation_carry_over_max`)
  - Balance auto-initialisation per year with carry-over from previous year
  - Overlap detection for pending/approved requests
  - Past-date validation
  - Self-approval prevention (managers cannot approve their own requests)
  - Audit logging for all state changes
- **VacationController** (`/api/vacation`): 12 endpoints, all with `@PreAuthorize`

#### Email Notifications
- **EmailService**: async HTML/text dispatch, globally togglable via `app.mail.enabled`
- **NotificationService**: vacation created → manager notified; approved/rejected → employee notified; approved-then-cancelled → approver notified
- **MonthlyReportScheduler**: cron 1st of each month — personal hours + vacation balance report to all active employees; team summary to managers
- `spring-boot-starter-mail` dependency added
- `@EnableScheduling` added to main application class

#### DSGVO / GDPR Compliance
- `TimeTrackingService.getTeamMemberEntries` verifies manager/subordinate relationship server-side
- `VacationService.getBalanceForManager` enforces subordinate check before returning balance
- Employees cannot access other employees' time or vacation data via any API endpoint

#### Pre-existing Bug Fixes
- Fixed `contextLoads()` test failure: downgraded `springdoc-openapi` from `3.0.1` → `2.3.0` (3.0.1 requires Spring Boot 4.x, causing class-not-found errors at test startup)
- Fixed invalid JPQL: `FUNCTION('EXTRACT', 'YEAR' FROM p.date)` → `EXTRACT(YEAR FROM p.date)` in `PublicHolidayRepository`
- Fixed 6 `VacationServiceTest` failures caused by Mockito `any()` returning null for Kotlin non-nullable types
- Upgraded `@Value` annotations to `@param:Value` on all constructor parameters
- Fixed npm peer dependency conflicts: `i18next` ^23→^25, `@types/react` ^18→^19, `@vitest/coverage-v8` ^1→^4
- Fixed TypeScript error TS6133: removed unused `user` variable from `VacationPage.tsx`

#### Frontend
- **`vacationService.ts`**: typed API client for all 12 vacation endpoints
- **`VacationPage.tsx`**: balance card (total/used/pending/remaining), tabbed request list (filterable by year/status with status badges), new-request form with live day preview, monthly calendar; team view gated on `vacation.view.team` permission (DSGVO)
- **`VacationApprovalPage.tsx`**: manager approval queue with inline approve/reject-with-reason modal
- Route `/vacation/approvals` added to `App.tsx` with `vacation.approve` permission guard
- Sidebar nav item "Vacation Approvals" added to `Layout.tsx` (visible only with `vacation.approve`)
- Full i18n coverage in both `de` and `en` translation files including `common.unknown` fallback

#### Tests
- 24 unit tests in `VacationServiceTest` covering: create (success, overlap, past date, insufficient balance, half-day), cancel (pending, approved with balance restore), approve/reject (success, self-approval rejection, not-pending), calculateWorkingDays (weekdays only, with holidays, with half-days), balance initialization, carry-over enforcement, public holiday filtering

#### i18n (backend)
- All vacation lifecycle messages in `messages_en.properties` and `messages_de.properties`
- Email templates for: vacation created/cancelled/approved/rejected/monthly-personal/monthly-team
- `email.report.team.member.line` key added (fixes hard-coded German text in team report)

### Key API endpoints implemented
- POST /api/vacation/requests
- GET /api/vacation/requests (paginated, filterable by year/status)
- GET /api/vacation/requests/{id}
- PUT /api/vacation/requests/{id}
- DELETE /api/vacation/requests/{id} (cancel)
- POST /api/vacation/requests/{id}/approve
- POST /api/vacation/requests/{id}/reject
- GET /api/vacation/pending
- GET /api/vacation/balance
- GET /api/vacation/balance/{userId}
- GET /api/vacation/holidays
- GET /api/vacation/calendar

### Backend key files
- `backend/src/main/kotlin/com/zeiterfassung/model/entity/VacationRequestEntity.kt`
- `backend/src/main/kotlin/com/zeiterfassung/model/entity/VacationBalanceEntity.kt`
- `backend/src/main/kotlin/com/zeiterfassung/model/entity/PublicHolidayEntity.kt`
- `backend/src/main/kotlin/com/zeiterfassung/model/dto/VacationDtos.kt`
- `backend/src/main/kotlin/com/zeiterfassung/repository/VacationRequestRepository.kt`
- `backend/src/main/kotlin/com/zeiterfassung/repository/VacationBalanceRepository.kt`
- `backend/src/main/kotlin/com/zeiterfassung/repository/PublicHolidayRepository.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/VacationService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/EmailService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/NotificationService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/MonthlyReportScheduler.kt`
- `backend/src/main/kotlin/com/zeiterfassung/controller/VacationController.kt`
- `backend/src/main/resources/db/migration/V6__fix_work_days_column_type.sql`
- `backend/src/test/kotlin/com/zeiterfassung/service/VacationServiceTest.kt`

### Frontend key files
- `frontend/src/services/vacationService.ts`
- `frontend/src/pages/VacationPage.tsx`
- `frontend/src/pages/VacationApprovalPage.tsx`

### Depends on
- Phase 2 (auth/users) ✅
- Phase 3 (time tracking — for work day calculations) ✅

---

## Phase 5: Dashboard & Reporting 🔲
- **Status**: PLANNED
- **Priority**: HIGH

### What needs to be built
- **DashboardPage**: replace placeholder with real data widgets
  - Today's work time (live counter)
  - Weekly/monthly work hours summary
  - Vacation balance overview
  - Team status (for managers)
  - Compliance warnings
  - Recent activity feed
- **Reports**: monthly time sheets, overtime reports, vacation usage reports
- **CSV import/export**: time entries, user data, vacation data
- **Charts**: work hour trends, overtime trends, vacation usage
- **Admin dashboard**: system-wide statistics

### Depends on
- Phase 3 (time tracking) ✅
- Phase 4 (vacation management) ✅

---

## Phase 6: Terminal (Raspberry Pi) Full Integration 🔲
- **Status**: PLANNED
- **Priority**: MEDIUM

### What needs to be built
- **Terminal API endpoint**: POST /api/terminal/clock (RFID-based clock in/out)
- **Terminal service**: look up user by RFID tag, create time entry, return employee info
- **Terminal UI completion**: implement full iced screens (Idle → scan → ClockIn/ClockOut/Error)
- **Offline buffering**: complete SQLite buffer sync with backend
- **Audio feedback**: success/error sounds on scan
- **Network resilience**: automatic retry, offline queue processing
- **Terminal configuration**: admin page for managing terminals
- **Terminal health monitoring**: heartbeat endpoint, admin status view

### Depends on
- Phase 2 (auth/users — RFID management) ✅
- Phase 3 (time tracking) ✅

---

## Phase 7: Mobile Apps Full Implementation 🔲
- **Status**: PLANNED
- **Priority**: MEDIUM

### What needs to be built
- **Android**: implement all placeholder screens (login, dashboard, time tracking, vacation)
- **iOS**: implement all placeholder views (login, dashboard, time tracking, vacation)
- **Push notifications**: time tracking reminders, vacation approvals
- **Biometric auth**: fingerprint/face ID for quick access
- **Offline support**: local caching, sync on reconnect
- **App store preparation**: icons, screenshots, metadata

### Depends on
- Phase 4 (vacation management) ✅
- Phase 5 (dashboard/reporting)

---

## Phase 8: Admin Panel & Settings 🔲
- **Status**: PLANNED
- **Priority**: MEDIUM

### What needs to be built
- **Admin page**: replace placeholder with full user management UI
- **User CRUD UI**: create/edit/delete users, assign roles, manage RFID
- **Role management UI**: view/edit roles and permissions
- **System settings UI**: ArbZG thresholds, company info, email config
- **Public holiday management UI**: add/edit/remove holidays
- **Audit log viewer**: searchable/filterable audit trail
- **CSV import UI**: upload and preview CSV data before import

### Depends on
- Phase 2 (auth/users) ✅

---

## Phase 9: Testing & Security Hardening 🔲
- **Status**: PLANNED
- **Priority**: HIGH

### What needs to be built
- **Backend unit tests**: ≥90% coverage for all services and controllers
- **Frontend unit tests**: ≥85% coverage with Vitest
- **E2E tests**: comprehensive Playwright test suite with screenshots for docs
- **Penetration testing**: OWASP ZAP automated scans, manual testing checklist
- **Security headers**: verify CSP, HSTS, X-Frame-Options, X-Content-Type-Options
- **Input validation**: comprehensive server-side validation on all endpoints
- **Rate limiting**: implement on all sensitive endpoints
- **Dependency audit**: verify all dependencies are up-to-date and secure

---

## Phase 10: Documentation & Polish 🔲
- **Status**: PLANNED
- **Priority**: MEDIUM

### What needs to be built
- **User documentation**: with Playwright screenshots
- **Admin documentation**: configuration guide
- **API documentation**: complete OpenAPI/Swagger annotations
- **Installation guide**: complete with all scenarios
- **Architecture decision records**: document key technical decisions
- **Performance optimization**: database indexing, query optimization, caching
- **Accessibility**: WCAG 2.1 AA compliance
- **Browser testing**: cross-browser compatibility
