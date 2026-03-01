import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockDashboardApis } from './helpers'

const MOCK_PENDING_REQUESTS = {
  content: [
    {
      id: 'pr1',
      userId: '00000000-0000-0000-0000-000000000002',
      userName: 'Max Mustermann',
      startDate: '2026-03-20',
      endDate: '2026-03-22',
      totalDays: 3,
      isHalfDayStart: false,
      isHalfDayEnd: false,
      status: 'PENDING',
      notes: 'Family event',
    },
    {
      id: 'pr2',
      userId: '00000000-0000-0000-0000-000000000003',
      userName: 'Erika Musterfrau',
      startDate: '2026-04-10',
      endDate: '2026-04-12',
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
  pageSize: 100,
}

test.describe('Vacation Approval Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockDashboardApis(page)
    await page.route('**/api/vacation/pending**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(MOCK_PENDING_REQUESTS),
      }),
    )
  })

  test.describe('Rendering', () => {
    test('should render the approval page with title', async ({ page }) => {
      await page.goto('/vacation/approvals')

      await expect(page.getByRole('heading', { name: /Urlaubsgenehmigungen/i })).toBeVisible()
    })

    test('should show pending requests in table', async ({ page }) => {
      await page.goto('/vacation/approvals')

      // Employee names
      await expect(page.getByText('Max Mustermann')).toBeVisible()
      await expect(page.getByText('Erika Musterfrau')).toBeVisible()
    })

    test('should show approve and reject buttons for each request', async ({ page }) => {
      await page.goto('/vacation/approvals')

      // Each row should have approve/reject buttons
      const approveButtons = page.getByRole('button', { name: /Genehmigen/i })
      await expect(approveButtons).toHaveCount(2)

      const rejectButtons = page.getByRole('button', { name: /Ablehnen/i })
      await expect(rejectButtons).toHaveCount(2)
    })

    test('should show request notes', async ({ page }) => {
      await page.goto('/vacation/approvals')

      await expect(page.getByText('Family event')).toBeVisible()
    })
  })

  test.describe('Empty State', () => {
    test('should show no-pending message when there are no requests', async ({ page }) => {
      await page.route('**/api/vacation/pending**', (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 100 }),
        }),
      )

      await page.goto('/vacation/approvals')

      await expect(page.getByText(/Keine ausstehenden/i)).toBeVisible()
    })
  })

  test.describe('Reject Modal', () => {
    test('should open reject modal when reject button is clicked', async ({ page }) => {
      await page.goto('/vacation/approvals')

      // Click first reject button
      await page.getByRole('button', { name: /Ablehnen/i }).first().click()

      // Modal should appear with reason textarea
      await expect(page.getByPlaceholder(/Ablehnungsgrund/i)).toBeVisible()
    })
  })

  test.describe('Error Handling', () => {
    test('should show error when API fails', async ({ page }) => {
      await page.route('**/api/vacation/pending**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )

      await page.goto('/vacation/approvals')

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
    })
  })
})
