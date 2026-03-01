import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { authService } from '../services/authService'

export default function PasswordResetRequestPage() {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      await authService.requestPasswordReset(email)
    } catch {
      // Don't reveal whether the email exists
    } finally {
      setSubmitted(true)
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
      <div className="w-full max-w-md bg-white rounded-xl shadow-lg p-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">{t('auth.reset_password')}</h1>
        {submitted ? (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">{t('auth.reset_email_sent')}</p>
            <Link to="/login" className="text-sm text-primary-600 hover:underline">
              {t('auth.back_to_login')}
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <p className="text-sm text-gray-600 mb-4">{t('auth.reset_instructions')}</p>
            <div>
              <label htmlFor="reset-email" className="block text-sm font-medium text-gray-700 mb-1">
                {t('auth.email')}
              </label>
              <input
                id="reset-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm"
                required
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 text-sm font-medium"
            >
              {loading ? t('common.loading') : t('auth.send_reset_link')}
            </button>
            <Link to="/login" className="block text-center text-sm text-primary-600 hover:underline">
              {t('auth.back_to_login')}
            </Link>
          </form>
        )}
      </div>
    </div>
  )
}
