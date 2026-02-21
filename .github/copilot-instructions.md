# Copilot Instructions for Zeiterfassung

## Project Overview

**Zeiterfassung** is a comprehensive time recording (time tracking) system for employees, built to comply with **German labor law** (Arbeitszeitgesetz – ArbZG). The system consists of three major components:

1. **Web Application** — Administration panel and employee self-service portal (with PostgreSQL database)
2. **Mobile Apps** — Native cell phone apps for Android (Kotlin/Jetpack Compose) and iOS (Swift/SwiftUI) for employee time tracking and vacation requests
3. **Raspberry Pi 5 Terminal** — A kiosk-style terminal application for clocking in/out via NFC/RFID tokens

---

## System Architecture

### Web Application (Backend + Frontend)

- **Backend:** Kotlin with Spring Boot 3+ (latest stable), Spring Security, Spring Data JPA
- **Database:** PostgreSQL (latest stable)
- **Frontend:** React 18+ with TypeScript, Vite, TailwindCSS
- **API:** RESTful with OpenAPI/Swagger documentation
- **Authentication:** JWT-based authentication with refresh tokens, bcrypt password hashing
- **Authorization:** Role-based access control (RBAC) with fine-grained permissions

### Mobile Apps

- **Android:** Kotlin with Jetpack Compose, Material 3, Retrofit, Hilt for DI
- **iOS:** Swift with SwiftUI, Combine, URLSession, Swift Package Manager

### Raspberry Pi 5 Terminal

- **Language:** Python 3.12+ or Rust (prefer Rust for reliability)
- **NFC/RFID:** The RFID reader acts as a USB keyboard device and sends the RFID tag ID followed by a newline character
- **Display:** Minimal UI showing clock-in/clock-out status, employee name, and time
- **Communication:** REST API calls to the web application backend

---

## Security Requirements

### Secure Coding Standards

- Follow OWASP Top 10 guidelines for all components
- Use parameterized queries / prepared statements — NEVER concatenate SQL
- Implement Content Security Policy (CSP) headers
- Enable HTTPS everywhere; redirect HTTP to HTTPS
- Use HSTS (HTTP Strict Transport Security)
- Sanitize and validate ALL user input on both frontend and backend
- Implement rate limiting on authentication endpoints
- Use CSRF protection for all state-changing operations
- Implement proper CORS configuration (whitelist allowed origins)
- Store secrets in environment variables or a secrets manager — NEVER hardcode credentials
- Use secure session management with HttpOnly, Secure, and SameSite cookie attributes
- Implement account lockout after failed login attempts
- Log security-relevant events (login attempts, permission changes, data exports)
- Implement audit trails for all data modifications
- Use dependency scanning (Dependabot, Snyk) for vulnerability detection

### Authentication & Authorization

- JWT tokens with short expiration (15 min access, 7 day refresh)
- Refresh token rotation with reuse detection
- Multi-layer authorization: Frontend (hide UI elements), Middleware (validate requests), Backend (enforce permissions)
- NEVER rely solely on frontend for access control — always enforce on the backend
- Every API endpoint MUST check permissions before executing
- Role hierarchy: Super Admin → Admin → Manager → Employee
- Fine-grained permissions per feature (e.g., `time.edit.own`, `time.edit.team`, `vacation.approve`, `admin.users.manage`)

### Penetration Testing

- Include automated security tests in CI pipeline using OWASP ZAP
- Add SQL injection tests
- Add XSS vulnerability tests
- Add authentication bypass tests
- Add authorization escalation tests
- Add CSRF tests
- Document penetration test procedures in `docs/security/penetration-testing.md`

---

## Testing Requirements

### Unit Tests

- **Backend:** JUnit 5 + Mockito + AssertJ, aim for ≥90% code coverage
- **Frontend:** Vitest + React Testing Library, aim for ≥85% code coverage
- **Android:** JUnit 5 + MockK + Turbine for Flow testing
- **iOS:** XCTest + Swift Testing framework
- **Raspberry Pi:** pytest (Python) or `#[cfg(test)]` modules (Rust)
- Test all business logic, validation rules, permission checks, and edge cases
- Test German labor law compliance calculations thoroughly

### End-to-End (E2E) Tests

- **Web Application:** Playwright with TypeScript
- **Mobile Apps:** Appium or Detox (Android), XCUITest (iOS)
- Test complete user flows: login → clock in → break → clock out → view report
- Test all roles: Admin, Manager, Employee
- Test permission boundaries (ensure forbidden actions are blocked)
- Test vacation request → approval → calendar update flow
- Test CSV import and export flows
- Test NFC terminal clock-in/clock-out flow (via API mocking)

### Screenshot Generation

- Use Playwright to automatically capture screenshots of all major UI screens
- Store screenshots in `docs/screenshots/` organized by feature
- Update screenshots as part of CI when UI changes are detected
- Screenshots must be captured in both German and English locales

---

## CI/CD — GitHub Actions

### Required Workflows

Create the following GitHub Actions workflows in `.github/workflows/`:

1. **`ci.yml`** — Runs on every push and pull request:
   - Lint all code (ESLint, ktlint, SwiftLint, ruff/clippy)
   - Run all unit tests with coverage reporting
   - Run E2E tests with Playwright
   - Build all components
   - Upload test results and coverage reports as artifacts

2. **`security.yml`** — Runs on every push to main and weekly:
   - OWASP ZAP security scan
   - Dependency vulnerability scanning
   - SAST (Static Application Security Testing) with CodeQL
   - Container image scanning (if using Docker)

3. **`build-deploy.yml`** — Runs on tags/releases:
   - Build Docker images for web app
   - Build Android APK/AAB
   - Build iOS IPA (on macOS runner)
   - Build Raspberry Pi binary/package
   - Generate and publish documentation

4. **`docs.yml`** — Runs when docs or UI change:
   - Generate screenshots via Playwright
   - Build documentation site
   - Deploy to GitHub Pages

---

## Internationalization (i18n)

### Requirements

- ALL user-facing strings MUST be externalized — NEVER hardcode UI text
- Support at minimum: **German (de)** and **English (en)**
- German is the PRIMARY language (default locale)
- English is the SECONDARY language

### Implementation

- **Frontend (React):** Use `react-i18next` with JSON translation files in `src/locales/{lang}/`
- **Android:** Use standard `strings.xml` resource files in `res/values/` and `res/values-de/`
- **iOS:** Use `.strings` or `.xcstrings` localization files
- **Backend:** Use Spring MessageSource with `messages_{locale}.properties`
- **Raspberry Pi:** Use gettext (Python) or fluent (Rust) for localization

### Adding New Languages

- Adding a new language MUST only require:
  1. Creating a new translation file (copy from English template)
  2. Translating the strings
  3. Registering the locale in the configuration
- Document the process in `docs/development/adding-languages.md`
- Provide a translation template file with comments explaining context for each string

---

## Code Quality & Readability

### General Principles

- Write self-documenting code with clear, descriptive names
- Follow the Single Responsibility Principle
- Keep functions/methods short (≤30 lines preferred)
- Use meaningful variable and function names in English
- Add comments only for complex business logic — especially German labor law calculations
- Add JSDoc/KDoc/Swift doc comments for all public APIs
- Use consistent code formatting enforced by linters and formatters

### Code Style

- **Kotlin:** Follow Kotlin coding conventions, use ktlint
- **TypeScript/React:** Follow Airbnb style guide, use ESLint + Prettier
- **Swift:** Follow Swift API Design Guidelines, use SwiftLint
- **Python:** Follow PEP 8, use ruff
- **Rust:** Follow Rust API Guidelines, use rustfmt + clippy

### Project Structure

```
zeiterfassung/
├── .github/
│   ├── copilot-instructions.md
│   └── workflows/
│       ├── ci.yml
│       ├── security.yml
│       ├── build-deploy.yml
│       └── docs.yml
├── backend/                    # Spring Boot backend
│   ├── src/main/kotlin/
│   ├── src/main/resources/
│   └── src/test/kotlin/
├── frontend/                   # React frontend
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── locales/
│   │   │   ├── de/
│   │   │   └── en/
│   │   └── utils/
│   ├── e2e/                   # Playwright E2E tests
│   └── public/
├── mobile/
│   ├── android/               # Android app (Kotlin/Jetpack Compose)
│   └── ios/                   # iOS app (Swift/SwiftUI)
├── terminal/                  # Raspberry Pi 5 terminal app
├── docs/
│   ├── installation/
│   ├── administration/
│   ├── user-guide/
│   ├── development/
│   ├── security/
│   ├── api/
│   └── screenshots/
├── docker-compose.yml
├── Dockerfile
└── README.md
```

---

## Feature Specifications

### User Roles & Permissions

| Role | Description |
|------|-------------|
| **Super Admin** | Full system access, can manage all settings, users, and data |
| **Admin** | Can manage users, settings, and view all reports |
| **Manager** | Can approve vacation, correct time entries for their team, view team reports. Managers are also employees and can have a manager above them |
| **Employee** | Can track own time, request vacation, view own data |

- Only show UI elements (buttons, menus, pages) that the current user has permission to access
- Protect every frontend route, every API middleware layer, and every backend endpoint against unauthorized access
- Implement a flexible permission system that allows custom role definitions

### Time Tracking (German Labor Law Compliance)

Implement time tracking that complies with the **Arbeitszeitgesetz (ArbZG)**:

- Maximum 8 hours of work per day (extendable to 10 hours if averaged to 8 over 6 months)
- Mandatory breaks: 30 min after 6 hours, 45 min after 9 hours
- Minimum 11 hours of rest between work days
- No work on Sundays and public holidays (with configurable exceptions)
- Track and warn about overtime
- Calculate and display daily, weekly, and monthly summaries
- Support flexible working hours (Gleitzeit)
- Configurable work hours per employee (full-time, part-time, custom)
- Configurable work days per employee

### Vacation Management

- Configurable vacation days per employee per year
- Vacation request workflow: Employee requests → Manager approves/rejects
- Visual vacation calendar for planning (show team availability)
- Track remaining vacation days
- Support half-day vacations
- Carry-over rules for unused vacation days
- Public holiday calendar (German federal + state-specific holidays, configurable)
- Conflict detection (warn if too many team members are off)

### NFC Terminal (Raspberry Pi 5)

- The RFID reader behaves as a USB keyboard, sending the tag ID followed by a newline
- Each user can have a configurable RFID tag ID stored in their profile
- Terminal displays: current time, employee name (after scan), clock-in/clock-out confirmation
- Offline mode: buffer clock events locally and sync when connection is restored
- Visual and optional audible feedback for successful/failed scans
- Auto-logout display after a few seconds

### Dashboard

- **Employee dashboard:** Today's hours, week summary, remaining vacation days, upcoming holidays, recent activity
- **Manager dashboard:** Team attendance overview, pending vacation requests, overtime alerts, team calendar
- **Admin dashboard:** System statistics, user count, active sessions, recent security events, system health

### CSV Import & Export

- Export time records, vacation data, employee lists
- Import employee data, time corrections (with validation)
- Configurable column mapping for imports
- Data validation with detailed error reporting
- Support for German CSV format (semicolon separator, comma decimal)

### Additional Features (Common for Time Tracking Systems)

- Employee profile management (personal info, RFID tag, working hours config)
- Sick leave tracking
- Home office / remote work tracking
- Project-based time tracking (optional time assignment to projects/cost centers)
- Overtime calculation and compensation tracking
- Monthly/annual reports generation (PDF export)
- Email notifications (vacation request updates, overtime warnings)
- System settings (company info, default working hours, holiday calendar)
- Data backup and restore functionality
- GDPR compliance (data export, data deletion for employees)
- Activity log / audit trail

---

## Documentation Requirements

Maintain comprehensive documentation in the `docs/` directory:

### Installation Guide (`docs/installation/`)
- System requirements
- Step-by-step installation for all components
- Docker Compose setup for quick start
- Database setup and migration
- SSL/TLS certificate configuration
- Raspberry Pi terminal setup (including RFID reader configuration)
- Environment variable reference

### Administration Guide (`docs/administration/`)
- User management
- Role and permission configuration
- System settings
- Backup and restore
- Monitoring and logging
- Troubleshooting guide

### User Guide (`docs/user-guide/`)
- Getting started for employees
- Time tracking guide
- Vacation request guide
- Mobile app guide
- NFC terminal usage

### Developer Guide (`docs/development/`)
- Development environment setup
- Architecture overview
- API documentation
- Adding new languages
- Testing guide
- Contributing guidelines

### Screenshots

- Capture screenshots automatically using Playwright during E2E tests
- Include annotated screenshots in all user-facing documentation
- Organize by feature and language
- Update automatically in CI when UI changes

---

## Database Considerations

- Use Flyway or Liquibase for database migrations
- Never modify existing migrations — always create new ones
- Add indexes for frequently queried columns
- Use UUIDs as primary keys for all entities
- Implement soft delete for user data (GDPR compliance)
- Add database-level constraints in addition to application-level validation
- Regular automated backups

---

## Environment & Configuration

- Use environment variables for all configuration
- Provide `.env.example` with documented defaults
- Support profiles: `development`, `test`, `production`
- Docker Compose for local development with all dependencies
- Separate configuration for each component

---

## Summary of Key Principles

1. **Security first** — Every feature must consider security implications
2. **Test everything** — Unit tests, E2E tests, security tests
3. **German law compliance** — Time tracking must comply with ArbZG
4. **Bilingual** — All UI in German and English, easy to add more languages
5. **Human readable** — Clean, well-documented, maintainable code
6. **Documentation** — Keep docs up to date with screenshots
7. **CI/CD** — Automate everything via GitHub Actions
8. **Permission enforcement** — Frontend, middleware, AND backend must all enforce access control