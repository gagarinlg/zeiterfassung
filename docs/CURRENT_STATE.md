# Current Project State

> Last updated: 2026-03-01

## Quick Summary

Zeiterfassung is a German labor law (ArbZG) compliant time tracking system. **Phases 1–6 are complete.** The backend has working auth, user management, time tracking, vacation management, email notifications, CSV export, and terminal RFID scan endpoint. The terminal Raspberry Pi app is fully implemented with multi-terminal support. The frontend has fully implemented login, navigation, dashboard, time tracking, and vacation pages. Mobile apps are scaffolded.

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
- ✅ Terminal RFID scan: `POST /api/terminal/scan` — looks up user by RFID, toggles clock state, returns summary; transactional to prevent race conditions with multiple terminals
- ✅ Terminal heartbeat: `GET /api/terminal/heartbeat` — connectivity check for terminal devices

### Frontend (Partially Implemented)
- ✅ Login page with validation and error handling
- ✅ Auth context with auto-refresh and permission helpers
- ✅ Protected routes with 403 handling
- ✅ Navigation with role-based menu items
- ✅ Vacation page: balance card, request list, new-request form, monthly calendar
- ✅ Vacation approval page: manager queue with approve/reject-with-reason modal
- ✅ Dashboard: real data widgets (today hours, weekly hours, vacation balance, team status, compliance warnings)
- ✅ Time Tracking page: full implementation (status, clock in/out/break, live timer, today's entries list, monthly timesheet, CSV export)
- ⚠️ Admin page: placeholder

### Mobile (Scaffolded Only)
- ⚠️ Android: navigation + placeholder screens
- ⚠️ iOS: tab view + placeholder views

### Terminal (Fully Functional — Phase 6 Complete)
- ✅ Full iced 0.12.1 Application with complete state machine (Idle/Loading/ClockIn/ClockOut/OfflineConfirm/Error)
- ✅ All 6 screens implemented with colour-coded views (green clock-in / red clock-out / amber offline & error)
- ✅ RFID subscription (async iced subscription via `subscription::channel`, 50 ms polling)
- ✅ Offline SQLite buffering with auto-sync; stale/conflicted events discarded cleanly
- ✅ Audio feedback (rodio 0.22)
- ✅ Multi-terminal support: each terminal has a unique `terminal_id` in `terminal.toml`; `@Transactional` scan prevents race conditions; HTTP 409 shown as "Bitte erneut scannen"
- ✅ 15 unit tests (api, buffer, config)

## Tech Debt / Known Issues
- Admin page is a placeholder (Phase 8)
- Backend test coverage targets (≥90%) not yet fully verified
- Frontend test coverage targets (≥85%) not yet fully verified
- E2E Playwright tests need expansion
- Mobile apps are scaffold-only (Phase 7)

## Next Steps
1. **Phase 7: Mobile Apps** — implement Android and iOS placeholder screens
2. **Phase 8: Admin Panel** — replace admin placeholder with full user/role/settings management UI
3. Continue dependency updates (review and merge Dependabot PRs)
