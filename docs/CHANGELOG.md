# Changelog

All notable changes to the Zeiterfassung project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased] — Unit Tests & Code Quality

### Added
- **`SickLeaveServiceTest`** (12 tests): report, update, cancel, certificate, validation, authorization
- **`BusinessTripServiceTest`** (16 tests): create, update, cancel, approve, reject, complete, validation, authorization
- **`ProjectServiceTest`** (14 tests): project CRUD, time allocation CRUD, validation, authorization
- **`GdprServiceTest`** (7 tests): data export, anonymization, deletion flags, token revocation

### Fixed
- All ktlint issues across the entire codebase (unused imports, expression body formatting)
- Hardcoded fallback value in `NotificationService` replaced with localized `common.not_specified` message key
- Fully qualified `Page.empty()` call replaced with short form in `BusinessTripService`

### Changed
- Backend test count increased from 210 to 260 tests across 26 test classes

## [Unreleased] — Phase 13: Database Backup Frontend Tab

### Added
- **Admin Backups Tab** (frontend): New "Backups" tab in AdminPage with full backup management UI
  - List existing backups in a table (filename, human-readable size, date)
  - Create new backup with loading state
  - Download backup files
  - Restore from existing backup with confirmation dialog
  - Delete backup with confirmation dialog
  - Upload & restore from a `.sql.gz` file with file input
  - Success/error banners with auto-clear after 5 seconds
- **`adminService` backup methods**: `listBackups`, `createBackup`, `downloadBackup`, `restoreBackup`, `restoreFromUpload`, `deleteBackup` with `encodeURIComponent` for safe filenames
- **`BackupInfo` and `RestoreResponse` types** in `adminService.ts`
- **i18n translations**: Full English and German translations for all backup UI strings (`backup.*` keys, `admin.backups_tab`)

## [Unreleased] — GDPR Data Export & Deletion

### Added
- **GDPR Data Export** (backend): `GdprService.exportUserData()` gathers all personal data (user info, time entries, vacation requests, sick leaves, business trips, audit log) into a structured `GdprDataExportResponse`
- **GDPR Account Deletion** (backend): `GdprService.requestDeletion()` performs soft delete, anonymizes personal data (name, email, phone, RFID, employee number, TOTP), and revokes all refresh tokens
- **GdprController** at `/gdpr`: 4 endpoints with Swagger annotations
  - `GET /gdpr/export` — export own data (authenticated users)
  - `POST /gdpr/delete` — request own account deletion (authenticated users)
  - `GET /gdpr/export/{userId}` — admin export of user data (`admin.users.manage`)
  - `POST /gdpr/delete/{userId}` — admin delete user data (`admin.users.manage`)
- **GdprDtos.kt**: `GdprDataExportResponse`, `GdprPersonalInfo`, `GdprTimeEntry`, `GdprVacationRequest`, `GdprSickLeave`, `GdprBusinessTrip`, `GdprAuditEntry`, `GdprDeletionResponse`
- **Repository methods**: `TimeEntryRepository.findByUserIdOrderByTimestampAsc()`, `AuditLogRepository.findByUserIdOrderByCreatedAtAsc()` for unpaginated GDPR data retrieval

## [Unreleased] — Phase 14: Frontend Pages for Sick Leave, Business Trips & Projects

### Added
- **Sick Leave Page** (frontend): `SickLeavePage.tsx` with list/report tabs, cancel action, certificate submission
- **Business Trip Page** (frontend): `BusinessTripPage.tsx` with list/new request tabs, cancel, complete with actual cost modal
- **Business Trip Approval Page** (frontend): `BusinessTripApprovalPage.tsx` manager view with approve/reject workflow
- **Projects Page** (frontend): `ProjectsPage.tsx` with allocations list, new allocation form, admin project management
- **API services**: `sickLeaveService.ts`, `businessTripService.ts`, `projectService.ts`
- **Routes**: `/sick-leave`, `/business-trips`, `/business-trips/approvals`, `/projects`
- **Navigation**: Sidebar links with icons (Thermometer, Plane, CheckCircle, FolderKanban)
- **i18n**: Full `sick_leave`, `business_trip`, `projects` sections in EN + DE translation files
- **Screenshots**: 4 new auto-generated screenshots (sick-leave, business-trips, business-trip-approvals, projects)
- **E2E screenshot tests**: 5 new tests in `screenshots.spec.ts` with mock data
- **User guide**: Added documentation sections for Sick Leave, Business Trips, Trip Approvals, Projects with screenshots

## [Unreleased] — Phase 14: Sick Leave, Business Trips & Project Time Allocation

### Added
- **Sick Leave Tracking** (backend): `SickLeaveEntity`, `SickLeaveRepository`, `SickLeaveService`, `SickLeaveController` at `/sick-leave`
  - Report, update, cancel sick leave; certificate submission workflow
  - Manager can report on behalf of employee (`POST /sick-leave/{userId}`)
  - Overlap detection for non-cancelled sick leaves
  - Audit logging for all state changes
  - Email notifications to managers on sick leave report
- **Business Trip Management** (backend): `BusinessTripEntity`, `BusinessTripRepository`, `BusinessTripService`, `BusinessTripController` at `/business-trips`
  - Full CRUD + approve/reject/complete workflow (like vacation requests)
  - Cost tracking (estimated and actual costs, cost center)
  - Overlap detection for active trips
  - Pending trips endpoint for managers
  - Audit logging and email notifications
- **Project/Cost Center Time Allocation** (backend): `ProjectEntity`, `TimeAllocationEntity`, repositories, `ProjectService`, `ProjectController` at `/projects`
  - Project CRUD (admin-only create/update, authenticated read)
  - Time allocation CRUD for employees with date/range queries
  - Project-level allocation reporting (admin)
  - Duplicate code/name detection for projects
- **Database migration** `V10__create_sick_leave_business_trip_projects.sql`: 4 new tables with indexes
- **Enums**: `SickLeaveStatus`, `BusinessTripStatus`
- **DTOs**: `SickLeaveDtos.kt`, `BusinessTripDtos.kt`, `ProjectDtos.kt`
- **Notification methods**: sick leave reported, business trip requested/approved/rejected
- **i18n messages**: German and English email templates for sick leave and business trip notifications

## [Unreleased] — Backend Unit Test Coverage Expansion

### Added
- **`BackupServiceTest`** (11 tests): list/get/delete backups, filename validation, path traversal protection, temp dir handling
- **`EmployeeConfigServiceTest`** (8 tests): get/update config, default config creation, partial updates, invalid JSON fallback
- **`MonthlyReportSchedulerTest`** (7 tests): personal & team report sending, inactive/deleted user skipping, email failure handling, formatHours
- **`AuditServiceTest`** (6 tests): login/logout/data-change/permission-change audit logging, exception handling, non-existent user
- **`TimeTrackingServiceEdgeCasesTest`** (12 tests): break-state conflicts, notes/terminal fields, delete/getTimeSheet errors, CLOCKED_OUT status
- **`UserServiceEdgeCasesTest`** (18 tests): update/delete edge cases, manager/substitute assignment, password change/reset, recursive subordinates, substitute delegation
- **`AuthServiceEdgeCasesTest`** (12 tests): successful login, TOTP flows, refresh token reuse detection, logout/logoutAll, getCurrentUser

### Changed
- Backend test count increased from 136 to 210 tests across 22 test classes

## [Unreleased] — CI: Mobile Release Builds

### Added
- **`build-mobile.yml`**: `build-android-release` job — builds signed (or unsigned) release APK/AAB on push to main or tags; uses `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` secrets for signing
- **`build-mobile.yml`**: `build-ios-release` job — builds Swift package in release configuration on push to main or tags
- **`build-deploy.yml`**: `build-android-release` and `build-ios-release` jobs added to tag-triggered release workflow; Android APK/AAB and iOS release artifacts are downloaded and included in GitHub Releases alongside the terminal binary

## [Unreleased] — Documentation: Installation, Provisioning & Testing Guides

### Added
- **`docs/installation/mobile-apps.md`**: Comprehensive mobile app installation guide covering Android and iOS build, signing, distribution (APK, Google Play, TestFlight, App Store), server URL configuration, and MDM provisioning overview
- **`docs/installation/mobile-provisioning.md`**: Detailed MDM/enterprise provisioning guide with configuration examples for Google Workspace, VMware Workspace ONE, Microsoft Intune (Android), Jamf Pro, Microsoft Intune (iOS), VMware Workspace ONE (iOS); includes local testing procedures
- **`docs/installation/terminal.md`**: Full Raspberry Pi terminal installation guide covering hardware requirements, OS setup, system dependencies, building from source, cross-compilation, `terminal.toml` annotated reference, kiosk mode with cage, RFID reader setup, audio configuration, systemd service file, network/firewall, offline buffering, maintenance, and troubleshooting
- **`docs/development/testing.md`**: Testing strategies guide covering backend (JUnit 5 + Mockito), frontend (Vitest + Playwright), terminal (cargo test, mock HTTP, virtual RFID, GUI state testing, offline buffer), mobile (JUnit + MockK, XCTest, Compose UI tests, device matrix), E2E overview, CI/CD integration, and coverage reporting

### Changed
- **`docs/installation/README.md`**: Added links to the new detailed installation guides (mobile-apps.md, mobile-provisioning.md, terminal.md)
- **`docs/development/setup.md`**: Added cross-reference link to the new testing guide
- **`docs/CURRENT_STATE.md`**: Added Documentation section listing all new guides; updated summary

## [Unreleased] — iOS Server URL Configuration

### Added
- **ServerConfigManager**: Singleton managing server URL with priority chain (MDM managed config > UserDefaults > default)
- **ServerSettingsView**: SwiftUI Form view for viewing/editing server URL; disables editing when URL is managed via MDM
- **ServerSettingsViewModel**: `@MainActor` ViewModel with managed config detection, save with success feedback
- **APIClient**: Updated initializer to use `ServerConfigManager.shared.effectiveServerUrl` instead of `ProcessInfo` environment variable
- **MainTabView**: Added gear menu with server settings and logout options
- **Localizable.strings**: Added 11 new i18n keys for server settings UI (EN + DE)

## [Unreleased] — Android Server URL Configuration

### Added
- **ServerConfigPreferences**: DataStore-based preferences for storing server URL with MDM managed app configuration support (`RestrictionsManager`)
- **ServerSettingsScreen**: Compose UI screen for viewing/editing server URL; disables editing when URL is managed by MDM
- **ServerSettingsViewModel**: ViewModel with managed config detection, save with success feedback
- **app_restrictions.xml**: Android managed app configuration schema for MDM provisioning of `server_url`
- **AndroidManifest.xml**: `APP_RESTRICTIONS` meta-data reference
- **NetworkModule**: `provideRetrofit` now reads effective server URL from `ServerConfigPreferences` (managed > user setting > default)
- **ZeiterfassungNavGraph**: `ServerSettings` route added to navigation

## [Unreleased] — Phase 13: Database Backup & Restore (IN PROGRESS)

### Added
- **BackupService**: scheduled daily backups at 2 AM via `@Scheduled(cron)`, configurable retention limit (default 31), `pg_dump`/`psql` via `ProcessBuilder`, gzip compression, audit logging for all operations
- **BackupController** (`/api/admin/backups`): `GET /` (list), `POST /` (create), `GET /{filename}` (download), `POST /restore/{filename}` (restore), `POST /restore/upload` (restore from upload), `DELETE /{filename}` (delete) — all `@PreAuthorize("hasAuthority('admin.users.manage')")`
- **BackupDtos.kt**: `BackupInfo(filename, sizeBytes, createdAt)`, `RestoreResponse(status, message)`
- **application.yml**: `app.backup.directory` and `app.backup.max-count` configuration properties with env var support

### Security
- Path traversal prevention: filename regex validation (`^[a-zA-Z0-9._-]+$`), canonical path check
- Uploaded files saved to temp location before restore
- All operations logged via `AuditService`

## [Unreleased] — Phase 12: Performance, Accessibility & API Documentation (COMPLETE)

### Improved
- **WCAG 2.1 AA accessibility**: Added skip-to-content link in Layout.tsx, ARIA landmark on sidebar, `id="main-content"` on main element, `aria-live="polite"` on DashboardPage loading state, reusable `.focus-ring` CSS utility class, and i18n keys for accessibility labels (EN + DE)

### Added
- **Spring caching**: `CacheConfig.kt` with `ConcurrentMapCacheManager` for `publicHolidays` and `systemSettings` caches; `@Cacheable` on `VacationService.getPublicHolidays()` and `AdminService.getSystemSettings()`; `@CacheEvict` on `AdminService.updateSystemSetting()` to invalidate cache on updates
- **OpenAPI/Swagger annotations**: Added `@Tag`, `@Operation`, `@ApiResponse`, and `@SecurityRequirement` annotations to all 7 backend controllers (AuthController, AdminController, TerminalController, EmployeeConfigController, TimeTrackingController, UserController, VacationController)
- **OpenApiConfig.kt**: New configuration class with `@OpenAPIDefinition` (title, description, version, tags) and `@SecurityScheme` for JWT Bearer authentication
- **Cross-browser testing**: Playwright config updated to run tests on Chromium, Firefox, and WebKit in all environments
- **Architecture Decision Records**: 5 ADRs in `docs/architecture/decisions/` (Spring Boot, JWT auth, React frontend, ArbZG compliance, Rust terminal)

### Changed
- Phase 12 status: PLANNED → COMPLETE
- **Break gap detection**: Gaps ≥15 min between CLOCK_OUT and next CLOCK_IN on the same day are now treated as qualifying breaks (ArbZG §4)
- **Vacation balance management**: Admin can manually set vacation balance via PUT /vacation/balance/{userId} (totalDays, usedDays, carriedOverDays)
- **Vacation carry-over endpoint**: Admin can trigger carry-over calculation via POST /vacation/balance/{userId}/carry-over
- **Vacation emails to substitutes**: Manager substitutes now receive notification emails when subordinates request vacation
- **Mobile build CI**: New `build-mobile.yml` GitHub Actions workflow builds Android APK and iOS app with downloadable artifacts
- **TimeTrackingServiceTest**: 2 new tests for break gap detection (qualifying ≥15 min and short <15 min gaps between CLOCK_OUT/CLOCK_IN)
- **VacationServiceTest**: 4 new tests for `setBalance` (update totalDays, update carriedOverDays) and `triggerCarryOver` (carry over remaining, cap at max)
- **NotificationServiceTest**: new test class with 2 tests for `notifyVacationRequestCreated` (sends to all managers, skips blank emails)

### Fixed
- **CI: Backend ktlint failures** in EmailServiceTest, LdapServiceTest, PasswordResetServiceTest, TotpServiceTest
- **CI: Frontend ESLint failure** — removed unused MOCK_MONTHLY_SUMMARY import from screenshots.spec.ts

## [Unreleased] — Phase 11: Documentation & Polish (COMPLETE)

### Added
- **Playwright screenshot spec**: `e2e/tests/screenshots.spec.ts` — generates 12 documentation screenshots across all pages
- **Documentation screenshots**: 12 PNG screenshots in `docs/screenshots/` (login, dashboard, time tracking, vacation, admin, settings, password reset)
- **User Guide**: `docs/user-guide/README.md` — comprehensive guide with annotated screenshots
- **Administration Guide**: `docs/administration/README.md` — admin panel, email config, security, terminal management

### Changed
- Phase 11 status: PLANNED → COMPLETE

## [Unreleased] — Phase 10: Security Features & LDAP Integration (COMPLETE)

### Added
- **V8 Flyway migration**: TOTP columns on users table (`totp_secret`, `totp_enabled`), `password_reset_tokens` table, LDAP configuration settings in `system_settings`
- **TOTP 2FA**: `TotpService` with RFC 6238 TOTP generation/verification (base32, HmacSHA1, ±1 time step window); endpoints `POST /auth/totp/setup`, `/totp/enable`, `/totp/disable`
- **Password reset flow**: `PasswordResetTokenEntity` + `PasswordResetTokenRepository`; `PasswordResetService` with email-based token flow; endpoints `POST /auth/password/reset-request`, `/auth/password/reset-confirm` (public, unauthenticated)
- **LDAP/Active Directory configuration**: `LdapService` for reading/updating LDAP settings from system_settings; endpoints `GET /admin/ldap`, `PUT /admin/ldap`
- **Self-service profile update**: `PUT /users/me` endpoint for users to update own preferences (name, phone, date/time format)
- **Recursive subordinate listing**: `GET /users/{id}/all-subordinates` — returns full hierarchy of subordinates for a manager
- **Password confirmation validation**: `confirmPassword` field added to `ChangePasswordRequest`, `ResetPasswordRequest`, and new `PasswordResetConfirmRequest`
- **TOTP in login flow**: `totpCode` field added to `LoginRequest`; `AuthService.login()` verifies TOTP code when enabled
- **New DTOs**: `TotpSetupResponse`, `TotpVerifyRequest`, `PasswordResetLinkRequest`, `PasswordResetConfirmRequest`, `LdapConfigResponse`, `UpdateLdapConfigRequest`
- **UserResponse extended**: `totpEnabled` field added to `UserResponse`
- **UpdateUserRequest extended**: `employeeNumber` field added for admin updates
- **SecurityConfig**: password reset endpoints added to public paths
- **Frontend: UserSettingsPage** (`/settings`): display preferences and password change with confirmation
- **Frontend: PasswordResetRequestPage** (`/forgot-password`): request password reset email
- **Frontend: PasswordResetConfirmPage** (`/reset-password`): confirm password reset with token
- **Frontend: LoginPage TOTP**: TOTP code input shown on 401 TOTP-required; forgot password link
- **Frontend: AdminPage**: manager dropdown, always-visible employee number, confirm password in reset modal
- **Frontend: AuthContext**: `refreshUser` method; Settings nav link in sidebar
- **Frontend: Services**: LDAP config, updateOwnProfile, changeOwnPassword, TOTP, password reset APIs
- **Frontend: i18n**: auth reset, TOTP, settings, nav keys for DE and EN
- **Manager substitute feature**: V9 Flyway migration (substitute_id), UserEntity, DTOs, access check updates in TimeTrackingService & VacationService, UserService substitute helpers, Admin UI dropdown
- **TOTP 2FA QR code**: `qrcode.react` dependency for rendering QR codes in UserSettingsPage
- **LDAP admin tab**: New tab in AdminPage with full LDAP/AD configuration form (17 fields)
- **Backend unit tests**: TotpServiceTest (8), PasswordResetServiceTest (7), LdapServiceTest (5), EmailServiceTest (5) = 25 new tests
- **i18n**: TOTP, LDAP, and substitute keys in both DE and EN

### Fixed
- **Test email 500 error**: `sendTestMail()` now checks `mailEnabled` flag before attempting to send; returns 400 (not 500) for configuration errors
- **Docker Compose missing MAIL_* env vars**: All `MAIL_*`, `SEED_ADMIN_PASSWORD`, and `FRONTEND_URL` environment variables are now passed to the backend container from `.env`
- **Dev profile mail config**: Changed hardcoded SMTP values to env var placeholders so MAIL_HOST, MAIL_PORT etc. work in development
- **SMTP timeouts**: Added 5-second connection/read/write timeouts to prevent hanging connections
- **Frontend error display**: Test mail errors now show the actual server error message instead of generic "Request failed"

## [Unreleased] — Phase 9: Testing & Security Hardening (COMPLETE)

### Fixed
- **CORS**: Changed `allowedOrigins` to `allowedOriginPatterns` in SecurityConfig, fixing "Invalid CORS request" on localhost
- **Date localization**: Dates now display in locale-correct format (DD.MM.YYYY for German) instead of US format; calendar uses Monday as first day of week for German locale
- **JwtAuthenticationFilter**: Added `/terminal/` to publicPaths for consistency with SecurityConfig permitAll rules
- **V7 migration**: Fixed column names from `setting_key`/`setting_value` to `key`/`value` matching V5 schema; fixed `ON CONFLICT` clause to target `(key)` column
- **NavLink active state**: Added `end` prop to `/vacation` NavLink so it no longer highlights when on `/vacation/approvals`
- **ktlint CI**: Auto-formatted `AuthControllerIntegrationTest.kt` to pass ktlint checks
- **E2E mock API paths**: Corrected all E2E test mocks from wrong `/api/time-tracking/` to correct `/api/time/` matching actual frontend service endpoints

### Added
- **V7 migration**: `date_format` and `time_format` columns on users table; system settings for `display.date_format`, `display.time_format`, `display.first_day_of_week`
- **Per-user date/time preferences**: UserEntity, UserResponse, UpdateUserRequest extended with dateFormat/timeFormat; falls back to global system settings
- **`dateUtils.ts`**: locale-aware date formatting utility using date-fns (formatDate, formatTime, formatDateTime, formatMonthYear, getFirstDayOfWeek, getWeekdayHeaders)
- **`DateFormatContext`**: React context providing user's date/time format preferences to all components
- **AuthControllerIntegrationTest**: 10 integration tests (CORS preflight, credentials, auth login/logout, security headers)
- Frontend unit tests for `dateUtils.ts` (18 tests)
- **Playwright E2E test suite** (7 spec files, 62+ tests):
  - `login.spec.ts`: 12 tests — form rendering, language toggle, validation, login flows
  - `navigation.spec.ts`: 6 tests — sidebar, route navigation, protected routes, logout
  - `dashboard.spec.ts`: 8 tests — widgets, status banners, weekly summary, error handling
  - `time-tracking.spec.ts`: 14 tests — clock states, buttons, timesheet, CSV export, errors
  - `vacation.spec.ts`: 8 tests — balance card, requests tab, new request form, calendar, errors
  - `vacation-approval.spec.ts`: 6 tests — pending requests, approve/reject, reject modal, empty state, errors
  - `admin.spec.ts`: 8 tests — user list, create user, audit log, settings, errors
- E2E testing job in CI workflow (`.github/workflows/ci.yml`)
- Comprehensive installation guide (`docs/installation/README.md`)

### Changed
- Date/calendar localization across the frontend (Dashboard, TimeTracking, Vacation pages)
- Per-user date/time format preferences in backend (V7 migration, UserEntity, DTOs)
- `playwright.config.ts`: screenshot mode set to `on` (always capture for docs)

### Planned
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
