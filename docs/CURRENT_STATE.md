# Current Project State

> Last updated: 2026-02-22

## Quick Summary

Zeiterfassung is a German labor law (ArbZG) compliant time tracking system. **Phases 1–4 are complete.** The backend has working auth, user management, time tracking, and a full vacation management system with email notifications and DSGVO-compliant data isolation. The frontend has login, navigation, time tracking, and vacation pages implemented. Mobile apps and terminal are scaffolded but not fully implemented.

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

### Terminal (Scaffolded Only)
- ⚠️ Rust app compiles with iced GUI, reqwest, rusqlite
- ⚠️ Config parsing works, RFID reader module exists
- ⚠️ API client and buffer modules exist but need backend integration

## Tech Debt / Known Issues
- Dashboard page is fully implemented with real API data
- Time Tracking page is fully implemented with clock-in/out/break controls and monthly timesheet
- Admin page is a placeholder
- Backend test coverage targets (≥90%) not yet fully verified
- Frontend test coverage targets (≥85%) not yet fully verified
- E2E Playwright tests need expansion
- Mobile apps are scaffold-only
- Terminal needs backend API endpoint for RFID-based clock in/out

## Next Steps
1. **Phase 5: Dashboard & Reporting** — replace placeholder dashboard with real data widgets, time sheet reports, CSV export
2. **Phase 8: Admin Panel** — replace admin placeholder with full user/role/settings management UI
3. Continue dependency updates (review and merge Dependabot PRs)
