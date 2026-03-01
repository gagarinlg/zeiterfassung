import apiClient from './apiClient'
import type { LoginRequest, AuthTokens, User } from '../types'

interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: User
}

export const authService = {
  async login(request: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/auth/login', request)
    return response.data
  },

  async logout(refreshToken?: string): Promise<void> {
    await apiClient.post('/auth/logout', refreshToken ? { refreshToken } : {})
  },

  async refreshToken(refreshToken: string): Promise<AuthTokens> {
    const response = await apiClient.post<AuthTokens>('/auth/refresh', { refreshToken })
    return response.data
  },

  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/auth/me')
    return response.data
  },

  // TOTP 2FA
  setupTotp: () =>
    apiClient.post('/auth/totp/setup').then((r) => r.data),

  enableTotp: (secret: string, code: string) =>
    apiClient.post(`/auth/totp/enable?secret=${encodeURIComponent(secret)}`, { code }).then((r) => r.data),

  disableTotp: () =>
    apiClient.post('/auth/totp/disable').then((r) => r.data),

  // Password reset
  requestPasswordReset: (email: string) =>
    apiClient.post('/auth/password/reset-request', { email }).then((r) => r.data),

  confirmPasswordReset: (token: string, newPassword: string, confirmPassword: string) =>
    apiClient.post('/auth/password/reset-confirm', { token, newPassword, confirmPassword }).then((r) => r.data),
}
