import { test, expect } from '@playwright/test'
import {
  mockAuthenticatedUser,
  mockTimeTrackingApis,
  MOCK_TRACKING_STATUS_CLOCKED_IN,
  MOCK_TRACKING_STATUS_CLOCKED_OUT,
  MOCK_MONTHLY_SUMMARY,
} from './helpers'

test.describe('Time Tracking Page', () => {
  test.describe('Clocked Out State', () => {
    test.beforeEach(async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockTimeTrackingApis(page, MOCK_TRACKING_STATUS_CLOCKED_OUT)
    })

    test('should render the time tracking page with title', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.locator('main').getByRole('heading', { name: /Zeiterfassung/i })).toBeVisible()
    })

    test('should show current status label', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByText(/Aktueller Status/i)).toBeVisible()
      // Status badge shows "Ausgestempelt" (clocked out in German)
      await expect(page.getByText(/Ausgestempelt/i)).toBeVisible()
    })

    test('should show Clock In button when clocked out', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByRole('button', { name: /Einstempeln/i })).toBeVisible()
      // Should NOT show Clock Out or Break buttons
      await expect(page.getByRole('button', { name: /Ausstempeln/i })).not.toBeVisible()
      await expect(page.getByRole('button', { name: /Pause beginnen/i })).not.toBeVisible()
    })

    test('should show "no entries today" message when there are no entries', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByText(/Heute noch keine Einträge/i)).toBeVisible()
    })
  })

  test.describe('Clocked In State', () => {
    test.beforeEach(async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockTimeTrackingApis(page, MOCK_TRACKING_STATUS_CLOCKED_IN)
    })

    test('should show Clocked In status', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByText(/Eingestempelt/i)).toBeVisible()
    })

    test('should show Break and Clock Out buttons when clocked in', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByRole('button', { name: /Pause beginnen/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Ausstempeln/i })).toBeVisible()
      // Clock In button should NOT be visible
      await expect(page.getByRole('button', { name: /Einstempeln/i })).not.toBeVisible()
    })

    test('should show today summary with work and break time', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByText(/Arbeitszeit/i).first()).toBeVisible()
      await expect(page.getByText(/Pausenzeit/i).first()).toBeVisible()
    })
  })

  test.describe('On Break State', () => {
    test('should show End Break and Clock Out buttons when on break', async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockTimeTrackingApis(page, {
        status: 'ON_BREAK',
        clockedInSince: new Date().toISOString(),
        breakStartedAt: new Date().toISOString(),
        elapsedWorkMinutes: 120,
        elapsedBreakMinutes: 15,
        todayWorkMinutes: 120,
        todayBreakMinutes: 15,
      })

      await page.goto('/time-tracking')

      await expect(page.getByText(/In der Pause/i)).toBeVisible()
      await expect(page.getByRole('button', { name: /Pause beenden/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Ausstempeln/i })).toBeVisible()
    })
  })

  test.describe('Monthly Timesheet', () => {
    test.beforeEach(async ({ page }) => {
      await mockAuthenticatedUser(page)
      await mockTimeTrackingApis(page)
    })

    test('should display the timesheet table', async ({ page }) => {
      await page.goto('/time-tracking')

      // Timesheet heading (German: "Stundenzettel")
      await expect(page.getByRole('heading', { name: /Stundenzettel/i })).toBeVisible()
    })

    test('should display timesheet column headers', async ({ page }) => {
      await page.goto('/time-tracking')

      // Table column headers
      await expect(page.getByRole('columnheader', { name: /Datum/i })).toBeVisible()
      await expect(page.getByRole('columnheader', { name: /Arbeitszeit/i })).toBeVisible()
      await expect(page.getByRole('columnheader', { name: /Pausenzeit/i })).toBeVisible()
      await expect(page.getByRole('columnheader', { name: /Überstunden/i })).toBeVisible()
      await expect(page.getByRole('columnheader', { name: /Status/i })).toBeVisible()
    })

    test('should show daily entries in the timesheet', async ({ page }) => {
      await page.goto('/time-tracking')

      // Check that dates from mock data appear (formatted as DD.MM.YYYY)
      for (const day of MOCK_MONTHLY_SUMMARY.dailySummaries) {
        const [year, month, dayOfMonth] = day.date.split('-')
        const formatted = `${dayOfMonth}.${month}.${year}`
        await expect(page.getByText(formatted)).toBeVisible()
      }
    })

    test('should show total row in timesheet footer', async ({ page }) => {
      await page.goto('/time-tracking')

      // Total row (German: "Gesamt")
      await expect(page.getByText(/Gesamt/i)).toBeVisible()
    })

    test('should show compliance badges', async ({ page }) => {
      await page.goto('/time-tracking')

      // All mock days are compliant (German: "Konform")
      const badges = page.getByText(/Konform/i)
      await expect(badges.first()).toBeVisible()
    })

    test('should show CSV export button', async ({ page }) => {
      await page.goto('/time-tracking')

      await expect(page.getByRole('button', { name: /CSV exportieren/i })).toBeVisible()
    })
  })

  test.describe('Error Handling', () => {
    test('should show error alert when API fails', async ({ page }) => {
      await mockAuthenticatedUser(page)

      await page.route('**/api/time/status', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )
      await page.route('**/api/time/entries**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )
      await page.route('**/api/time/summary/monthly**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )

      await page.goto('/time-tracking')

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
    })
  })
})
