# Current Project State

> Last updated: 2025-07-14

## Quick Summary

Zeiterfassung is a German labor law (ArbZG) compliant time tracking system. **Phases 1–12 are complete.** Phase 13 (Database Backup & Restore) is in progress. The backend has working auth, user management, time tracking, vacation management, email notifications, CSV export, terminal RFID scan endpoint, admin endpoints, TOTP 2FA, password reset flow, LDAP configuration, manager substitute/deputy feature, Spring caching, full OpenAPI/Swagger documentation, and database backup/restore system. The terminal Raspberry Pi app is fully implemented. The frontend has fully implemented login, navigation, dashboard, time tracking, vacation, and admin pages with proper date/calendar localization, WCAG 2.1 AA accessibility, and 59 unit tests (including dateUtils). Mobile apps are fully implemented (Android and iOS) with real API integration and ViewModel unit tests. CI includes E2E testing with Playwright across Chromium, Firefox, and WebKit. Documentation includes user guide, administration guide, 12 auto-generated screenshots, 5 architecture decision records, comprehensive installation guides for mobile apps and terminal, MDM provisioning documentation, and a full testing guide.

## What Works Right Now

### Backend (Fully Functional)
- ✅ JWT authentication with refresh token rotation and reuse detection
- ✅ User CRUD with RBAC (SUPER_ADMIN, ADMIN, MANAGER, EMPLOYEE)
- ✅ Time tracking: clock-in/out, breaks, daily summaries, ArbZG compliance checks
- ✅ ArbZG compliance validation (10h max, mandatory breaks, 11h rest)
- ✅ Audit logging for all significant actions
- ✅ Manager endpoints for team management
- ✅ Employee configuration management
- ✅ Vacation requests: create, update, cancel (PENDING/APPROVED), approve, reject
- ✅ Vacation balance management: annual balance, carry-over (capped), remaining days
- ✅ Working-day calculation: excludes weekends, public holidays, per-employee work_days; half-day support
- ✅ Public holidays: German federal holidays seeded; state-specific support
- ✅ Email notifications: vacation lifecycle events (created/approved/rejected/cancelled)
- ✅ Monthly email reports: personal hours + vacation balance to employees; team summary to managers
- ✅ DSGVO enforcement: employees cannot access other employees' time or vacation data
- ✅ Terminal RFID scan: `POST /api/terminal/scan` — looks up user by RFID, toggles clock state; transactional with 409 on race condition
- ✅ Terminal heartbeat: `GET /api/terminal/heartbeat`
- ✅ Admin audit log: `GET /api/admin/audit-log` (paginated, filterable by userId)
- ✅ Admin system settings: `GET /api/admin/settings`, `PUT /api/admin/settings/{key}`
- ✅ Per-user date/time format preferences with system-wide defaults
- ✅ CORS: `allowedOriginPatterns` for proper localhost + credential handling
- ✅ TOTP 2FA: setup, enable, disable, verification during login
- ✅ Password reset: token-based flow with email notifications
- ✅ LDAP/AD configuration: read/update via admin endpoints
- ✅ Self-service profile update: `PUT /api/users/me`
- ✅ Recursive subordinate listing: `GET /api/users/{id}/all-subordinates`
- ✅ Manager substitute/deputy feature: configurable substitute with same rights for subordinates
- ✅ Test email fix: proper mailEnabled check, dev profile uses env vars, SMTP timeouts
- ✅ Spring caching: `CacheConfig.kt` with `ConcurrentMapCacheManager` for `publicHolidays` and `systemSettings` caches; `@Cacheable` on `VacationService.getPublicHolidays()` and `AdminService.getSystemSettings()`; `@CacheEvict` on `AdminService.updateSystemSetting()`
- ✅ OpenAPI/Swagger: all 7 controllers annotated with `@Tag`, `@Operation`, `@ApiResponse`, `@SecurityRequirement`; `OpenApiConfig.kt` with API metadata and JWT security scheme
- ✅ Database backup/restore: `BackupService` with scheduled daily backups, configurable retention, `BackupController` at `/admin/backups` with list/create/download/restore/upload/delete endpoints, audit logging, path traversal protection

### Frontend (Fully Implemented)
- ✅ Login page with validation and error handling
- ✅ Auth context with auto-refresh and permission helpers
- ✅ Protected routes with 403 handling
- ✅ Navigation with role-based menu items and correct active state highlighting
- ✅ Vacation page: balance card, request list, new-request form, monthly calendar
- ✅ Vacation approval page: manager queue with approve/reject-with-reason modal
- ✅ Dashboard: real data widgets (today hours, weekly hours, vacation balance, team status, compliance warnings)
- ✅ Time Tracking page: full implementation (status, clock in/out/break, live timer, today's entries list, monthly timesheet, CSV export)
- ✅ Admin page: 3-tab UI (User Management, Audit Log, System Settings) with full CRUD, search, modals
- ✅ Date/calendar localization: `dateUtils.ts` utility with `formatDate`, `formatTime`, `formatDateTime`, `formatMonthYear` using date-fns locales; `DateFormatContext` for per-user date/time format preferences; all pages (Dashboard, TimeTracking, Vacation) use localized date formatting
- ✅ User Settings page: display preferences (date/time format) and password change with confirmation
- ✅ Password reset flow: request page and confirm page with token
- ✅ TOTP 2FA support on login page (shown after TOTP-required error)
- ✅ Admin UserModal: manager assignment dropdown, always-visible employee number
- ✅ Admin ResetPasswordModal: confirm password field
- ✅ Forgot password link on login page
- ✅ Settings nav link in sidebar
- ✅ Unit tests: 59 tests total (LoginPage, AuthContext, ProtectedRoute, AdminPage, dateUtils)
- ✅ E2E tests: 62+ Playwright tests covering all 7 pages (login, navigation, dashboard, time-tracking, vacation, vacation-approval, admin)
- ✅ CI: E2E testing with Playwright (Chromium) integrated into GitHub Actions workflow
- ✅ TOTP 2FA setup with QR code in UserSettingsPage
- ✅ LDAP configuration tab in AdminPage
- ✅ Manager substitute dropdown in user management
- ✅ WCAG 2.1 AA: skip-to-content link, ARIA landmarks, aria-live loading states, focus-visible utility
- ✅ 25 new backend unit tests (TotpService, PasswordResetService, LdapService, EmailService)
- ✅ 8 additional backend unit tests (TimeTrackingService break gap detection, VacationService setBalance/triggerCarryOver, NotificationService email dispatch)
- ✅ Playwright screenshot generation (12 screenshots)

### Mobile (Fully Implemented)
- ✅ **Android**: real API integration with Retrofit + Moshi + Hilt; LoginScreen, DashboardScreen, TimeTrackingScreen (state-aware buttons), VacationScreen (balance + requests); ServerSettingsScreen with configurable server URL (DataStore + MDM managed config support); 24 ViewModel unit tests
- ✅ **iOS**: real API integration using URLSession; Keychain token storage (replaces UserDefaults); DashboardView, TimeTrackingView (action buttons, elapsed timer), VacationView (balance breakdown + request list); AuthViewModel, DashboardViewModel, TimeTrackingViewModel, VacationViewModel; ServerConfigManager with MDM + UserDefaults + default URL priority; ServerSettingsView with gear menu in MainTabView

### Terminal (Fully Functional — Phase 6 Complete)
- ✅ Full iced 0.12.1 Application with complete state machine (Idle/Loading/ClockIn/ClockOut/OfflineConfirm/Error)
- ✅ All 6 screens implemented with colour-coded views
- ✅ RFID subscription, offline SQLite buffering, audio feedback
- ✅ Multi-terminal support with HTTP 409 race condition protection
- ✅ 15 unit tests (api, buffer, config)

### Documentation
- ✅ User guide, administration guide
- ✅ 12 auto-generated screenshots
- ✅ 5 architecture decision records
- ✅ Comprehensive mobile app installation & deployment guide (`docs/installation/mobile-apps.md`)
- ✅ MDM provisioning guide with examples for Intune, Jamf, Workspace ONE, Google Workspace (`docs/installation/mobile-provisioning.md`)
- ✅ Raspberry Pi terminal full installation guide with systemd, kiosk mode, offline buffering (`docs/installation/terminal.md`)
- ✅ Testing guide covering all components: backend, frontend, terminal, mobile, E2E, CI/CD (`docs/development/testing.md`)

## Tech Debt / Known Issues
- Backend test coverage targets (≥90%) not yet fully verified for all services
- Android: push notifications, biometric auth, offline caching — not yet implemented
- iOS: push notifications, Face ID / Touch ID — not yet implemented

## Next Steps
1. Continue dependency updates (review and merge Dependabot PRs)
2. Increase backend test coverage toward ≥90% target
3. Mobile: push notifications, biometric auth, offline caching

