import { Page } from '@playwright/test'

/** Mock user data used across all E2E tests. */
export const MOCK_USER = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'admin@zeiterfassung.local',
  firstName: 'Admin',
  lastName: 'User',
  employeeNumber: 'EMP001',
  isActive: true,
  roles: ['ADMIN'],
  permissions: [
    'time.track',
    'time.view.team',
    'vacation.request',
    'vacation.approve',
    'admin.users.manage',
  ],
}

export const MOCK_TOKENS = {
  accessToken: 'mock-access-token',
  refreshToken: 'mock-refresh-token',
  expiresIn: 900,
}

export const MOCK_TRACKING_STATUS_CLOCKED_OUT = {
  status: 'CLOCKED_OUT',
  clockedInSince: null,
  breakStartedAt: null,
  elapsedWorkMinutes: 0,
  elapsedBreakMinutes: 0,
  todayWorkMinutes: 0,
  todayBreakMinutes: 0,
}

export const MOCK_TRACKING_STATUS_CLOCKED_IN = {
  status: 'CLOCKED_IN',
  clockedInSince: new Date().toISOString(),
  breakStartedAt: null,
  elapsedWorkMinutes: 125,
  elapsedBreakMinutes: 0,
  todayWorkMinutes: 125,
  todayBreakMinutes: 0,
}

export const MOCK_WEEKLY_SUMMARY = {
  userId: MOCK_USER.id,
  startDate: '2025-01-06',
  endDate: '2025-01-12',
  dailySummaries: [
    {
      id: 'd1',
      userId: MOCK_USER.id,
      date: '2025-01-06',
      totalWorkMinutes: 480,
      totalBreakMinutes: 30,
      overtimeMinutes: 0,
      isCompliant: true,
    },
  ],
  totalWorkMinutes: 480,
  totalBreakMinutes: 30,
  totalOvertimeMinutes: 0,
  entries: [],
}

export const MOCK_MONTHLY_SUMMARY = {
  userId: MOCK_USER.id,
  startDate: '2025-01-01',
  endDate: '2025-01-31',
  dailySummaries: [
    {
      id: 'd1',
      userId: MOCK_USER.id,
      date: '2025-01-06',
      totalWorkMinutes: 480,
      totalBreakMinutes: 30,
      overtimeMinutes: 0,
      isCompliant: true,
    },
    {
      id: 'd2',
      userId: MOCK_USER.id,
      date: '2025-01-07',
      totalWorkMinutes: 510,
      totalBreakMinutes: 45,
      overtimeMinutes: 30,
      isCompliant: true,
    },
  ],
  totalWorkMinutes: 990,
  totalBreakMinutes: 75,
  totalOvertimeMinutes: 30,
  entries: [],
}

export const MOCK_VACATION_BALANCE = {
  id: 'vb1',
  userId: MOCK_USER.id,
  year: new Date().getFullYear(),
  totalDays: 30,
  usedDays: 5,
  carriedOverDays: 2,
  remainingDays: 25,
  pendingDays: 3,
}

/**
 * Sets up localStorage with mock auth state and intercepts API routes
 * that the authenticated app makes on initial load (e.g. /auth/me, /auth/refresh).
 */
export async function mockAuthenticatedUser(page: Page) {
  const user = MOCK_USER
  const tokens = MOCK_TOKENS

  // Inject auth state into localStorage before any navigation
  await page.addInitScript(
    ({ u, t }) => {
      localStorage.setItem('auth_tokens', JSON.stringify(t))
      localStorage.setItem('auth_user', JSON.stringify(u))
    },
    { u: user, t: tokens },
  )

  // Mock auth endpoints that the app may call during navigation
  await page.route('**/api/auth/me', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(user) }),
  )
  await page.route('**/api/auth/refresh', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(tokens),
    }),
  )
  await page.route('**/api/auth/logout', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: '{}' }),
  )
}

/** Mock the common dashboard API calls so the page loads without errors. */
export async function mockDashboardApis(page: Page) {
  await page.route('**/api/time/status', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_TRACKING_STATUS_CLOCKED_OUT),
    }),
  )
  await page.route('**/api/time/summary/weekly**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_WEEKLY_SUMMARY),
    }),
  )
  await page.route('**/api/vacation/balance**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_VACATION_BALANCE),
    }),
  )
  await page.route('**/api/time/manage/team/status', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({}) }),
  )
  await page.route('**/api/vacation/requests?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 1 }),
    }),
  )
  await page.route(`**/api/users/${MOCK_USER.id}/team`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
  )
  await page.route('**/api/vacation/pending**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 1 }),
    }),
  )
}

/** Mock time-tracking page specific APIs. */
export async function mockTimeTrackingApis(page: Page, statusOverride?: object) {
  const trackingStatus = statusOverride ?? MOCK_TRACKING_STATUS_CLOCKED_OUT

  await page.route('**/api/time/status', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(trackingStatus),
    }),
  )
  await page.route('**/api/time/entries**', (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([]) }),
  )
  await page.route('**/api/time/summary/monthly**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_MONTHLY_SUMMARY),
    }),
  )
}
