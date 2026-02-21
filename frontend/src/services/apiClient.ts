import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

let isRefreshing = false
let failedQueue: Array<{
  resolve: (value: string) => void
  reject: (error: unknown) => void
}> = []

function processQueue(error: unknown, token: string | null) {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token!)
    }
  })
  failedQueue = []
}

apiClient.interceptors.request.use((config) => {
  const tokens = localStorage.getItem('auth_tokens')
  if (tokens) {
    const parsed = JSON.parse(tokens) as { accessToken: string }
    config.headers.Authorization = `Bearer ${parsed.accessToken}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as typeof error.config & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return apiClient(originalRequest)
          })
          .catch((err) => Promise.reject(err))
      }

      originalRequest._retry = true
      isRefreshing = true

      const storedTokens = localStorage.getItem('auth_tokens')
      if (!storedTokens) {
        localStorage.removeItem('auth_user')
        window.location.href = '/login'
        return Promise.reject(error)
      }

      const { refreshToken } = JSON.parse(storedTokens) as { refreshToken: string }

      try {
        const response = await axios.post<{
          accessToken: string
          refreshToken: string
          expiresIn: number
        }>('/api/auth/refresh', { refreshToken })
        const { accessToken, refreshToken: newRefreshToken, expiresIn } = response.data

        const newTokens = { accessToken, refreshToken: newRefreshToken, expiresIn }
        localStorage.setItem('auth_tokens', JSON.stringify(newTokens))

        processQueue(null, accessToken)
        originalRequest.headers.Authorization = `Bearer ${accessToken}`
        return apiClient(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        localStorage.removeItem('auth_tokens')
        localStorage.removeItem('auth_user')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default apiClient
