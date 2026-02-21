import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'
import type { User, AuthTokens } from '../types'

interface AuthContextValue {
  user: User | null
  tokens: AuthTokens | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (tokens: AuthTokens, user: User) => void
  logout: () => void
  hasPermission: (permission: string) => boolean
  hasRole: (role: string) => boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [tokens, setTokens] = useState<AuthTokens | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    const storedTokens = localStorage.getItem('auth_tokens')
    const storedUser = localStorage.getItem('auth_user')
    if (storedTokens && storedUser) {
      setTokens(JSON.parse(storedTokens))
      setUser(JSON.parse(storedUser))
    }
    setIsLoading(false)
  }, [])

  const login = useCallback((newTokens: AuthTokens, newUser: User) => {
    setTokens(newTokens)
    setUser(newUser)
    localStorage.setItem('auth_tokens', JSON.stringify(newTokens))
    localStorage.setItem('auth_user', JSON.stringify(newUser))
  }, [])

  const logout = useCallback(() => {
    setTokens(null)
    setUser(null)
    localStorage.removeItem('auth_tokens')
    localStorage.removeItem('auth_user')
  }, [])

  const hasPermission = useCallback(
    (permission: string) => {
      return user?.permissions.includes(permission) ?? false
    },
    [user]
  )

  const hasRole = useCallback(
    (role: string) => {
      return user?.roles.includes(role) ?? false
    },
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
        hasPermission,
        hasRole,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
