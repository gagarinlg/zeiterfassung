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
  const [user, setUser] = useState<User | null>(null)
  const [tokens, setTokens] = useState<AuthTokens | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

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
        scheduleTokenRefresh(newTokens.expiresIn)
      } catch {
        setTokens(null)
        setUser(null)
        localStorage.removeItem('auth_tokens')
        localStorage.removeItem('auth_user')
      }
    }, refreshIn)
  }, [])

  useEffect(() => {
    const storedTokens = localStorage.getItem('auth_tokens')
    const storedUser = localStorage.getItem('auth_user')
    if (storedTokens && storedUser) {
      const parsedTokens = JSON.parse(storedTokens) as AuthTokens
      const parsedUser = JSON.parse(storedUser) as User
      setTokens(parsedTokens)
      setUser(parsedUser)
      if (parsedTokens.expiresIn) {
        scheduleTokenRefresh(parsedTokens.expiresIn)
      }
    }
    setIsLoading(false)

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
