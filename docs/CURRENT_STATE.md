# Current Project State

> Last updated: 2026-02-21

## Quick Summary

Zeiterfassung is a German labor law (ArbZG) compliant time tracking system. **Phases 1–3 are complete.** The backend has working auth, user management, and time tracking. The frontend has login and basic navigation but most pages are still placeholders. Mobile apps and terminal are scaffolded but not fully implemented.

## What Works Right Now

### Backend (Fully Functional)
- ✅ JWT authentication with refresh token rotation and reuse detection
- ✅ User CRUD with RBAC (SUPER_ADMIN, ADMIN, MANAGER, EMPLOYEE)
- ✅ Time tracking: clock-in/out, breaks, daily summaries, compliance checks
- ✅ ArbZG compliance validation (10h max, mandatory breaks, 11h rest)
- ✅ Audit logging for all significant actions
- ✅ Manager endpoints for team management
- ✅ Employee configuration management

### Frontend (Partially Implemented)
- ✅ Login page with validation and error handling
- ✅ Auth context with auto-refresh and permission helpers
- ✅ Protected routes with 403 handling
- ✅ Navigation with role-based menu items
- ⚠️ Dashboard: placeholder (shows static data)
- ⚠️ Time Tracking page: placeholder (shows title only)
- ⚠️ Vacation page: placeholder (shows title only)
- ⚠️ Admin page: placeholder

### Mobile (Scaffolded Only)
- ⚠️ Android: navigation + placeholder screens
- ⚠️ iOS: tab view + placeholder views

### Terminal (Scaffolded Only)
- ⚠️ Rust app compiles with iced GUI, reqwest, rusqlite
- ⚠️ Config parsing works, RFID reader module exists
- ⚠️ API client and buffer modules exist but need backend integration

## Tech Debt / Known Issues
- Frontend pages are mostly placeholders — need real implementations
- Backend tests exist but coverage targets (≥90%) not yet verified
- Frontend tests exist but coverage targets (≥85%) not yet verified
- E2E Playwright tests need expansion
- Mobile apps are scaffold-only
- Terminal needs backend API endpoint for RFID-based clock in/out
- Dependabot has 30+ open PRs for dependency updates

## Next Steps
1. **Phase 4: Vacation Management** — highest priority
2. **Phase 5: Dashboard & Reporting** — make the UI useful
3. Continue dependency updates (review and merge Dependabot PRs)
