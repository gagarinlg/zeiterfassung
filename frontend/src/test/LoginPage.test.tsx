import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import LoginPage from '../pages/LoginPage'

vi.mock('../services/authService', () => ({
  authService: {
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'de', changeLanguage: vi.fn() },
  }),
}))

function renderLoginPage() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <LoginPage />
      </AuthProvider>
    </BrowserRouter>
  )
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should render login form', () => {
    renderLoginPage()
    expect(screen.getByRole('heading', { name: 'app.name' })).toBeInTheDocument()
    expect(screen.getByLabelText('auth.email')).toBeInTheDocument()
    expect(screen.getByLabelText('auth.password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'auth.login_button' })).toBeInTheDocument()
  })

  it('should show validation errors for empty form', async () => {
    renderLoginPage()
    fireEvent.click(screen.getByRole('button', { name: 'auth.login_button' }))
    await waitFor(() => {
      expect(screen.getByText('auth.validation.email_required')).toBeInTheDocument()
    })
  })

  it('should show server error on failed login', async () => {
    const { authService } = await import('../services/authService')
    vi.mocked(authService.login).mockRejectedValue({ response: { status: 401 } })

    renderLoginPage()
    fireEvent.change(screen.getByLabelText('auth.email'), { target: { value: 'test@test.com' } })
    fireEvent.change(screen.getByLabelText('auth.password'), { target: { value: 'password' } })
    fireEvent.click(screen.getByRole('button', { name: 'auth.login_button' }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })
  })

  it('should show account locked error for 423', async () => {
    const { authService } = await import('../services/authService')
    vi.mocked(authService.login).mockRejectedValue({ response: { status: 423 } })

    renderLoginPage()
    fireEvent.change(screen.getByLabelText('auth.email'), { target: { value: 'test@test.com' } })
    fireEvent.change(screen.getByLabelText('auth.password'), { target: { value: 'password' } })
    fireEvent.click(screen.getByRole('button', { name: 'auth.login_button' }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('auth.account_locked')
    })
  })

  it('should show language switcher button', () => {
    renderLoginPage()
    expect(screen.getByLabelText('auth.switch_language')).toBeInTheDocument()
  })
})
