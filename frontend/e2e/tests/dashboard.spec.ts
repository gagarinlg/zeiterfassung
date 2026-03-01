import { test, expect } from '@playwright/test'
import {
  mockAuthenticatedUser,
  mockDashboardApis,
  MOCK_TRACKING_STATUS_CLOCKED_IN,
  MOCK_WEEKLY_SUMMARY,
  MOCK_VACATION_BALANCE,
  MOCK_USER,
} from './helpers'

test.describe('Dashboard Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockDashboardApis(page)
  })

  test.describe('Rendering', () => {
    test('should render the dashboard with title and user name', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.getByRole('heading', { name: /Dashboard/i })).toBeVisible()
      await expect(page.getByText(`${MOCK_USER.firstName} ${MOCK_USER.lastName}`)).toBeVisible()
    })

    test('should show today\'s work hours widget', async ({ page }) => {
      await page.goto('/dashboard')

      // "Heutige Arbeitsstunden" label (German default)
      await expect(page.getByText(/Heutige Arbeitsstunden/i)).toBeVisible()
    })

    test('should show weekly hours widget', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.getByText(/Wochenstunden/i)).toBeVisible()
    })

    test('should show vacation balance widget', async ({ page }) => {
      await page.goto('/dashboard')

      await expect(page.getByText(/Verbleibender Urlaub/i)).toBeVisible()
      // Balance value
      await expect(page.getByText(String(MOCK_VACATION_BALANCE.remainingDays))).toBeVisible()
    })

    test('should show team present widget for admin/manager users', async ({ page }) => {
      await page.goto('/dashboard')

      // Admin has time.view.team permission
      await expect(page.getByText(/Team anwesend/i)).toBeVisible()
    })
  })

  test.describe('Clocked-in Status', () => {
    test('should show clocked-in banner when user is clocked in', async ({ page }) => {
      // Override the tracking status mock to show clocked in
      await page.route('**/api/time-tracking/status', (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(MOCK_TRACKING_STATUS_CLOCKED_IN),
        }),
      )

      await page.goto('/dashboard')

      // Should show "Eingestempelt seit" banner
      await expect(page.getByText(/Eingestempelt seit/i)).toBeVisible()
    })
  })

  test.describe('Weekly Summary', () => {
    test('should display work minutes from weekly summary', async ({ page }) => {
      await page.goto('/dashboard')

      // The weekly summary shows totalWorkMinutes=480 => 8:00
      const weeklyValue = `${Math.floor(MOCK_WEEKLY_SUMMARY.totalWorkMinutes / 60)}:${String(MOCK_WEEKLY_SUMMARY.totalWorkMinutes % 60).padStart(2, '0')}`
      await expect(page.getByText(weeklyValue)).toBeVisible()
    })
  })

  test.describe('Error Handling', () => {
    test('should show error alert when API calls fail', async ({ page }) => {
      // Override tracking status to fail
      await page.route('**/api/time-tracking/status', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{"message":"Server error"}' }),
      )

      await page.goto('/dashboard')

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
    })
  })
})
