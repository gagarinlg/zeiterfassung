# Testing Guide

This guide covers the testing strategy, tools, and procedures for all components of the Zeiterfassung system.

---

## Table of Contents

- [Overview](#overview)
- [Backend Testing](#backend-testing)
  - [Unit Tests (JUnit 5 + Mockito)](#backend-unit-tests)
  - [Integration Tests](#backend-integration-tests)
  - [Running Tests](#backend-running-tests)
  - [Coverage](#backend-coverage)
- [Frontend Testing](#frontend-testing)
  - [Unit Tests (Vitest + React Testing Library)](#frontend-unit-tests)
  - [End-to-End Tests (Playwright)](#frontend-e2e-tests)
  - [Running Tests](#frontend-running-tests)
  - [Coverage](#frontend-coverage)
- [Terminal Testing](#terminal-testing)
  - [Unit Tests (cargo test)](#terminal-unit-tests)
  - [Integration Tests with Mock HTTP Server](#terminal-integration-tests)
  - [Manual Testing with Virtual RFID Input](#terminal-manual-testing)
  - [GUI Testing](#terminal-gui-testing)
  - [Offline Buffer Testing](#terminal-offline-buffer-testing)
- [Mobile App Testing](#mobile-app-testing)
  - [Android Testing](#android-testing)
  - [iOS Testing](#ios-testing)
  - [Integration Testing with Mock Server](#mobile-integration-testing)
  - [Device Testing Matrix](#device-testing-matrix)
- [End-to-End Testing Overview](#e2e-overview)
- [CI/CD Integration](#cicd-integration)
- [Coverage Reporting](#coverage-reporting)

---

## Overview

| Component | Framework | Target Coverage | Test Types |
|-----------|-----------|----------------|------------|
| Backend   | JUnit 5 + Mockito + AssertJ | ≥ 90% | Unit, Integration |
| Frontend  | Vitest + React Testing Library | ≥ 85% | Unit, Component |
| Frontend E2E | Playwright | Critical paths | End-to-End |
| Terminal  | `cargo test` + mockito (Rust) | ≥ 80% | Unit, Integration |
| Android   | JUnit 5 + MockK + Compose Test | ≥ 80% | Unit, UI |
| iOS       | XCTest + Swift Testing | ≥ 80% | Unit, UI |

### Testing Principles

1. **Test behavior, not implementation** — focus on what the code does, not how it does it.
2. **Fast feedback** — unit tests should run in seconds, not minutes.
3. **Deterministic** — tests must produce the same result every time.
4. **Isolated** — tests should not depend on external services, databases, or network.
5. **German labor law compliance** — thoroughly test all ArbZG calculations (breaks, overtime, rest periods).

---

## Backend Testing

<a id="backend-unit-tests"></a>

### Unit Tests (JUnit 5 + Mockito)

Backend unit tests use JUnit 5 with Mockito for mocking and AssertJ for assertions.

#### Test Structure

Tests are located in `backend/src/test/kotlin/` mirroring the main source structure:

```
backend/src/test/kotlin/com/zeiterfassung/
├── controller/
│   ├── AuthControllerTest.kt
│   ├── TimeEntryControllerTest.kt
│   └── VacationControllerTest.kt
├── service/
│   ├── TimeEntryServiceTest.kt
│   ├── VacationServiceTest.kt
│   └── BreakComplianceServiceTest.kt
└── security/
    └── JwtServiceTest.kt
```

#### Writing a Unit Test

```kotlin
@ExtendWith(MockitoExtension::class)
class TimeEntryServiceTest {

    @Mock
    private lateinit var timeEntryRepository: TimeEntryRepository

    @InjectMocks
    private lateinit var timeEntryService: TimeEntryService

    @Test
    fun `should calculate work duration excluding breaks`() {
        // Given
        val entry = TimeEntry(
            clockIn = LocalDateTime.of(2024, 1, 15, 8, 0),
            clockOut = LocalDateTime.of(2024, 1, 15, 17, 0),
            breakMinutes = 30
        )

        // When
        val duration = timeEntryService.calculateWorkDuration(entry)

        // Then
        assertThat(duration).isEqualTo(Duration.ofHours(8).plusMinutes(30))
    }

    @Test
    fun `should enforce ArbZG 30-minute break after 6 hours`() {
        // Given — employee worked 6h 45m with no break
        val entry = TimeEntry(
            clockIn = LocalDateTime.of(2024, 1, 15, 8, 0),
            clockOut = LocalDateTime.of(2024, 1, 15, 14, 45),
            breakMinutes = 0
        )

        // When
        val result = timeEntryService.applyBreakCompliance(entry)

        // Then — 30 min should be deducted
        assertThat(result.effectiveWorkMinutes).isEqualTo(375) // 6h 15m
        assertThat(result.deductedBreakMinutes).isEqualTo(30)
        assertThat(result.isCompliant).isTrue()
    }
}
```

<a id="backend-integration-tests"></a>

### Integration Tests

Integration tests use `@SpringBootTest` with an embedded H2 or Testcontainers PostgreSQL database:

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TimeEntryControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 401 for unauthenticated request`() {
        mockMvc.perform(get("/api/time-entries"))
            .andExpect(status().isUnauthorized)
    }
}
```

<a id="backend-running-tests"></a>

### Running Tests

```bash
cd backend

# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.zeiterfassung.service.TimeEntryServiceTest"

# Run tests with verbose output
./gradlew test --info

# Run tests and generate coverage report
./gradlew test jacocoTestReport
```

<a id="backend-coverage"></a>

### Coverage

- **Target:** ≥ 90% line coverage
- **Tool:** JaCoCo
- **Report:** `backend/build/reports/jacoco/test/html/index.html`

---

## Frontend Testing

<a id="frontend-unit-tests"></a>

### Unit Tests (Vitest + React Testing Library)

Frontend tests are located alongside the source files or in `__tests__/` directories:

```
frontend/src/
├── components/
│   ├── TimeEntry.tsx
│   └── __tests__/
│       └── TimeEntry.test.tsx
├── hooks/
│   ├── useAuth.ts
│   └── __tests__/
│       └── useAuth.test.ts
└── utils/
    ├── dateUtils.ts
    └── __tests__/
        └── dateUtils.test.ts
```

#### Writing a Component Test

```typescript
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { TimeEntry } from '../TimeEntry';

describe('TimeEntry', () => {
  it('should display clock-in time', () => {
    render(<TimeEntry clockIn="2024-01-15T08:00:00" />);
    expect(screen.getByText('08:00')).toBeInTheDocument();
  });

  it('should call onClockOut when button is clicked', async () => {
    const onClockOut = vi.fn();
    render(<TimeEntry clockIn="2024-01-15T08:00:00" onClockOut={onClockOut} />);

    fireEvent.click(screen.getByRole('button', { name: /clock out/i }));
    expect(onClockOut).toHaveBeenCalledOnce();
  });
});
```

<a id="frontend-e2e-tests"></a>

### End-to-End Tests (Playwright)

E2E tests are located in `frontend/e2e/` and test complete user flows:

```
frontend/e2e/
├── auth.spec.ts         — Login, logout, password reset
├── time-tracking.spec.ts — Clock in/out, breaks, timesheet
├── vacation.spec.ts     — Request, approve, reject vacations
├── admin.spec.ts        — User management, settings
└── accessibility.spec.ts — WCAG 2.1 AA checks
```

#### Writing an E2E Test

```typescript
import { test, expect } from '@playwright/test';

test.describe('Time Tracking', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'employee@test.com');
    await page.fill('[name="password"]', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/dashboard');
  });

  test('should clock in and show confirmation', async ({ page }) => {
    await page.click('button:has-text("Clock In")');
    await expect(page.locator('.status')).toContainText('Clocked In');
  });
});
```

<a id="frontend-running-tests"></a>

### Running Tests

```bash
cd frontend

# Run unit tests
npm test

# Run unit tests in watch mode
npm run test:watch

# Run unit tests with coverage
npm run test:coverage

# Run E2E tests
npx playwright test

# Run E2E tests with UI (headed mode)
npx playwright test --headed

# Run a specific E2E test file
npx playwright test e2e/auth.spec.ts

# Open Playwright report
npx playwright show-report
```

<a id="frontend-coverage"></a>

### Coverage

- **Target:** ≥ 85% line coverage (unit tests)
- **Tool:** Vitest with v8 coverage provider
- **Report:** `frontend/coverage/index.html`

---

## Terminal Testing

The terminal is a Rust application. Tests are written using Rust's built-in test framework with `#[cfg(test)]` modules inside each source file.

<a id="terminal-unit-tests"></a>

### Unit Tests (`cargo test`)

Each module contains inline tests. The existing test modules cover:

| Module | File | Tests |
|--------|------|-------|
| Configuration | `src/config.rs` | Default values, TOML parsing, resolution parsing, invalid input fallback |
| Event Buffer | `src/buffer/mod.rs` | Push/pop, pending count, mark synced, max size enforcement, FIFO ordering |
| RFID Reader | `src/rfid/mod.rs` | Tag reading, debounce behavior |
| API Client | `src/api/mod.rs` | Request building, response parsing |
| Audio | `src/audio/mod.rs` | Sound playback configuration |
| UI | `src/ui/mod.rs` | Screen state transitions |

#### Running Terminal Unit Tests

```bash
cd terminal

# Run all tests
cargo test

# Run tests with output (see println! in tests)
cargo test -- --nocapture

# Run tests for a specific module
cargo test config::tests
cargo test buffer::tests

# Run a single test
cargo test test_push_and_pending_count
```

#### Example: Buffer Module Tests

The buffer module has comprehensive tests covering the offline event queue:

```rust
#[cfg(test)]
mod tests {
    use super::*;

    fn make_buffer() -> EventBuffer {
        // Uses in-memory SQLite for fast, isolated tests
        EventBuffer::new(":memory:", 10).expect("failed to create in-memory buffer")
    }

    #[test]
    fn test_push_and_pending_count() {
        let buf = make_buffer();
        assert_eq!(buf.pending_count().unwrap(), 0);

        buf.push("TAG001", "terminal-1").unwrap();
        buf.push("TAG002", "terminal-1").unwrap();
        assert_eq!(buf.pending_count().unwrap(), 2);
    }

    #[test]
    fn test_max_size_enforcement() {
        let buf = EventBuffer::new(":memory:", 3).expect("failed");

        buf.push("TAG001", "terminal-1").unwrap();
        buf.push("TAG002", "terminal-1").unwrap();
        buf.push("TAG003", "terminal-1").unwrap();
        buf.push("TAG004", "terminal-1").unwrap(); // drops TAG001

        assert_eq!(buf.pending_count().unwrap(), 3);
        let pending = buf.get_pending().unwrap();
        let tags: Vec<&str> = pending.iter().map(|e| e.rfid_tag_id.as_str()).collect();
        assert!(!tags.contains(&"TAG001"));
        assert!(tags.contains(&"TAG004"));
    }
}
```

<a id="terminal-integration-tests"></a>

### Integration Tests with Mock HTTP Server

For testing the terminal's API client against a realistic HTTP server, use a mock server:

```rust
#[cfg(test)]
mod integration_tests {
    use super::*;
    use mockito::Server;

    #[tokio::test]
    async fn test_clock_in_success() {
        let mut server = Server::new_async().await;

        let mock = server
            .mock("POST", "/api/terminal/clock")
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(r#"{
                "employee_name": "Max Mustermann",
                "action": "CLOCK_IN",
                "timestamp": "2024-01-15T08:00:00Z",
                "today_hours": "0:00",
                "weekly_hours": "32:00",
                "overtime_balance": "+2:30",
                "vacation_days_remaining": 15
            }"#)
            .create_async()
            .await;

        let api = ApiClient::new(&server.url(), 10, 1, "terminal-01");
        let result = api.clock("TAG001").await;

        assert!(result.is_ok());
        let response = result.unwrap();
        assert_eq!(response.employee_name, "Max Mustermann");
        assert_eq!(response.action, "CLOCK_IN");

        mock.assert_async().await;
    }

    #[tokio::test]
    async fn test_server_unavailable_triggers_buffering() {
        // No mock server — connection will fail
        let api = ApiClient::new("http://127.0.0.1:1", 1, 1, "terminal-01");
        let result = api.clock("TAG001").await;

        assert!(result.is_err());
        // The calling code should buffer this event
    }
}
```

Add `mockito` as a dev dependency in `Cargo.toml`:

```toml
[dev-dependencies]
mockito = "1"
```

<a id="terminal-manual-testing"></a>

### Manual Testing with Virtual RFID Input

Since the RFID reader acts as a keyboard, you can simulate scans by typing tag IDs via stdin:

#### Method 1: Direct stdin

```bash
# Run the terminal (without cage for testing)
cargo run

# In the terminal's stdin, type a tag ID and press Enter:
# TAG001
# TAG002
```

#### Method 2: Pipe from a Script

```bash
# Simulate multiple scans with delays
(echo "TAG001"; sleep 3; echo "TAG001"; sleep 1; echo "TAG002") | cargo run
```

#### Method 3: Named Pipe (FIFO)

```bash
# Create a named pipe
mkfifo /tmp/rfid_input

# Run the terminal reading from the pipe
cargo run < /tmp/rfid_input &

# Send scans from another terminal
echo "TAG001" > /tmp/rfid_input
sleep 2
echo "TAG002" > /tmp/rfid_input

# Clean up
rm /tmp/rfid_input
```

#### Method 4: Virtual Keyboard Device (Linux)

For a more realistic test, create a virtual input device using `evemu` or `python-evdev`:

```python
#!/usr/bin/env python3
"""Simulate an RFID reader by creating a virtual keyboard device."""
import time
import evdev
from evdev import UInput, ecodes

ui = UInput()
time.sleep(1)

def type_string(s):
    for char in s:
        keycode = getattr(ecodes, f"KEY_{char.upper()}", None)
        if keycode:
            ui.write(ecodes.EV_KEY, keycode, 1)  # key down
            ui.write(ecodes.EV_KEY, keycode, 0)  # key up
            ui.syn()
    # Press Enter
    ui.write(ecodes.EV_KEY, ecodes.KEY_ENTER, 1)
    ui.write(ecodes.EV_KEY, ecodes.KEY_ENTER, 0)
    ui.syn()

type_string("TAG001")
time.sleep(3)
type_string("TAG002")

ui.close()
```

<a id="terminal-gui-testing"></a>

### GUI Testing

GUI testing for the `iced` framework is primarily done through:

1. **State machine testing** — test screen transitions without rendering:

```rust
#[cfg(test)]
mod gui_tests {
    use super::*;

    #[test]
    fn test_idle_to_clock_in_transition() {
        let mut state = AppState::Idle;

        // Simulate successful clock-in
        state = state.handle_scan_result(ScanResult::ClockIn {
            employee_name: "Max Mustermann".into(),
            timestamp: Utc::now(),
        });

        assert!(matches!(state, AppState::ClockInConfirmation { .. }));
    }

    #[test]
    fn test_error_screen_on_unknown_badge() {
        let mut state = AppState::Idle;

        state = state.handle_scan_result(ScanResult::Error {
            message: "Badge not recognized".into(),
        });

        assert!(matches!(state, AppState::Error { .. }));
    }

    #[test]
    fn test_auto_return_to_idle_after_timeout() {
        let state = AppState::ClockInConfirmation {
            employee_name: "Max".into(),
            timestamp: Utc::now(),
            display_since: Instant::now() - Duration::from_secs(10),
        };

        let next = state.check_timeout(Duration::from_secs(8));
        assert!(matches!(next, AppState::Idle));
    }
}
```

2. **Screenshot comparison** — capture screenshots of each screen state and compare against reference images (manual process, useful for regression testing).

3. **Manual testing** — run the terminal on a development machine and visually verify each screen.

<a id="terminal-offline-buffer-testing"></a>

### Offline Buffer Testing

Test the complete offline flow:

```rust
#[cfg(test)]
mod offline_tests {
    use super::*;

    #[test]
    fn test_full_offline_sync_cycle() {
        let buffer = EventBuffer::new(":memory:", 100).unwrap();

        // Simulate offline: buffer events
        let id1 = buffer.push("TAG001", "terminal-01").unwrap();
        let id2 = buffer.push("TAG002", "terminal-01").unwrap();

        assert_eq!(buffer.pending_count().unwrap(), 2);

        // Simulate sync: get pending, send to server, mark synced
        let pending = buffer.get_pending().unwrap();
        assert_eq!(pending.len(), 2);

        for event in &pending {
            // In real code: send to API, then mark synced on success
            buffer.mark_synced(event.id.unwrap()).unwrap();
        }

        assert_eq!(buffer.pending_count().unwrap(), 0);
    }

    #[test]
    fn test_partial_sync_on_intermittent_connection() {
        let buffer = EventBuffer::new(":memory:", 100).unwrap();

        buffer.push("TAG001", "terminal-01").unwrap();
        buffer.push("TAG002", "terminal-01").unwrap();
        buffer.push("TAG003", "terminal-01").unwrap();

        let pending = buffer.get_pending().unwrap();

        // Simulate: first event syncs, second fails, third not attempted
        buffer.mark_synced(pending[0].id.unwrap()).unwrap();
        // Event 2 failed — stays pending
        // Event 3 not attempted — stays pending

        assert_eq!(buffer.pending_count().unwrap(), 2);

        // Next sync cycle picks up where we left off
        let still_pending = buffer.get_pending().unwrap();
        assert_eq!(still_pending[0].rfid_tag_id, "TAG002");
    }
}
```

---

## Mobile App Testing

<a id="android-testing"></a>

### Android Testing

#### Unit Tests (JUnit 5 + MockK)

```kotlin
class TimeEntryViewModelTest {

    @MockK
    private lateinit var repository: TimeEntryRepository

    private lateinit var viewModel: TimeEntryViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = TimeEntryViewModel(repository)
    }

    @Test
    fun `clockIn should update state to clocked in`() = runTest {
        coEvery { repository.clockIn() } returns Result.success(ClockInResponse(...))

        viewModel.clockIn()

        assertEquals(ClockState.ClockedIn, viewModel.state.value)
    }

    @Test
    fun `clockIn should show error on network failure`() = runTest {
        coEvery { repository.clockIn() } returns Result.failure(IOException())

        viewModel.clockIn()

        assertTrue(viewModel.state.value is ClockState.Error)
    }
}
```

#### Running Android Tests

```bash
cd mobile/android

# Run unit tests
./gradlew test

# Run unit tests for a specific build variant
./gradlew testDebugUnitTest

# Run instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest
```

#### Compose UI Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginButton_isDisabled_whenFieldsAreEmpty() {
        composeTestRule.setContent {
            LoginScreen(onLogin = {})
        }

        composeTestRule
            .onNodeWithText("Login")
            .assertIsNotEnabled()
    }

    @Test
    fun loginButton_isEnabled_whenFieldsAreFilled() {
        composeTestRule.setContent {
            LoginScreen(onLogin = {})
        }

        composeTestRule
            .onNodeWithContentDescription("Username")
            .performTextInput("user@test.com")

        composeTestRule
            .onNodeWithContentDescription("Password")
            .performTextInput("password123")

        composeTestRule
            .onNodeWithText("Login")
            .assertIsEnabled()
    }
}
```

<a id="ios-testing"></a>

### iOS Testing

#### Unit Tests (XCTest)

```swift
import XCTest
@testable import ZeiterfassungApp

final class ServerConfigManagerTests: XCTestCase {

    var sut: ServerConfigManager!

    override func setUp() {
        super.setUp()
        sut = ServerConfigManager.shared
        sut.clearServerUrl()
    }

    func testDefaultUrl_whenNoConfigSet() {
        XCTAssertEqual(
            sut.effectiveServerUrl,
            "https://zeiterfassung.example.com/api"
        )
    }

    func testUserUrl_overridesDefault() {
        sut.saveServerUrl("https://custom.example.com/api")
        XCTAssertEqual(sut.effectiveServerUrl, "https://custom.example.com/api")
    }

    func testIsManaged_returnsFalse_whenNoMDMConfig() {
        XCTAssertFalse(sut.isManaged)
    }
}
```

#### Running iOS Tests

```bash
cd mobile/ios

# Run tests via xcodebuild
xcodebuild test \
  -scheme ZeiterfassungApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -resultBundlePath TestResults

# Run tests with swift test (for SPM-based project)
swift test
```

#### UI Tests (XCUITest)

```swift
import XCTest

final class LoginUITests: XCTestCase {

    let app = XCUIApplication()

    override func setUpWithError() throws {
        continueAfterFailure = false
        app.launch()
    }

    func testLogin_withValidCredentials() throws {
        let usernameField = app.textFields["Username"]
        let passwordField = app.secureTextFields["Password"]
        let loginButton = app.buttons["Login"]

        usernameField.tap()
        usernameField.typeText("employee@test.com")

        passwordField.tap()
        passwordField.typeText("password123")

        loginButton.tap()

        XCTAssertTrue(app.staticTexts["Dashboard"].waitForExistence(timeout: 5))
    }
}
```

<a id="mobile-integration-testing"></a>

### Integration Testing with Mock Server

For testing mobile apps against a controlled API:

#### Option 1: WireMock (Recommended)

Run a WireMock server with predefined responses:

```bash
# Start WireMock
docker run -d --name wiremock \
  -p 8080:8080 \
  -v ./test/wiremock:/home/wiremock \
  wiremock/wiremock:latest
```

Create stub mappings in `test/wiremock/mappings/`:

```json
{
  "request": {
    "method": "POST",
    "url": "/api/auth/login"
  },
  "response": {
    "status": 200,
    "headers": {
      "Content-Type": "application/json"
    },
    "jsonBody": {
      "accessToken": "mock-jwt-token",
      "refreshToken": "mock-refresh-token",
      "expiresIn": 900
    }
  }
}
```

Configure the mobile app to use `http://10.0.2.2:8080/api/` (Android emulator) or `http://localhost:8080/api/` (iOS simulator).

#### Option 2: Backend Test Profile

Run the actual backend with test data:

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=test'
```

<a id="device-testing-matrix"></a>

### Device Testing Matrix

Test on a representative set of devices to catch platform-specific issues:

#### Android

| Device Category | Example | Screen Size | API Level |
|----------------|---------|-------------|-----------|
| Small phone | Pixel 4a | 5.8" | API 30 |
| Standard phone | Pixel 7 | 6.3" | API 33 |
| Large phone | Samsung Galaxy S24 Ultra | 6.8" | API 34 |
| Tablet | Samsung Galaxy Tab S9 | 11" | API 34 |
| Budget device | Samsung Galaxy A14 | 6.6" | API 33 |

#### iOS

| Device Category | Example | Screen Size | iOS Version |
|----------------|---------|-------------|-------------|
| Compact | iPhone SE (3rd gen) | 4.7" | iOS 16+ |
| Standard | iPhone 15 | 6.1" | iOS 17 |
| Large | iPhone 15 Pro Max | 6.7" | iOS 17 |
| Tablet | iPad (10th gen) | 10.9" | iPadOS 16+ |

---

<a id="e2e-overview"></a>

## End-to-End Testing Overview

The Playwright E2E test suite covers complete user flows across all roles:

### Test Suites

| Suite | Tests | Description |
|-------|-------|-------------|
| `auth.spec.ts` | ~10 | Login, logout, password reset, 2FA |
| `time-tracking.spec.ts` | ~15 | Clock in/out, breaks, ArbZG compliance, monthly view |
| `vacation.spec.ts` | ~10 | Request, approve, reject, cancel, balance |
| `admin.spec.ts` | ~10 | User CRUD, role management, settings |
| `dashboard.spec.ts` | ~5 | Widget display, data accuracy |
| `accessibility.spec.ts` | ~12 | WCAG 2.1 AA, keyboard navigation, screen reader |

### Running E2E Tests

```bash
cd frontend

# Start the backend (required for E2E)
# In a separate terminal:
cd ../backend && ./gradlew bootRun

# Run all E2E tests
npx playwright test

# Run with specific browser
npx playwright test --project=chromium

# Run in debug mode
npx playwright test --debug

# Generate HTML report
npx playwright test --reporter=html
npx playwright show-report
```

---

## CI/CD Integration

Tests run automatically in the CI pipeline (`.github/workflows/ci.yml`):

```yaml
# Simplified CI test steps
jobs:
  backend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: cd backend && ./gradlew test jacocoTestReport
      - uses: actions/upload-artifact@v4
        with:
          name: backend-coverage
          path: backend/build/reports/jacoco/

  frontend-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      - run: cd frontend && npm ci && npm test -- --coverage
      - uses: actions/upload-artifact@v4
        with:
          name: frontend-coverage
          path: frontend/coverage/

  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npx playwright install --with-deps
      - run: cd frontend && npx playwright test
      - uses: actions/upload-artifact@v4
        if: failure()
        with:
          name: playwright-report
          path: frontend/playwright-report/

  terminal-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: dtolnay/rust-toolchain@stable
      - run: cd terminal && cargo test

  android-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
      - run: cd mobile/android && ./gradlew testDebugUnitTest

  ios-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - run: cd mobile/ios && swift test
```

---

## Coverage Reporting

### Generating Reports

| Component | Command | Report Location |
|-----------|---------|-----------------|
| Backend | `./gradlew test jacocoTestReport` | `backend/build/reports/jacoco/test/html/index.html` |
| Frontend | `npm run test:coverage` | `frontend/coverage/index.html` |
| Terminal | `cargo tarpaulin --out Html` | `terminal/tarpaulin-report.html` |
| Android | `./gradlew testDebugUnitTest jacocoTestReport` | `app/build/reports/jacoco/` |

### Coverage Targets

| Component | Target | Rationale |
|-----------|--------|-----------|
| Backend | ≥ 90% | Core business logic, ArbZG compliance |
| Frontend | ≥ 85% | UI components, form validation |
| Terminal | ≥ 80% | Buffer logic, config parsing, API client |
| Android | ≥ 80% | ViewModels, repositories, business logic |
| iOS | ≥ 80% | ViewModels, services, business logic |

### Installing `cargo-tarpaulin` (Terminal Coverage)

```bash
cargo install cargo-tarpaulin

cd terminal
cargo tarpaulin --out Html --output-dir coverage/
```

> **Note:** `cargo-tarpaulin` only works on Linux. For macOS, use `cargo-llvm-cov` instead:
>
> ```bash
> cargo install cargo-llvm-cov
> cargo llvm-cov --html --output-dir coverage/
> ```
