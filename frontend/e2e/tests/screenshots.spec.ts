import { test } from '@playwright/test'
import {
  mockAuthenticatedUser,
  mockDashboardApis,
  mockTimeTrackingApis,
  MOCK_USER,
  MOCK_TRACKING_STATUS_CLOCKED_IN,
  MOCK_VACATION_BALANCE,
} from './helpers'

const SCREENSHOT_DIR = '../docs/screenshots'

test.describe('Documentation Screenshots', () => {
  test('Login page (English)', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    await page.screenshot({ path: `${SCREENSHOT_DIR}/login-en.png`, fullPage: true })
  })

  test('Login page (German)', async ({ page }) => {
    await page.goto('/login')
    await page.waitForLoadState('networkidle')
    // Click language toggle to switch to German
    const langToggle = page.getByRole('button', { name: /deutsch|german/i })
    if (await langToggle.isVisible()) {
      await langToggle.click()
      await page.waitForTimeout(300)
    }
    await page.screenshot({ path: `${SCREENSHOT_DIR}/login-de.png`, fullPage: true })
  })

  test('Dashboard page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockDashboardApis(page)
    await page.goto('/')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/dashboard.png`, fullPage: true })
  })

  test('Time Tracking page - clocked out', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockTimeTrackingApis(page)
    await page.goto('/time-tracking')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/time-tracking-clocked-out.png`, fullPage: true })
  })

  test('Time Tracking page - clocked in', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await mockTimeTrackingApis(page, MOCK_TRACKING_STATUS_CLOCKED_IN)
    await page.goto('/time-tracking')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/time-tracking-clocked-in.png`, fullPage: true })
  })

  test('Vacation page', async ({ page }) => {
    await mockAuthenticatedUser(page)
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
        body: JSON.stringify({
          content: [
            {
              id: 'vr1',
              userId: MOCK_USER.id,
              startDate: '2025-06-01',
              endDate: '2025-06-05',
              isHalfDayStart: false,
              isHalfDayEnd: false,
              totalDays: 5,
              status: 'APPROVED',
              notes: 'Summer vacation',
              createdAt: '2025-05-01T10:00:00Z',
            },
            {
              id: 'vr2',
              userId: MOCK_USER.id,
              startDate: '2025-07-14',
              endDate: '2025-07-18',
              isHalfDayStart: false,
              isHalfDayEnd: true,
              totalDays: 4.5,
              status: 'PENDING',
              notes: '',
              createdAt: '2025-06-15T09:00:00Z',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.route('**/api/vacation/holidays**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([]),
      }),
    )
    await page.route('**/api/vacation/calendar**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          year: 2025,
          month: 6,
          ownRequests: [],
          teamRequests: [],
          publicHolidays: [],
        }),
      }),
    )
    await page.goto('/vacation')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/vacation.png`, fullPage: true })
  })

  test('Vacation Approvals page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/vacation/pending**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'vr3',
              userId: '00000000-0000-0000-0000-000000000002',
              startDate: '2025-08-01',
              endDate: '2025-08-05',
              isHalfDayStart: false,
              isHalfDayEnd: false,
              totalDays: 5,
              status: 'PENDING',
              notes: 'Family trip',
              createdAt: '2025-07-10T10:00:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.route('**/api/users/**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          id: '00000000-0000-0000-0000-000000000002',
          email: 'employee@zeiterfassung.local',
          firstName: 'Max',
          lastName: 'Mustermann',
          isActive: true,
          roles: ['EMPLOYEE'],
          permissions: ['time.track', 'vacation.request'],
        }),
      }),
    )
    await page.goto('/vacation/approvals')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/vacation-approvals.png`, fullPage: true })
  })

  test('Admin page - User Management', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/users?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: MOCK_USER.id,
              email: 'admin@zeiterfassung.local',
              firstName: 'Admin',
              lastName: 'User',
              employeeNumber: 'EMP001',
              isActive: true,
              roles: ['ADMIN'],
              permissions: ['admin.users.manage'],
            },
            {
              id: '00000000-0000-0000-0000-000000000002',
              email: 'max.mustermann@example.com',
              firstName: 'Max',
              lastName: 'Mustermann',
              employeeNumber: 'EMP002',
              isActive: true,
              roles: ['EMPLOYEE'],
              permissions: ['time.track', 'vacation.request'],
            },
            {
              id: '00000000-0000-0000-0000-000000000003',
              email: 'maria.schmidt@example.com',
              firstName: 'Maria',
              lastName: 'Schmidt',
              employeeNumber: 'EMP003',
              isActive: true,
              roles: ['MANAGER'],
              permissions: ['time.track', 'time.view.team', 'vacation.request', 'vacation.approve'],
            },
          ],
          totalElements: 3,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/admin-users.png`, fullPage: true })
  })

  test('Admin page - Audit Log', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/users?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 20 }),
      }),
    )
    await page.route('**/api/admin/audit-log**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'al1',
              userId: MOCK_USER.id,
              userEmail: 'admin@zeiterfassung.local',
              userFullName: 'Admin User',
              action: 'LOGIN',
              entityType: 'User',
              entityId: MOCK_USER.id,
              ipAddress: '192.168.1.100',
              createdAt: new Date().toISOString(),
            },
            {
              id: 'al2',
              userId: MOCK_USER.id,
              userEmail: 'admin@zeiterfassung.local',
              userFullName: 'Admin User',
              action: 'USER_CREATED',
              entityType: 'User',
              entityId: '00000000-0000-0000-0000-000000000002',
              ipAddress: '192.168.1.100',
              createdAt: new Date(Date.now() - 3600000).toISOString(),
            },
          ],
          totalElements: 2,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 50,
        }),
      }),
    )
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(300)
    // Click Audit Log tab
    const auditTab = page.getByRole('button', { name: /audit/i })
    if (await auditTab.isVisible()) {
      await auditTab.click()
      await page.waitForTimeout(500)
    }
    await page.screenshot({ path: `${SCREENSHOT_DIR}/admin-audit-log.png`, fullPage: true })
  })

  test('Admin page - System Settings', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/users?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [], totalElements: 0, totalPages: 0, pageNumber: 0, pageSize: 20 }),
      }),
    )
    await page.route('**/api/admin/settings', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([
          { key: 'arbzg.max_daily_hours', value: '10', description: 'Maximum daily work hours', updatedAt: new Date().toISOString() },
          { key: 'arbzg.mandatory_break_6h', value: '30', description: 'Mandatory break after 6h (minutes)', updatedAt: new Date().toISOString() },
          { key: 'vacation.default_days', value: '30', description: 'Default vacation days per year', updatedAt: new Date().toISOString() },
          { key: 'display.date_format', value: 'DD.MM.YYYY', description: 'Default date format', updatedAt: new Date().toISOString() },
        ]),
      }),
    )
    await page.goto('/admin')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(300)
    // Click Settings tab
    const settingsTab = page.getByRole('button', { name: /settings|einstellung/i })
    if (await settingsTab.isVisible()) {
      await settingsTab.click()
      await page.waitForTimeout(500)
    }
    await page.screenshot({ path: `${SCREENSHOT_DIR}/admin-settings.png`, fullPage: true })
  })

  test('Settings page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.goto('/settings')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/settings.png`, fullPage: true })
  })

  test('Password Reset Request page', async ({ page }) => {
    await page.goto('/forgot-password')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(300)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/forgot-password.png`, fullPage: true })
  })

  test('Sick Leave page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/sick-leave?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'sl1',
              userId: MOCK_USER.id,
              userName: 'Admin User',
              startDate: '2025-11-10',
              endDate: '2025-11-14',
              status: 'CERTIFICATE_RECEIVED',
              hasCertificate: true,
              certificateSubmittedAt: '2025-11-12T10:00:00Z',
              notes: 'Flu',
              reportedById: null,
              reportedByName: null,
              createdAt: '2025-11-10T08:00:00Z',
              updatedAt: '2025-11-12T10:00:00Z',
            },
            {
              id: 'sl2',
              userId: MOCK_USER.id,
              userName: 'Admin User',
              startDate: '2025-12-01',
              endDate: '2025-12-03',
              status: 'REPORTED',
              hasCertificate: false,
              certificateSubmittedAt: null,
              notes: null,
              reportedById: null,
              reportedByName: null,
              createdAt: '2025-12-01T07:30:00Z',
              updatedAt: '2025-12-01T07:30:00Z',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.goto('/sick-leave')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/sick-leave.png`, fullPage: true })
  })

  test('Business Trips page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/business-trips?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'bt1',
              userId: MOCK_USER.id,
              userName: 'Admin User',
              startDate: '2025-09-15',
              endDate: '2025-09-17',
              destination: 'München',
              purpose: 'Customer meeting',
              status: 'COMPLETED',
              approvedById: null,
              approvedByName: null,
              rejectionReason: null,
              notes: null,
              estimatedCost: 850.00,
              actualCost: 920.50,
              costCenter: 'CC-100',
              createdAt: '2025-09-01T10:00:00Z',
              updatedAt: '2025-09-17T18:00:00Z',
            },
            {
              id: 'bt2',
              userId: MOCK_USER.id,
              userName: 'Admin User',
              startDate: '2025-10-20',
              endDate: '2025-10-22',
              destination: 'Berlin',
              purpose: 'Conference',
              status: 'APPROVED',
              approvedById: '00000000-0000-0000-0000-000000000003',
              approvedByName: 'Maria Schmidt',
              rejectionReason: null,
              notes: 'Annual tech summit',
              estimatedCost: 1200.00,
              actualCost: null,
              costCenter: 'CC-200',
              createdAt: '2025-09-20T14:00:00Z',
              updatedAt: '2025-09-22T09:00:00Z',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.goto('/business-trips')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/business-trips.png`, fullPage: true })
  })

  test('Business Trip Approvals page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/business-trips/pending**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'bt3',
              userId: '00000000-0000-0000-0000-000000000002',
              userName: 'Max Mustermann',
              startDate: '2025-11-05',
              endDate: '2025-11-07',
              destination: 'Hamburg',
              purpose: 'Client workshop',
              status: 'REQUESTED',
              approvedById: null,
              approvedByName: null,
              rejectionReason: null,
              notes: 'Need approval by end of week',
              estimatedCost: 600.00,
              actualCost: null,
              costCenter: 'CC-100',
              createdAt: '2025-10-28T11:00:00Z',
              updatedAt: '2025-10-28T11:00:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.goto('/business-trips/approvals')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/business-trip-approvals.png`, fullPage: true })
  })

  test('Projects page', async ({ page }) => {
    await mockAuthenticatedUser(page)
    await page.route('**/api/projects?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'p1',
              name: 'Website Redesign',
              code: 'WEB-001',
              description: 'Complete website overhaul',
              costCenter: 'CC-100',
              isActive: true,
              createdAt: '2025-01-15T10:00:00Z',
              updatedAt: '2025-01-15T10:00:00Z',
            },
            {
              id: 'p2',
              name: 'Mobile App',
              code: 'MOB-001',
              description: 'Native mobile application',
              costCenter: 'CC-200',
              isActive: true,
              createdAt: '2025-02-01T10:00:00Z',
              updatedAt: '2025-02-01T10:00:00Z',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.route('**/api/projects/allocations?**', (route) =>
      route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: [
            {
              id: 'ta1',
              userId: MOCK_USER.id,
              userName: 'Admin User',
              projectId: 'p1',
              projectName: 'Website Redesign',
              projectCode: 'WEB-001',
              date: '2025-10-15',
              minutes: 240,
              notes: 'Frontend development',
              createdAt: '2025-10-15T16:00:00Z',
              updatedAt: '2025-10-15T16:00:00Z',
            },
            {
              id: 'ta2',
              userId: MOCK_USER.id,
              userName: 'Admin User',
              projectId: 'p2',
              projectName: 'Mobile App',
              projectCode: 'MOB-001',
              date: '2025-10-15',
              minutes: 120,
              notes: 'API integration',
              createdAt: '2025-10-15T16:00:00Z',
              updatedAt: '2025-10-15T16:00:00Z',
            },
          ],
          totalElements: 2,
          totalPages: 1,
          pageNumber: 0,
          pageSize: 20,
        }),
      }),
    )
    await page.goto('/projects')
    await page.waitForLoadState('networkidle')
    await page.waitForTimeout(500)
    await page.screenshot({ path: `${SCREENSHOT_DIR}/projects.png`, fullPage: true })
  })
})
