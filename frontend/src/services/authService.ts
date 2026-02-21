import apiClient from './apiClient'
import type { LoginRequest, AuthTokens, User } from '../types'

interface LoginResponse {
  tokens: AuthTokens
  user: User
}

export const authService = {
  async login(request: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>('/auth/login', request)
    return response.data
  },

  async logout(): Promise<void> {
    await apiClient.post('/auth/logout')
  },

  async refreshToken(refreshToken: string): Promise<AuthTokens> {
    const response = await apiClient.post<AuthTokens>('/auth/refresh', { refreshToken })
    return response.data
  },

  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/auth/me')
    return response.data
  },
}
