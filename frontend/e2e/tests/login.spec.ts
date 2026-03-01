import { test, expect } from '@playwright/test'
import { mockAuthenticatedUser, mockDashboardApis, MOCK_USER, MOCK_TOKENS } from './helpers'

test.describe('Login Page', () => {
  test.describe('Rendering', () => {
    test('should display the login form with all elements', async ({ page }) => {
      await page.goto('/login')

      // App heading
      await expect(page.getByRole('heading', { name: 'Zeiterfassung' })).toBeVisible()

      // Email and password fields (German labels by default)
      await expect(page.getByLabel(/E-Mail/)).toBeVisible()
      await expect(page.getByLabel(/Passwort/)).toBeVisible()

      // Submit button
      await expect(page.getByRole('button', { name: /Anmelden/i })).toBeVisible()

      // Language toggle button
      await expect(page.getByRole('button', { name: /Sprache/i })).toBeVisible()
    })

    test('should toggle language between DE and EN', async ({ page }) => {
      await page.goto('/login')

      // Default is German — button shows "EN" to switch
      const langButton = page.getByRole('button', { name: /Sprache/i })
      await expect(langButton).toHaveText('EN')

      // Switch to English
      await langButton.click()
      await expect(langButton).toHaveText('DE')

      // Labels should now be in English
      await expect(page.getByLabel(/Email Address/i)).toBeVisible()
      await expect(page.getByLabel(/Password/i)).toBeVisible()
      await expect(page.getByRole('button', { name: /Sign In/i })).toBeVisible()
    })
  })

  test.describe('Redirect', () => {
    test('should redirect unauthenticated users from /dashboard to /login', async ({ page }) => {
      await page.goto('/dashboard')
      await expect(page).toHaveURL(/\/login/)
    })

    test('should redirect unauthenticated users from /time-tracking to /login', async ({ page }) => {
      await page.goto('/time-tracking')
      await expect(page).toHaveURL(/\/login/)
    })

    test('should redirect unauthenticated users from /admin to /login', async ({ page }) => {
      await page.goto('/admin')
      await expect(page).toHaveURL(/\/login/)
    })

    test('should redirect root path to /login when not authenticated', async ({ page }) => {
      await page.goto('/')
      await expect(page).toHaveURL(/\/login/)
    })
  })

  test.describe('Validation', () => {
    test('should show validation error for empty email and password on submit', async ({ page }) => {
      await page.goto('/login')

      // Click submit without filling anything
      await page.getByRole('button', { name: /Anmelden/i }).click()

      // Validation errors should appear (German)
      await expect(page.locator('#email-error')).toBeVisible()
      await expect(page.locator('#password-error')).toBeVisible()
    })

    test('should show validation error for invalid email format', async ({ page }) => {
      await page.goto('/login')

      await page.getByLabel(/E-Mail/).fill('not-an-email')
      await page.getByLabel(/Passwort/).fill('somepassword')
      await page.getByRole('button', { name: /Anmelden/i }).click()

      // Email validation error should appear
      await expect(page.locator('#email-error')).toBeVisible()
      // Password error should NOT appear
      await expect(page.locator('#password-error')).not.toBeVisible()
    })

    test('should show validation error for empty password only', async ({ page }) => {
      await page.goto('/login')

      await page.getByLabel(/E-Mail/).fill('admin@zeiterfassung.local')
      // Leave password empty
      await page.getByRole('button', { name: /Anmelden/i }).click()

      // Only password error should appear
      await expect(page.locator('#email-error')).not.toBeVisible()
      await expect(page.locator('#password-error')).toBeVisible()
    })
  })

  test.describe('Login Flow', () => {
    test('should successfully login and redirect to dashboard', async ({ page }) => {
      // Mock the login API endpoint
      await page.route('**/api/auth/login', (route) =>
        route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify({
            accessToken: MOCK_TOKENS.accessToken,
            refreshToken: MOCK_TOKENS.refreshToken,
            expiresIn: MOCK_TOKENS.expiresIn,
            user: MOCK_USER,
          }),
        }),
      )

      // Mock the APIs the dashboard will call after redirect
      await mockAuthenticatedUser(page)
      await mockDashboardApis(page)

      await page.goto('/login')

      // Fill in the form
      await page.getByLabel(/E-Mail/).fill('admin@zeiterfassung.local')
      await page.getByLabel(/Passwort/).fill('Admin@123!')
      await page.getByRole('button', { name: /Anmelden/i }).click()

      // Should navigate to dashboard
      await expect(page).toHaveURL(/\/dashboard/)
    })

    test('should show error message on failed login (401)', async ({ page }) => {
      await page.route('**/api/auth/login', (route) =>
        route.fulfill({
          status: 401,
          contentType: 'application/json',
          body: JSON.stringify({ message: 'Invalid credentials' }),
        }),
      )

      await page.goto('/login')

      await page.getByLabel(/E-Mail/).fill('admin@zeiterfassung.local')
      await page.getByLabel(/Passwort/).fill('wrongpassword')
      await page.getByRole('button', { name: /Anmelden/i }).click()

      // Error alert should be visible with the German error message
      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
      await expect(alert).toContainText(/Ungültige Anmeldedaten/i)
    })

    test('should show account locked message on 423 response', async ({ page }) => {
      await page.route('**/api/auth/login', (route) =>
        route.fulfill({
          status: 423,
          contentType: 'application/json',
          body: JSON.stringify({ message: 'Account locked' }),
        }),
      )

      await page.goto('/login')

      await page.getByLabel(/E-Mail/).fill('admin@zeiterfassung.local')
      await page.getByLabel(/Passwort/).fill('Admin@123!')
      await page.getByRole('button', { name: /Anmelden/i }).click()

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
      await expect(alert).toContainText(/gesperrt/i)
    })

    test('should show rate limit message on 429 response', async ({ page }) => {
      await page.route('**/api/auth/login', (route) =>
        route.fulfill({
          status: 429,
          contentType: 'application/json',
          body: JSON.stringify({ message: 'Too many attempts' }),
        }),
      )

      await page.goto('/login')

      await page.getByLabel(/E-Mail/).fill('admin@zeiterfassung.local')
      await page.getByLabel(/Passwort/).fill('Admin@123!')
      await page.getByRole('button', { name: /Anmelden/i }).click()

      const alert = page.getByRole('alert')
      await expect(alert).toBeVisible()
      await expect(alert).toContainText(/Anmeldeversuche/i)
    })
  })
})
