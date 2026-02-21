import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { useAuth } from '../hooks/useAuth'
import { authService } from '../services/authService'

const loginSchema = z.object({
  email: z.string().email('auth.validation.email_invalid').min(1, 'auth.validation.email_required'),
  password: z.string().min(1, 'auth.validation.password_required'),
})

type LoginFormData = z.infer<typeof loginSchema>
type FormErrors = Partial<Record<keyof LoginFormData, string>>

export default function LoginPage() {
  const { t, i18n } = useTranslation()
  const { login } = useAuth()
  const navigate = useNavigate()
  const [formData, setFormData] = useState<LoginFormData>({ email: '', password: '' })
  const [errors, setErrors] = useState<FormErrors>({})
  const [serverError, setServerError] = useState('')
  const [isLoading, setIsLoading] = useState(false)

  const validateForm = (): boolean => {
    const result = loginSchema.safeParse(formData)
    if (!result.success) {
      const fieldErrors: FormErrors = {}
      result.error.issues.forEach((issue) => {
        const field = issue.path[0] as keyof LoginFormData
        fieldErrors[field] = t(issue.message)
      })
      setErrors(fieldErrors)
      return false
    }
    setErrors({})
    return true
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setServerError('')
    if (!validateForm()) return

    setIsLoading(true)
    try {
      const response = await authService.login(formData)
      const tokens = {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        expiresIn: response.expiresIn,
      }
      login(tokens, response.user)
      navigate('/dashboard')
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number } }
      if (axiosError.response?.status === 423) {
        setServerError(t('auth.account_locked'))
      } else if (axiosError.response?.status === 429) {
        setServerError(t('auth.rate_limit_exceeded'))
      } else {
        setServerError(t('auth.login_error'))
      }
    } finally {
      setIsLoading(false)
    }
  }

  const toggleLanguage = () => {
    const newLang = i18n.language === 'de' ? 'en' : 'de'
    void i18n.changeLanguage(newLang)
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="absolute top-4 right-4">
        <button
          onClick={toggleLanguage}
          className="px-3 py-1 text-sm text-gray-600 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
          aria-label={t('auth.switch_language')}
        >
          {i18n.language === 'de' ? 'EN' : 'DE'}
        </button>
      </div>
      <div className="max-w-md w-full space-y-8 p-8 bg-white rounded-xl shadow-lg">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-primary-700">{t('app.name')}</h1>
          <p className="mt-2 text-gray-600">{t('app.tagline')}</p>
        </div>
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          {serverError && (
            <div
              role="alert"
              className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg text-sm"
            >
              {serverError}
            </div>
          )}
          <div>
            <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
              {t('auth.email')}
            </label>
            <input
              id="email"
              type="email"
              value={formData.email}
              onChange={(e) => setFormData((prev) => ({ ...prev, email: e.target.value }))}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.email ? 'border-red-400' : 'border-gray-300'
              }`}
              aria-invalid={!!errors.email}
              aria-describedby={errors.email ? 'email-error' : undefined}
            />
            {errors.email && (
              <p id="email-error" className="mt-1 text-sm text-red-600">
                {errors.email}
              </p>
            )}
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
              {t('auth.password')}
            </label>
            <input
              id="password"
              type="password"
              value={formData.password}
              onChange={(e) => setFormData((prev) => ({ ...prev, password: e.target.value }))}
              className={`w-full px-3 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500 ${
                errors.password ? 'border-red-400' : 'border-gray-300'
              }`}
              aria-invalid={!!errors.password}
              aria-describedby={errors.password ? 'password-error' : undefined}
            />
            {errors.password && (
              <p id="password-error" className="mt-1 text-sm text-red-600">
                {errors.password}
              </p>
            )}
          </div>
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-2 px-4 bg-primary-600 text-white rounded-lg hover:bg-primary-700 focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-50 transition-colors"
          >
            {isLoading ? t('common.loading') : t('auth.login_button')}
          </button>
        </form>
      </div>
    </div>
  )
}
