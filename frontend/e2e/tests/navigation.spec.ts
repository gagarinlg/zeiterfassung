import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockDashboardApis } from './helpers'

test.describe('Navigation', () => {
  test.describe('Sidebar', () => {
    test('should display sidebar with navigation links for admin user', async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockDashboardApis(page)

      await page.goto('/dashboard')

      // App branding in sidebar
      await expect(page.getByRole('heading', { name: 'Zeiterfassung' })).toBeVisible()

      // Core navigation links (German labels by default)
      await expect(page.getByRole('link', { name: /Dashboard/i })).toBeVisible()
      await expect(page.getByRole('link', { name: /Zeiterfassung/i })).toBeVisible()
      await expect(page.getByRole('link', { name: /Urlaub/i })).toBeVisible()

      // Permission-gated links should be visible for admin
      await expect(page.getByRole('link', { name: /Urlaubsgenehmigungen/i })).toBeVisible()
      await expect(page.getByRole('link', { name: /Administration/i })).toBeVisible()

      // User info and logout
      await expect(page.getByText('Admin User')).toBeVisible()
      await expect(page.getByRole('button', { name: /Abmelden/i })).toBeVisible()
    })
  })

  test.describe('Route Navigation', () => {
    test('should navigate to Time Tracking page', async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockDashboardApis(page)

      // Mock time-tracking APIs
      await page.route('**/api/time-tracking/status', (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            status: 'CLOCKED_OUT',
            clockedInSince: null,
            breakStartedAt: null,
            elapsedWorkMinutes: 0,
            elapsedBreakMinutes: 0,
            todayWorkMinutes: 0,
            todayBreakMinutes: 0,
          }),
        }),
      )
      await page.route('**/api/time-tracking/entries**', (route) =>
        route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
      )
      await page.route('**/api/time-tracking/monthly-summary**', (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            userId: '00000000-0000-0000-0000-000000000001',
            startDate: '2025-01-01',
            endDate: '2025-01-31',
            dailySummaries: [],
            totalWorkMinutes: 0,
            totalBreakMinutes: 0,
            totalOvertimeMinutes: 0,
            entries: [],
          }),
        }),
      )

      await page.goto('/dashboard')
      await page.getByRole('link', { name: /Zeiterfassung/i }).click()

      await expect(page).toHaveURL(/\/time-tracking/)
      await expect(page.getByRole('heading', { name: /Zeiterfassung/i })).toBeVisible()
    })

    test('should navigate to Vacation page', async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockDashboardApis(page)

      // Mock vacation APIs
      await page.route('**/api/vacation/**', (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 10 }),
        }),
      )
      await page.route('**/api/holidays**', (route) =>
        route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }),
      )

      await page.goto('/dashboard')
      await page.getByRole('link', { name: /^Urlaub$/i }).click()

      await expect(page).toHaveURL(/\/vacation/)
    })
  })

  test.describe('Protected Routes', () => {
    test('should redirect to /login when accessing protected routes without auth', async ({
      page,
    }) => {
      const protectedPaths = ['/dashboard', '/time-tracking', '/vacation', '/admin']

      for (const path of protectedPaths) {
        await page.goto(path)
        await expect(page).toHaveURL(/\/login/)
      }
    })
  })

  test.describe('Logout', () => {
    test('should logout and redirect to login page', async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockDashboardApis(page)

      await page.goto('/dashboard')

      // Click logout
      await page.getByRole('button', { name: /Abmelden/i }).click()

      // Should redirect to login
      await expect(page).toHaveURL(/\/login/)
    })
  })
})
