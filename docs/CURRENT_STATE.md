# Current Project State

> Last updated: 2026-03-01

## Quick Summary

Zeiterfassung is a German labor law (ArbZG) compliant time tracking system. **Phases 1–8 are complete.** The backend has working auth, user management, time tracking, vacation management, email notifications, CSV export, terminal RFID scan endpoint, and admin endpoints (audit log + system settings). The terminal Raspberry Pi app is fully implemented. The frontend has fully implemented login, navigation, dashboard, time tracking, vacation, and admin pages. Mobile apps are fully implemented (Android and iOS) with real API integration and ViewModel unit tests.

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

### Frontend (Fully Implemented)
- ✅ Login page with validation and error handling
- ✅ Auth context with auto-refresh and permission helpers
- ✅ Protected routes with 403 handling
- ✅ Navigation with role-based menu items
- ✅ Vacation page: balance card, request list, new-request form, monthly calendar
- ✅ Vacation approval page: manager queue with approve/reject-with-reason modal
- ✅ Dashboard: real data widgets (today hours, weekly hours, vacation balance, team status, compliance warnings)
- ✅ Time Tracking page: full implementation (status, clock in/out/break, live timer, today's entries list, monthly timesheet, CSV export)
- ✅ Admin page: 3-tab UI (User Management, Audit Log, System Settings) with full CRUD, search, modals

### Mobile (Fully Implemented)
- ✅ **Android**: real API integration with Retrofit + Moshi + Hilt; LoginScreen, DashboardScreen, TimeTrackingScreen (state-aware buttons), VacationScreen (balance + requests); 24 ViewModel unit tests
- ✅ **iOS**: real API integration using URLSession; Keychain token storage (replaces UserDefaults); DashboardView, TimeTrackingView (action buttons, elapsed timer), VacationView (balance breakdown + request list); AuthViewModel, DashboardViewModel, TimeTrackingViewModel, VacationViewModel

### Terminal (Fully Functional — Phase 6 Complete)
- ✅ Full iced 0.12.1 Application with complete state machine (Idle/Loading/ClockIn/ClockOut/OfflineConfirm/Error)
- ✅ All 6 screens implemented with colour-coded views
- ✅ RFID subscription, offline SQLite buffering, audio feedback
- ✅ Multi-terminal support with HTTP 409 race condition protection
- ✅ 15 unit tests (api, buffer, config)

## Tech Debt / Known Issues
- Backend test coverage targets (≥90%) not yet fully verified for all services
- E2E Playwright tests need expansion (Phase 9)
- Android: push notifications, biometric auth, offline caching — not yet implemented
- iOS: push notifications, Face ID / Touch ID — not yet implemented

## Next Steps
1. **Phase 9: Testing & Security Hardening** — E2E Playwright tests, penetration testing, OWASP ZAP scans, coverage enforcement
2. **Phase 10: Documentation & Polish** — Playwright screenshots, full user docs, API reference
3. Continue dependency updates (review and merge Dependabot PRs)

