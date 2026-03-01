import React, { createContext, useState, useCallback, useEffect, useRef } from 'react'
import type { User, AuthTokens } from '../types'
import { authService } from '../services/authService'

export interface AuthContextValue {
  user: User | null
  tokens: AuthTokens | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (tokens: AuthTokens, user: User) => void
  logout: () => Promise<void>
  updateUser: (user: User) => void
  hasPermission: (permission: string) => boolean
  hasRole: (role: string) => boolean
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  // Use lazy initialization to read auth state from localStorage synchronously.
  // This avoids calling setState inside a useEffect (react-hooks/set-state-in-effect).
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('auth_user')
    return stored ? (JSON.parse(stored) as User) : null
  })
  const [tokens, setTokens] = useState<AuthTokens | null>(() => {
    const stored = localStorage.getItem('auth_tokens')
    return stored ? (JSON.parse(stored) as AuthTokens) : null
  })
  // Auth state is known immediately via lazy init — no async loading phase needed.
  // Using useState rather than a constant preserves the AuthContextValue API shape
  // and allows future async initialization without changing the interface.
  const [isLoading] = useState(false)
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  // Ref to hold the latest scheduleTokenRefresh so the timer callback can call
  // it recursively without violating react-hooks/immutability. Initialized to
  // null; the useEffect below sets it before any timer can fire.
  const scheduleTokenRefreshRef = useRef<((expiresInSeconds: number) => void) | null>(null)

  const scheduleTokenRefresh = useCallback((expiresInSeconds: number) => {
    if (refreshTimerRef.current) {
      clearTimeout(refreshTimerRef.current)
    }
    // Refresh 1 minute before expiry
    const refreshIn = Math.max((expiresInSeconds - 60) * 1000, 0)
    refreshTimerRef.current = setTimeout(async () => {
      const stored = localStorage.getItem('auth_tokens')
      if (!stored) return
      const storedTokens = JSON.parse(stored) as AuthTokens
      try {
        const newTokens = await authService.refreshToken(storedTokens.refreshToken)
        localStorage.setItem('auth_tokens', JSON.stringify(newTokens))
        setTokens(newTokens)
        scheduleTokenRefreshRef.current?.(newTokens.expiresIn)
      } catch {
        setTokens(null)
        setUser(null)
        localStorage.removeItem('auth_tokens')
        localStorage.removeItem('auth_user')
      }
    }, refreshIn)
  }, [])
  // Keep the ref in sync whenever scheduleTokenRefresh changes (stable, runs once).
  useEffect(() => {
    scheduleTokenRefreshRef.current = scheduleTokenRefresh
  }, [scheduleTokenRefresh])

  // Start the refresh timer on mount using the tokens already loaded via lazy init.
  // No setState calls here — satisfies react-hooks/set-state-in-effect.
  useEffect(() => {
    const stored = localStorage.getItem('auth_tokens')
    if (stored) {
      const parsed = JSON.parse(stored) as AuthTokens
      if (parsed.expiresIn) {
        scheduleTokenRefresh(parsed.expiresIn)
      }
    }

    return () => {
      if (refreshTimerRef.current) {
        clearTimeout(refreshTimerRef.current)
      }
    }
  }, [scheduleTokenRefresh])

  const login = useCallback(
    (newTokens: AuthTokens, newUser: User) => {
      setTokens(newTokens)
      setUser(newUser)
      localStorage.setItem('auth_tokens', JSON.stringify(newTokens))
      localStorage.setItem('auth_user', JSON.stringify(newUser))
      if (newTokens.expiresIn) {
        scheduleTokenRefresh(newTokens.expiresIn)
      }
    },
    [scheduleTokenRefresh]
  )

  const logout = useCallback(async () => {
    const stored = localStorage.getItem('auth_tokens')
    if (stored) {
      const storedTokens = JSON.parse(stored) as AuthTokens
      try {
        await authService.logout(storedTokens.refreshToken)
      } catch {
        // Ignore logout errors
      }
    }
    if (refreshTimerRef.current) {
      clearTimeout(refreshTimerRef.current)
    }
    setTokens(null)
    setUser(null)
    localStorage.removeItem('auth_tokens')
    localStorage.removeItem('auth_user')
  }, [])

  const updateUser = useCallback((updatedUser: User) => {
    setUser(updatedUser)
    localStorage.setItem('auth_user', JSON.stringify(updatedUser))
  }, [])

  const hasPermission = useCallback(
    (permission: string) => user?.permissions.includes(permission) ?? false,
    [user]
  )

  const hasRole = useCallback(
    (role: string) => user?.roles.includes(role) ?? false,
    [user]
  )

  return (
    <AuthContext.Provider
      value={{
        user,
        tokens,
        isAuthenticated: !!tokens && !!user,
        isLoading,
        login,
        logout,
        updateUser,
        hasPermission,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
