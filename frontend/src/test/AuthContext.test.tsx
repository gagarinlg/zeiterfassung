import { renderHook, act } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { AuthProvider } from '../context/AuthContext'
import { useAuth } from '../hooks/useAuth'
import type { User, AuthTokens } from '../types'

const mockUser: User = {
  id: '123',
  email: 'test@test.com',
  firstName: 'Test',
  lastName: 'User',
  isActive: true,
  roles: ['EMPLOYEE'],
  permissions: ['time.track.own'],
}

const mockTokens: AuthTokens = {
  accessToken: 'access-token',
  refreshToken: 'refresh-token',
  expiresIn: 900,
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('should start as not authenticated', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    // Wait for loading to complete
    await act(async () => {
      await new Promise((r) => setTimeout(r, 0))
    })
    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
  })

  it('should login and set user', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    await act(async () => {
      result.current.login(mockTokens, mockUser)
    })
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user).toEqual(mockUser)
  })

  it('should persist tokens to localStorage on login', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    await act(async () => {
      result.current.login(mockTokens, mockUser)
    })
    expect(localStorage.getItem('auth_tokens')).toBeTruthy()
    expect(localStorage.getItem('auth_user')).toBeTruthy()
  })

  it('should logout and clear user', async () => {
    vi.mock('../services/authService', () => ({
      authService: { logout: vi.fn().mockResolvedValue(undefined) },
    }))
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    await act(async () => {
      result.current.login(mockTokens, mockUser)
    })
    await act(async () => {
      await result.current.logout()
    })
    expect(result.current.isAuthenticated).toBe(false)
    expect(result.current.user).toBeNull()
  })

  it('should hasPermission return true for valid permission', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    await act(async () => {
      result.current.login(mockTokens, mockUser)
    })
    expect(result.current.hasPermission('time.track.own')).toBe(true)
    expect(result.current.hasPermission('admin.users.manage')).toBe(false)
  })

  it('should hasRole return true for valid role', async () => {
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    await act(async () => {
      result.current.login(mockTokens, mockUser)
    })
    expect(result.current.hasRole('EMPLOYEE')).toBe(true)
    expect(result.current.hasRole('ADMIN')).toBe(false)
  })

  it('should restore session from localStorage', async () => {
    localStorage.setItem('auth_tokens', JSON.stringify(mockTokens))
    localStorage.setItem('auth_user', JSON.stringify(mockUser))
    const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider })
    await act(async () => {
      await new Promise((r) => setTimeout(r, 0))
    })
    expect(result.current.isAuthenticated).toBe(true)
    expect(result.current.user?.email).toBe('test@test.com')
  })
})
