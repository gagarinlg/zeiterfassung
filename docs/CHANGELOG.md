# Changelog

All notable changes to the Zeiterfassung project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added
- Comprehensive Playwright E2E test suite (Phase 9)
  - `e2e/tests/helpers.ts`: shared mock data and auth/API mock helpers
  - `e2e/tests/login.spec.ts`: 12 tests covering form rendering, language toggle, validation, login flows (success/401/423/429)
  - `e2e/tests/navigation.spec.ts`: 6 tests for sidebar, route navigation, protected routes, logout
  - `e2e/tests/dashboard.spec.ts`: 8 tests for widgets, status banners, error handling
  - `e2e/tests/time-tracking.spec.ts`: 14 tests for all clock states, buttons, timesheet table, CSV export, errors
  - `e2e/tsconfig.json`: TypeScript configuration for E2E tests
  - `@types/node` devDependency for Node.js types in E2E config

### Changed
- `playwright.config.ts`: screenshot mode changed from `only-on-failure` to `on` (always capture for docs)

### Planned
- Phase 9: Testing & Security Hardening (continued)
- Phase 10: Documentation & Polish

## [Phase 7+8 - Mobile Apps + Admin Panel] - 2026-03-01

### Added (Phase 8 — Admin Panel)

**Backend**
- `SystemSettingEntity` + `SystemSettingRepository`: JPA key-value store for system settings
- `AdminService`: paginated audit log retrieval (filter by userId), system settings CRUD
- `AdminController` (`/api/admin`): `GET /audit-log`, `GET /settings`, `PUT /settings/{key}` — all `@PreAuthorize("hasRole('ADMIN')")`
- `AdminDtos.kt`: `AuditLogResponse`, `SystemSettingResponse`, `UpdateSystemSettingRequest`
- `AdminServiceTest`: 9 unit tests

**Frontend**
- `adminService.ts`: typed API client for user management, audit log, and settings endpoints
- `AdminPage.tsx`: full 3-tab admin panel (User Management, Audit Log, System Settings)
  - User Management: paginated table with client-side search, create/edit/deactivate/delete modals, role assignment, RFID management, password reset
  - Audit Log: paginated table with action, user, entity, IP address, timestamp
  - System Settings: inline-editable key-value table
- All form inputs have `htmlFor`/`id` pairs for accessibility
- i18n keys `admin.users_tab`, `admin.audit.*`, `admin.settings.*`, `admin.errors.*` in EN + DE
- `AdminPage.test.tsx`: 25 frontend tests

### Added (Phase 7 — Android)
- `Models.kt`: User, LoginResponse, TrackingStatusResponse, VacationBalance, VacationRequest, PageResponse
- Retrofit `ZeiterfassungApi` with all required endpoints
- `AuthPreferences` (DataStore), `AuthRepository`, `TimeTrackingRepository`, `VacationRepository`
- `NetworkModule`: OkHttp auth interceptor, Moshi, Retrofit DI module
- `AuthViewModel` with empty-field validation and session restore
- `DashboardViewModel`, `TimeTrackingViewModel`, `VacationViewModel`
- `LoginScreen`, `DashboardScreen`, `TimeTrackingScreen`, `VacationScreen` — all replace placeholder composables
- `NavGraph` updated to use real screens
- `AuthViewModelTest` (8), `TimeTrackingViewModelTest` (9), `VacationViewModelTest` (7)
- Test deps: `mockk`, `turbine`, `kotlinx-coroutines-test`, `core-testing`

### Added (Phase 7 — iOS)
- `KeychainHelper`: Security-framework wrapper for secure token storage
- `DashboardViewModel`, `TimeTrackingViewModel`, `VacationViewModel`
- `VacationService`: getBalance, paginated getRequests
- `TimeService` extended: getTrackingStatus, startBreak, endBreak
- `DashboardView`, `TimeTrackingView`, `VacationView` — real data replacing placeholder `Text`
- `VacationRequest` updated to flat backend-compatible fields with `PageResponse<T>`
- `TrackingStatusResponse`, `VacationBalance` models added
- i18n: `vacation_request`, `time_tracking_today_work/break`, `vacation_total/used`, `common_days`, improved `common_error` in EN + DE

### Fixed
- **CI**: removed unused `within` import in `AdminPage.test.tsx` that caused ESLint error and failed the "Frontend - Lint & Test" CI job
- **iOS security**: replaced all `UserDefaults` token storage with Keychain in `AuthViewModel` and `APIClient`
- **iOS model**: `LoginResponse` corrected from `tokens: AuthTokens` to flat `accessToken`/`refreshToken` to match backend
- **iOS model**: `VacationRequest` updated to match backend fields (`startDate`/`endDate` as String, `isHalfDayStart/End`, `totalDays`, `rejectionReason`, `createdAt`)
- **Android forms**: user creation/edit modal labels have `htmlFor`/`id` pairs for accessibility and screen reader support

## [Phase 6 - multi-terminal support] - 2026-03-01

### Added (backend)
- `TerminalService`: extracted business logic from `TerminalController`; `remainingVacationDays` now uses real `VacationService.getBalance()` instead of a TODO placeholder
- `@Transactional` on `TerminalService.scan()` — makes `getCurrentStatus` + `clockIn/clockOut` atomic; concurrent scans from two terminals cannot both succeed with the same action — the loser receives HTTP 409
- `GET /api/terminal/heartbeat` — terminals poll this to detect backend connectivity
- Renamed `TerminalScanResponse.action` → `entryType` for consistency with Rust struct
- `TerminalServiceTest`: 9 unit tests including race-condition scenario and per-terminal ID attribution

### Added (terminal/)
- `terminal_id` added to `[api]` section of `terminal.toml` and `ApiConfig` — each physical terminal must set a unique value so clock entries are attributed to the correct device
- `ApiError::Conflict` (HTTP 409) — distinguished from generic errors; shown as "Bitte erneut scannen" on screen
- Offline-sync loop now explicitly discards stale/conflicted events (409, 404) instead of blocking the queue
- New config test: `test_each_terminal_has_unique_id_in_config`
- Updated `test_load_from_toml_string` to include and assert `terminal_id`
- Updated `test_api_error_display` to cover `ApiError::Conflict`
- 15 total unit tests (terminal)


### Added (terminal/)
- `src/ui/mod.rs`: Full iced 0.12.1 `Application` implementation
  - State machine: `Idle` → `Loading` → `ClockIn` / `ClockOut` / `OfflineConfirm` / `Error` → `Idle`
  - Auto-return timers using `iced::time::every` (1 s ticks, configurable timeouts)
  - Async scan command via `Command::perform` → `POST /terminal/scan`
  - RFID subscription via `iced::subscription::channel` polling `RfidReader` at 50 ms
  - Background sync subscription triggered every `sync_interval_seconds`
- `src/ui/screens.rs`: Six screen view functions with colour-coded backgrounds
  - `idle_view` — clock, company name, offline banner
  - `loading_view` — "Verarbeitung…" spinner text
  - `clock_in_view` — green, employee name, timestamp, countdown
  - `clock_out_view` — red, hours worked, break time, overtime, remaining vacation
  - `offline_confirm_view` — amber, buffered-locally message
  - `error_view` — amber, error type and message
- 14 unit tests across `api`, `buffer`, and `config` modules

### Fixed
- `src/api/mod.rs`: Added `#[serde(rename_all = "camelCase")]` to `EmployeeInfo`,
  `ClockResponse`, and `ClockRequest`; changed endpoint from `/terminal/clock` to
  `/terminal/scan`
- `src/audio/mod.rs`: Explicit `drop(handle)` after playback to keep audio device
  alive until sound finishes
- `src/main.rs`: Removed `#![allow(dead_code)]` scaffold placeholder

## [Phase 5 - partial] - 2026-02-22

### Added
- `GET /api/time/export/csv` endpoint: exports monthly timesheet as CSV (`TimeTrackingController`)
- `TrackingStatusResponse`, `TimeSheetResponse`, `TrackingStatus` TypeScript types (`frontend/src/types/index.ts`)
- Full `timeService.ts` rewrite with correct API endpoints matching backend (`/time/clock-in`, `/time/clock-out`, `/time/break/start`, `/time/break/end`, `/time/status`, `/time/today`, `/time/entries`, `/time/summary/daily/{date}`, `/time/summary/weekly`, `/time/summary/monthly`, `/time/timesheet`, `/time/manage/team/status`, `/time/export/csv`)
- `DashboardPage`: real data widgets — today work time, weekly hours, vacation balance, team presence count (managers), compliance warnings, pending vacation requests (managers), team status list
- `TimeTrackingPage`: full implementation — status badge, clock-in/out/break buttons, live elapsed timer (updates every minute), today's entries list, monthly timesheet table with compliance badges, CSV export button
- i18n keys: `time_tracking.status.*`, `time_tracking.elapsed`, `time_tracking.timesheet`, `time_tracking.export_csv`, `time_tracking.no_entries_today`, `time_tracking.total_work`, `time_tracking.total_break`, `time_tracking.overtime`, `time_tracking.compliant`, `time_tracking.non_compliant`, `time_tracking.week`, `time_tracking.month`, `time_tracking.errors.*`, `dashboard.clocked_in_since`, `dashboard.on_break_since`, `dashboard.compliance_warning`, `dashboard.team_status`, `dashboard.no_team` (DE + EN)

## [Phase 4] - 2026-02-21

### Added
- VacationRequestEntity, VacationBalanceEntity, PublicHolidayEntity JPA entities
- VacationRequestRepository, VacationBalanceRepository, PublicHolidayRepository
- VacationService: BUrlG-compliant working-day calculation, half-day support, balance management, carry-over logic, overlap detection, self-approval prevention
- VacationController (/api/vacation): 12 REST endpoints with @PreAuthorize
- EmailService: async email dispatch, globally togglable via `app.mail.enabled`
- NotificationService: vacation lifecycle email notifications (created/approved/rejected/cancelled)
- MonthlyReportScheduler: cron job sending personal hours reports and team summaries on 1st of each month
- spring-boot-starter-mail dependency
- @EnableScheduling on main application class
- DSGVO enforcement: server-side manager/subordinate checks in TimeTrackingService and VacationService
- Frontend VacationPage: balance card, tabbed request list, new-request form with live day preview, monthly calendar
- Frontend VacationApprovalPage: manager approval queue with approve/reject-with-reason modal
- Frontend vacationService.ts: typed API client for all vacation endpoints
- Route /vacation/approvals with vacation.approve permission guard
- Sidebar nav item "Vacation Approvals" (permission-gated)
- Full i18n coverage in de and en translation files
- All vacation + email message keys in messages_en.properties and messages_de.properties
- 24 unit tests in VacationServiceTest
- V6 Flyway migration: fix work_days column type (jsonb → text)
- copilot-instructions.md: added principles 10-12 (Fix all issues, DSGVO/GDPR, Email notifications)
- copilot-instructions.md: added Project Phase Tracking (MANDATORY) section

### Fixed
- contextLoads() test failure: downgraded springdoc-openapi from 3.0.1 → 2.3.0
- Invalid JPQL: FUNCTION('EXTRACT', 'YEAR' FROM ...) → EXTRACT(YEAR FROM ...) in PublicHolidayRepository
- 6 VacationServiceTest failures caused by Mockito any() / Kotlin non-nullable type incompatibility
- @Value annotations upgraded to @param:Value on all Kotlin constructor parameters
- npm peer dependency: i18next ^23 → ^25 (react-i18next@16 requirement)
- npm peer dependency: @types/react ^18 → ^19 (@types/react-dom@19 requirement)
- npm peer dependency: @vitest/coverage-v8 ^1 → ^4 (vitest@4 requirement)
- TypeScript TS6133: removed unused `user` variable from VacationPage.tsx


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
