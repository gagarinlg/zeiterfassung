import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockDashboardApis, MOCK_VACATION_BALANCE } from './helpers'
import { Page } from '@playwright/test'

const MOCK_VACATION_REQUESTS = {
  content: [
    {
      id: 'vr1',
      userId: '00000000-0000-0000-0000-000000000001',
      startDate: '2026-03-10',
      endDate: '2026-03-14',
      totalDays: 5,
      isHalfDayStart: false,
      isHalfDayEnd: false,
      status: 'APPROVED',
      notes: 'Spring break',
    },
    {
      id: 'vr2',
      userId: '00000000-0000-0000-0000-000000000001',
      startDate: '2026-04-01',
      endDate: '2026-04-03',
      totalDays: 3,
      isHalfDayStart: false,
      isHalfDayEnd: false,
      status: 'PENDING',
      notes: '',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 20,
}

const MOCK_HOLIDAYS = [
  { id: 'h1', date: '2026-01-01', name: 'Neujahrstag', isRecurring: true },
  { id: 'h2', date: '2026-12-25', name: 'Erster Weihnachtstag', isRecurring: true },
]

const MOCK_CALENDAR = {
  year: 2026,
  month: 3,
  ownRequests: [],
  teamRequests: [],
  publicHolidays: [],
}

async function mockVacationApis(page: Page) {
  await page.route('**/api/vacation/balance**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_VACATION_BALANCE),
    }),
  )
  await page.route('**/api/vacation/requests?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_VACATION_REQUESTS),
    }),
  )
  await page.route('**/api/vacation/requests', (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({ id: 'new1', status: 'PENDING', totalDays: 1 }),
      })
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_VACATION_REQUESTS),
    })
  })
  await page.route('**/api/vacation/holidays**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_HOLIDAYS),
    }),
  )
  await page.route('**/api/vacation/calendar**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_CALENDAR),
    }),
  )
  await page.route('**/api/vacation/pending**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 1 }),
    }),
  )
}

test.describe('Vacation Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockDashboardApis(page)
    await mockVacationApis(page)
  })

  test.describe('Rendering', () => {
    test('should render the vacation page with title', async ({ page }) => {
      await page.goto('/vacation')

      await expect(page.getByRole('heading', { name: /Urlaub/i })).toBeVisible()
    })

    test('should show vacation balance card', async ({ page }) => {
      await page.goto('/vacation')

      // Balance values from MOCK_VACATION_BALANCE
      await expect(page.getByText(String(MOCK_VACATION_BALANCE.totalDays))).toBeVisible()
      await expect(page.getByText(String(MOCK_VACATION_BALANCE.remainingDays))).toBeVisible()
    })

    test('should show tab navigation', async ({ page }) => {
      await page.goto('/vacation')

      // Check tab buttons exist (German labels)
      await expect(page.getByRole('button', { name: /Meine Anträge/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Neuer Antrag/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Kalender/i })).toBeVisible()
    })
  })

  test.describe('Requests Tab', () => {
    test('should display vacation requests in table', async ({ page }) => {
      await page.goto('/vacation')

      // Should show start and end dates from mock requests
      await expect(page.getByText('10.03.2026')).toBeVisible()
      await expect(page.getByText('14.03.2026')).toBeVisible()
    })

    test('should show status badges for requests', async ({ page }) => {
      await page.goto('/vacation')

      // APPROVED and PENDING badges (German)
      await expect(page.getByText(/Genehmigt/i).first()).toBeVisible()
      await expect(page.getByText(/Ausstehend/i).first()).toBeVisible()
    })
  })

  test.describe('New Request Tab', () => {
    test('should show the new request form when tab is clicked', async ({ page }) => {
      await page.goto('/vacation')

      await page.getByRole('button', { name: /Neuer Antrag/i }).click()

      // Start date and end date inputs should be visible
      await expect(page.getByLabel(/Startdatum/i)).toBeVisible()
      await expect(page.getByLabel(/Enddatum/i)).toBeVisible()
    })
  })

  test.describe('Calendar Tab', () => {
    test('should show calendar when tab is clicked', async ({ page }) => {
      await page.goto('/vacation')

      await page.getByRole('button', { name: /Kalender/i }).click()

      // Calendar navigation buttons should be visible
      await expect(page.getByRole('button', { name: '‹' })).toBeVisible()
      await expect(page.getByRole('button', { name: '›' })).toBeVisible()
    })
  })

  test.describe('Error Handling', () => {
    test('should show error when vacation API fails', async ({ page }) => {
      await page.route('**/api/vacation/balance**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )
      await page.route('**/api/vacation/requests?**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )
      await page.route('**/api/vacation/holidays**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )

      await page.goto('/vacation')

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
    })
  })
})
