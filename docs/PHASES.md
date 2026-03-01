# Zeiterfassung — Project Phases & Roadmap

> Last updated: 2026-03-01
>
> **Current phase:** Phase 10 (Security Features & LDAP Integration) — IN PROGRESS

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

## Phase 5: Dashboard & Reporting ✅
- **Status**: COMPLETE (partial — charts, admin dashboard, and advanced reports deferred to Phase 10)
- **Priority**: HIGH

### What was delivered
- **DashboardPage**: real data widgets — today's work time, weekly hours, vacation balance, team presence count (managers), compliance warnings for the week, pending vacation requests count (managers), team status list with employee names
- **TimeTrackingPage**: full implementation — color-coded status badge (CLOCKED_IN/ON_BREAK/CLOCKED_OUT), clock-in/out/break action buttons, drift-free elapsed timer (recalculates from `clockedInSince` on each tick), today's entries list, monthly timesheet table with compliance badges, CSV export button
- **CSV export endpoint**: `GET /api/time/export/csv?start=<date>&end=<date>` — returns RFC 4180-compliant CSV with CSV-injection protection
- **timeService.ts fix**: all API endpoints corrected to match actual backend paths (`/time/clock-in`, `/time/break/start`, etc.)
- **TypeScript types**: `TrackingStatus`, `TrackingStatusResponse`, `TimeSheetResponse`
- **i18n**: new keys for time tracking status, elapsed timer, timesheet, export, compliance, errors (DE + EN)
- **`common.total`** translation key added (DE + EN)

### Key files changed
- `frontend/src/services/timeService.ts` — complete rewrite with correct endpoints
- `frontend/src/types/index.ts` — added `TrackingStatus`, `TrackingStatusResponse`, `TimeSheetResponse`
- `frontend/src/pages/DashboardPage.tsx` — real API data, team member names, compliance warnings
- `frontend/src/pages/TimeTrackingPage.tsx` — full 359-line implementation
- `backend/src/main/kotlin/com/zeiterfassung/controller/TimeTrackingController.kt` — CSV export endpoint
- `frontend/src/locales/de/translation.json` — new i18n keys
- `frontend/src/locales/en/translation.json` — new i18n keys

### Depends on
- Phase 3 (time tracking) ✅
- Phase 4 (vacation management) ✅

---

## Phase 6: Terminal (Raspberry Pi) Full Integration ✅
- **Status**: COMPLETE
- **PR**: #58 (copilot/do-next-project-phase)
- **Merged**: 2026-03-01
- **Priority**: MEDIUM

### What was delivered

#### Backend
- **`TerminalService`**: extracted business logic from `TerminalController`
  - `scan(rfidTagId, terminalId)` — looks up user by RFID, toggles clock state, returns full response
  - `@Transactional` — `getCurrentStatus` + `clockIn/clockOut` are atomic; concurrent scans from two terminals result in HTTP 409 for the loser
  - `remainingVacationDays` uses real `VacationService.getBalance()` (fixes TODO placeholder)
- **`TerminalController`**: slim; delegates entirely to `TerminalService`
  - `POST /api/terminal/scan` — RFID scan → clock toggle
  - `GET /api/terminal/heartbeat` — terminals poll this to detect connectivity
- **`TerminalScanResponse.entryType`**: renamed from `action` for consistency with Rust struct
- **`TerminalServiceTest`**: 9 unit tests including:
  - Clock-in when CLOCKED_OUT, clock-out when CLOCKED_IN, clock-out when ON_BREAK
  - Unknown RFID → `ResourceNotFoundException`
  - Vacation service failure → graceful 0-days fallback
  - Concurrent scan conflict → `ConflictException` propagates (HTTP 409)
  - Two terminals with different IDs — correct `terminalId` attributed to each entry

#### Terminal (Rust)
- **Multi-terminal `terminal_id`**: moved from env-var fallback into `terminal.toml` / `ApiConfig`; every physical device configures a unique `terminal_id`
- **`ApiError::Conflict` (HTTP 409)**: new variant; shown as "Bitte erneut scannen" — prompts the user to scan again after a race condition
- **Offline sync loop**: explicitly discards stale/conflicted events (409, 404) so the queue never blocks
- **Full iced 0.12 Application**: state machine (Idle → Loading → ClockIn/ClockOut/OfflineConfirm/Error → Idle)
- **Screen views**: 6 colour-coded screens (green, red, amber)
- **RFID subscription**: async `iced::subscription::channel` at 50 ms
- **Audio**: rodio success/error sounds
- **15 unit tests**: api (5), buffer (5), config (5)

### Key files
- `backend/src/main/kotlin/com/zeiterfassung/service/TerminalService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/controller/TerminalController.kt`
- `backend/src/test/kotlin/com/zeiterfassung/service/TerminalServiceTest.kt`
- `terminal/src/ui/mod.rs`, `terminal/src/ui/screens.rs`
- `terminal/src/api/mod.rs`, `terminal/src/config.rs`
- `terminal/terminal.toml`

### Depends on
- Phase 2 (auth/users — RFID management) ✅
- Phase 3 (time tracking) ✅

---

## Phase 7: Mobile Apps Full Implementation ✅
- **Status**: COMPLETE
- **PR**: #59 (copilot/update-project-phase-and-tests)
- **Merged**: 2026-03-01
- **Priority**: MEDIUM

### What was delivered

#### Android (Kotlin / Jetpack Compose)
- **Data layer**: `Models.kt` (User, LoginResponse, TrackingStatusResponse, VacationBalance, VacationRequest, PageResponse), Retrofit `ZeiterfassungApi`, `AuthPreferences` (DataStore), `AuthRepository`, `TimeTrackingRepository`, `VacationRepository`
- **DI**: `NetworkModule` (OkHttp auth interceptor, Moshi, Retrofit)
- **ViewModels**: `AuthViewModel` (login/logout with input validation, session restore), `DashboardViewModel`, `TimeTrackingViewModel` (clock-in/out/break), `VacationViewModel`
- **Screens**: `LoginScreen`, `DashboardScreen` (status + vacation summary), `TimeTrackingScreen` (state-aware buttons, today totals), `VacationScreen` (balance card + request list)
- **NavGraph**: updated to use real screens (replaced all `*Placeholder` composables)
- **Tests**: `AuthViewModelTest` (8 tests), `TimeTrackingViewModelTest` (9 tests), `VacationViewModelTest` (7 tests)
- **New deps**: `mockk`, `turbine`, `kotlinx-coroutines-test`, `androidx.arch.core:core-testing`

#### iOS (Swift / SwiftUI)
- **Security**: replaced all `UserDefaults` token storage with `KeychainHelper` (Security framework) — including `APIClient` auth header and `AuthViewModel`
- **Models**: fixed `LoginResponse` to flat structure matching backend; added `TrackingStatusResponse`, `VacationBalance`, `PageResponse<T>`; updated `VacationRequest` to match backend fields
- **Services**: `VacationService` (balance, paginated requests), `TimeService` extended (status, startBreak, endBreak), `AuthService` cleaned up
- **ViewModels**: `AuthViewModel` (Keychain, `currentUserId`), `DashboardViewModel`, `TimeTrackingViewModel`, `VacationViewModel`
- **Views**: `DashboardView` (real status + vacation balance), `TimeTrackingView` (state-aware action buttons, elapsed timer format), `VacationView` (balance breakdown + request list with rejection reasons)
- **i18n**: added `vacation_request`, `time_tracking_today_work`, `time_tracking_today_break`, `vacation_total`, `vacation_used`, `common_days`, improved `common_error` in EN + DE

### Key files
- `mobile/android/app/src/main/kotlin/com/zeiterfassung/app/data/`
- `mobile/android/app/src/main/kotlin/com/zeiterfassung/app/di/NetworkModule.kt`
- `mobile/android/app/src/main/kotlin/com/zeiterfassung/app/ui/viewmodel/`
- `mobile/android/app/src/main/kotlin/com/zeiterfassung/app/ui/screen/`
- `mobile/android/app/src/test/kotlin/com/zeiterfassung/app/`
- `mobile/ios/ZeiterfassungApp/Utils/KeychainHelper.swift`
- `mobile/ios/ZeiterfassungApp/ViewModels/`
- `mobile/ios/ZeiterfassungApp/Views/`
- `mobile/ios/ZeiterfassungApp/Services/`

### Depends on
- Phase 4 (vacation management) ✅
- Phase 5 (dashboard/reporting) ✅

---

## Phase 8: Admin Panel & Settings ✅
- **Status**: COMPLETE
- **PR**: #59 (copilot/update-project-phase-and-tests)
- **Merged**: 2026-03-01
- **Priority**: MEDIUM

### What was delivered

#### Backend
- **`SystemSettingEntity`** + **`SystemSettingRepository`**: JPA entity for key-value system settings
- **`AdminService`**: audit log (paginated, user-filtered), system settings CRUD (getAll, getByKey, update)
- **`AdminController`** (`/api/admin`): `GET /audit-log`, `GET /settings`, `PUT /settings/{key}` — all `@PreAuthorize("hasRole('ADMIN')")`
- **`AdminDtos.kt`**: `AuditLogResponse`, `SystemSettingResponse`, `UpdateSystemSettingRequest`
- **`AdminServiceTest`**: 9 unit tests (audit log paging, user filter, settings CRUD, key-not-found, edge cases)

#### Frontend
- **`adminService.ts`**: typed client for user management, audit log, and settings endpoints
- **`AdminPage.tsx`**: 3-tab UI:
  - **User Management**: paginated table with client-side search, create/edit/deactivate/delete modals, role checkbox assignment, RFID update modal, password reset modal, delete confirmation
  - **Audit Log**: paginated table showing action, user, entity type/ID, IP address, timestamp
  - **System Settings**: inline-editable settings table (click pencil → edit input → save/cancel)
- **All form labels** have `htmlFor`/`id` pairs for accessibility
- **`AdminPage.test.tsx`**: 25 frontend tests (all tabs, modals, error banners, search filter, CRUD flows)
- **i18n**: `admin.users_tab`, `admin.audit.*`, `admin.settings.*`, `admin.errors.*` in EN + DE

### Key files
- `backend/src/main/kotlin/com/zeiterfassung/model/entity/SystemSettingEntity.kt`
- `backend/src/main/kotlin/com/zeiterfassung/repository/SystemSettingRepository.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/AdminService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/controller/AdminController.kt`
- `backend/src/main/kotlin/com/zeiterfassung/model/dto/AdminDtos.kt`
- `backend/src/test/kotlin/com/zeiterfassung/service/AdminServiceTest.kt`
- `frontend/src/services/adminService.ts`
- `frontend/src/pages/AdminPage.tsx`
- `frontend/src/test/AdminPage.test.tsx`

### Depends on
- Phase 2 (auth/users) ✅

---

## Phase 9: Testing & Security Hardening ✅
- **Status**: COMPLETE
- **Priority**: HIGH

### What has been delivered

#### Playwright E2E Test Suite (7 spec files, 62+ tests)
- **`e2e/tests/helpers.ts`**: shared mock data (user, tokens, tracking status, summaries, vacation balance) and reusable mock setup functions (`mockAuthenticatedUser`, `mockDashboardApis`, `mockTimeTrackingApis`); mock API paths corrected to match actual frontend service paths (`/api/time/` not `/api/time-tracking/`)
- **`e2e/tests/login.spec.ts`**: 12 tests — form rendering, language toggle, redirect from all protected routes, Zod client-side validation (empty fields, invalid email, empty password), successful login with mocked API, failed login (401/423/429 error messages)
- **`e2e/tests/navigation.spec.ts`**: 6 tests — sidebar links for admin user, route navigation to time tracking and vacation, protected route redirect, logout flow
- **`e2e/tests/dashboard.spec.ts`**: 8 tests — page rendering with title and user name, today's hours / weekly hours / vacation balance / team present widgets, clocked-in status banner, weekly summary values, error handling
- **`e2e/tests/time-tracking.spec.ts`**: 14 tests — clocked-out/clocked-in/on-break states with correct buttons, status badges, today summary, no-entries message, monthly timesheet table (heading, column headers, daily entries, total row, compliance badges, CSV export button), error handling
- **`e2e/tests/vacation.spec.ts`**: 8 tests — page rendering with title, balance card, tab navigation, requests table with status badges, new request form, calendar navigation, error handling
- **`e2e/tests/vacation-approval.spec.ts`**: 6 tests — page title, pending requests table with employee names, approve/reject buttons, reject modal with reason textarea, empty state message, error handling
- **`e2e/tests/admin.spec.ts`**: 8 tests — page title, tab navigation, user list display, create user button/modal, audit log entries, system settings display with edit buttons, error handling
- **`e2e/tsconfig.json`**: TypeScript config for E2E tests
- **`playwright.config.ts`**: updated to always capture screenshots (`screenshot: 'on'`)

#### Backend Integration Tests
- **`AuthControllerIntegrationTest`**: 10 `@SpringBootTest` integration tests — CORS preflight allowed/blocked, credentials header, login success/failure/validation, protected endpoint auth, security headers (HSTS, X-Frame-Options, X-Content-Type-Options)

#### Frontend Unit Tests
- **`dateUtils.test.ts`**: 18 tests covering `formatDate` (DD.MM.YYYY, YYYY-MM-DD, MM/DD/YYYY, empty, invalid, ISO timestamp, locale default), `formatTime` (24h, 12h, empty), `formatDateTime` (combined, empty), `formatMonthYear` (German, English), `getFirstDayOfWeek` (German=1, English=0)

#### CI/CD
- **`frontend-e2e` job** added to `.github/workflows/ci.yml` — installs Playwright + Chromium, runs E2E tests, uploads test results and screenshots as artifacts
- **`build` job** now depends on `frontend-e2e` in addition to all other lint/test jobs

#### Bug Fixes
- **CORS**: Changed `allowedOrigins` → `allowedOriginPatterns` in `SecurityConfig.kt` to fix localhost login
- **V7 migration**: Fixed column names from `setting_key`/`setting_value` to `key`/`value` matching V5 schema
- **NavLink active state**: Added `end` prop to `/vacation` NavLink to prevent highlighting when on `/vacation/approvals`
- **ktlint**: Auto-formatted `AuthControllerIntegrationTest.kt` to pass CI

#### Documentation
- **`docs/installation/README.md`**: comprehensive installation guide covering Docker deployment, development setup, production deployment, Raspberry Pi terminal setup, mobile apps, configuration reference, and troubleshooting

### What could be expanded in future
- **Backend unit tests**: expand to ≥90% coverage for all services and controllers
- **Frontend unit tests**: expand to ≥85% coverage with Vitest
- **Penetration testing**: OWASP ZAP automated scans, manual testing checklist
- **Rate limiting**: implement on all sensitive endpoints
- **Dependency audit**: verify all dependencies are up-to-date and secure

---

## Phase 10: Security Features & LDAP Integration 🚧
- **Status**: IN PROGRESS
- **Priority**: HIGH

### What has been delivered
- **V8 Flyway migration**: TOTP columns, password_reset_tokens table, LDAP system settings
- **TotpService**: RFC 6238 TOTP generation and verification with base32 encoding
- **PasswordResetService**: email-based password reset with token hashing and expiration
- **LdapService**: LDAP/AD configuration management via system_settings
- **Self-service profile update**: users can update their own preferences
- **Recursive subordinate listing**: hierarchical manager rights
- **Auth enhancements**: TOTP verification during login, password confirmation validation
- **New DTOs**: TOTP, password reset, LDAP configuration
- **SecurityConfig**: public password reset endpoints

### Key files
- `backend/src/main/resources/db/migration/V8__add_totp_and_password_reset.sql`
- `backend/src/main/kotlin/com/zeiterfassung/model/entity/PasswordResetTokenEntity.kt`
- `backend/src/main/kotlin/com/zeiterfassung/repository/PasswordResetTokenRepository.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/TotpService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/PasswordResetService.kt`
- `backend/src/main/kotlin/com/zeiterfassung/service/LdapService.kt`

### Key API endpoints
- POST /api/auth/totp/setup, /totp/enable, /totp/disable
- POST /api/auth/password/reset-request, /password/reset-confirm
- PUT /api/users/me
- GET /api/users/{id}/all-subordinates
- GET /api/admin/ldap, PUT /api/admin/ldap

### What still needs to be built
- Frontend UI for TOTP setup/disable
- Frontend password reset page
- Frontend LDAP configuration page in admin panel
- Unit tests for TotpService, PasswordResetService, LdapService

### Depends on
- Phase 2 (auth/users) ✅
- Phase 8 (admin panel) ✅

---

## Phase 11: Documentation & Polish 🔲
- **Status**: PLANNED
- **Priority**: MEDIUM

### What needs to be built
- **User documentation**: with Playwright screenshots
- **Admin documentation**: configuration guide
- **API documentation**: complete OpenAPI/Swagger annotations
- **Installation guide**: complete with Docker, development, production, terminal, and mobile setup
- **Architecture decision records**: document key technical decisions
- **Performance optimization**: database indexing, query optimization, caching
- **Accessibility**: WCAG 2.1 AA compliance
- **Browser testing**: cross-browser compatibility
