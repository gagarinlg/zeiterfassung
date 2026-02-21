import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import ProtectedRoute from '../components/ProtectedRoute'

vi.mock('../hooks/useAuth', () => ({
  useAuth: vi.fn(),
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}))

import { useAuth } from '../hooks/useAuth'

describe('ProtectedRoute', () => {
  it('should show loading spinner while loading', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      isLoading: true,
      hasPermission: () => false,
      hasRole: () => false,
      user: null,
      tokens: null,
      login: vi.fn(),
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
    render(
      <MemoryRouter>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    )
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('should redirect to login when not authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: false,
      isLoading: false,
      hasPermission: () => false,
      hasRole: () => false,
      user: null,
      tokens: null,
      login: vi.fn(),
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
    render(
      <MemoryRouter initialEntries={['/dashboard']}>
        <ProtectedRoute>
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    )
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('should show 403 page when missing permission', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      hasPermission: () => false,
      hasRole: () => false,
      user: {
        id: '1',
        email: 'u@u.com',
        firstName: 'U',
        lastName: 'U',
        isActive: true,
        roles: ['EMPLOYEE'],
        permissions: [],
      },
      tokens: { accessToken: 'token', refreshToken: 'rt', expiresIn: 900 },
      login: vi.fn(),
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
    render(
      <MemoryRouter>
        <ProtectedRoute requiredPermission="admin.users.manage">
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    )
    expect(screen.getByText('403')).toBeInTheDocument()
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument()
  })

  it('should render children when authenticated with permission', () => {
    vi.mocked(useAuth).mockReturnValue({
      isAuthenticated: true,
      isLoading: false,
      hasPermission: (p: string) => p === 'admin.users.manage',
      hasRole: () => false,
      user: {
        id: '1',
        email: 'u@u.com',
        firstName: 'U',
        lastName: 'U',
        isActive: true,
        roles: ['ADMIN'],
        permissions: ['admin.users.manage'],
      },
      tokens: { accessToken: 'token', refreshToken: 'rt', expiresIn: 900 },
      login: vi.fn(),
      logout: vi.fn(),
      updateUser: vi.fn(),
    })
    render(
      <MemoryRouter>
        <ProtectedRoute requiredPermission="admin.users.manage">
          <div>Protected Content</div>
        </ProtectedRoute>
      </MemoryRouter>
    )
    expect(screen.getByText('Protected Content')).toBeInTheDocument()
  })
})
