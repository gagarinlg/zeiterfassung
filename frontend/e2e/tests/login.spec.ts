import { test, expect } from '@playwright/test'

test.describe('Login', () => {
  test('should display login form', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: 'Zeiterfassung' })).toBeVisible()
    await expect(page.getByLabel(/E-Mail/)).toBeVisible()
    await expect(page.getByLabel(/Passwort/)).toBeVisible()
  })

  test('should redirect to login when not authenticated', async ({ page }) => {
    await page.goto('/dashboard')
    await expect(page).toHaveURL(/\/login/)
  })
})
