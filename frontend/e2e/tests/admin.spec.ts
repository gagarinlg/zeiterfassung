import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockDashboardApis, MOCK_USER } from './helpers'
import { Page } from '@playwright/test'

const MOCK_USERS = {
  content: [
    {
      id: '00000000-0000-0000-0000-000000000001',
      email: 'admin@zeiterfassung.local',
      firstName: 'Admin',
      lastName: 'User',
      employeeNumber: 'EMP001',
      isActive: true,
      roles: ['ADMIN'],
      permissions: ['admin.users.manage'],
    },
    {
      id: '00000000-0000-0000-0000-000000000010',
      email: 'employee@zeiterfassung.local',
      firstName: 'Max',
      lastName: 'Mustermann',
      employeeNumber: 'EMP002',
      isActive: true,
      roles: ['EMPLOYEE'],
      permissions: ['time.track'],
    },
  ],
  totalElements: 2,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 20,
}

const MOCK_AUDIT_LOG = {
  content: [
    {
      id: 'al1',
      userId: MOCK_USER.id,
      userEmail: 'admin@zeiterfassung.local',
      userFullName: 'Admin User',
      action: 'USER_LOGIN',
      entityType: 'USER',
      entityId: MOCK_USER.id,
      ipAddress: '127.0.0.1',
      userAgent: 'Mozilla/5.0',
      createdAt: '2026-03-01T10:00:00Z',
    },
  ],
  totalElements: 1,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 50,
}

const MOCK_SETTINGS = [
  { key: 'company.name', value: 'Zeiterfassung GmbH', description: 'Company name', updatedAt: '2026-01-01T00:00:00Z' },
  { key: 'working.hours.default', value: '8', description: 'Default daily working hours', updatedAt: '2026-01-01T00:00:00Z' },
  { key: 'display.date_format', value: 'DD.MM.YYYY', description: 'Default date format', updatedAt: '2026-01-01T00:00:00Z' },
]

async function mockAdminApis(page: Page) {
  await page.route('**/api/users?**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_USERS),
    }),
  )
  await page.route('**/api/users', (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({ id: 'new-user', email: 'new@test.com', firstName: 'New', lastName: 'User', isActive: true, roles: ['EMPLOYEE'], permissions: [] }),
      })
    }
    return route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_USERS),
    })
  })
  await page.route('**/api/admin/audit-log**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_AUDIT_LOG),
    }),
  )
  await page.route('**/api/admin/settings', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(MOCK_SETTINGS),
    }),
  )
}

test.describe('Admin Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockDashboardApis(page)
    await mockAdminApis(page)
  })

  test.describe('Rendering', () => {
    test('should render the admin page with title', async ({ page }) => {
      await page.goto('/admin')

      await expect(page.getByRole('heading', { name: /Administration/i })).toBeVisible()
    })

    test('should show tab navigation', async ({ page }) => {
      await page.goto('/admin')

      await expect(page.getByRole('button', { name: /Benutzerverwaltung/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Audit-Protokoll/i })).toBeVisible()
      await expect(page.getByRole('button', { name: /Systemeinstellungen/i })).toBeVisible()
    })
  })

  test.describe('Users Tab', () => {
    test('should display user list', async ({ page }) => {
      await page.goto('/admin')

      await expect(page.getByText('admin@zeiterfassung.local')).toBeVisible()
      await expect(page.getByText('employee@zeiterfassung.local')).toBeVisible()
    })

    test('should show create user button', async ({ page }) => {
      await page.goto('/admin')

      await expect(page.getByRole('button', { name: /Benutzer erstellen/i })).toBeVisible()
    })

    test('should open create user modal', async ({ page }) => {
      await page.goto('/admin')

      await page.getByRole('button', { name: /Benutzer erstellen/i }).click()

      // Modal should appear with form fields
      await expect(page.getByRole('dialog')).toBeVisible()
    })
  })

  test.describe('Audit Log Tab', () => {
    test('should display audit log entries', async ({ page }) => {
      await page.goto('/admin')

      await page.getByRole('button', { name: /Audit-Protokoll/i }).click()

      // Should show the log entry action
      await expect(page.getByText('USER_LOGIN')).toBeVisible()
    })

    test('should show admin user email in audit log', async ({ page }) => {
      await page.goto('/admin')

      await page.getByRole('button', { name: /Audit-Protokoll/i }).click()

      await expect(page.getByText('admin@zeiterfassung.local').first()).toBeVisible()
    })
  })

  test.describe('Settings Tab', () => {
    test('should display system settings', async ({ page }) => {
      await page.goto('/admin')

      await page.getByRole('button', { name: /Systemeinstellungen/i }).click()

      // Should show setting keys
      await expect(page.getByText('company.name')).toBeVisible()
      await expect(page.getByText('Zeiterfassung GmbH')).toBeVisible()
    })

    test('should show edit buttons for settings', async ({ page }) => {
      await page.goto('/admin')

      await page.getByRole('button', { name: /Systemeinstellungen/i }).click()

      // Each setting should have an edit button
      const editButtons = page.locator('button:has(svg)')
      await expect(editButtons.first()).toBeVisible()
    })
  })

  test.describe('Error Handling', () => {
    test('should show error when users API fails', async ({ page }) => {
      await page.route('**/api/users?**', (route) =>
        route.fulfill({ status: 500, contentType: 'application/json', body: '{}' }),
      )

      await page.goto('/admin')

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
    })
  })
})
