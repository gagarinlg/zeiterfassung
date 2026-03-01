# Changelog

All notable changes to the Zeiterfassung project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Planned
- Phase 5 (continued): Charts, admin dashboard, reports page
- Phase 6: Terminal (Raspberry Pi) Full Integration
- Phase 7: Mobile Apps Full Implementation

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
