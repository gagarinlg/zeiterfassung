import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'
import adminService from '../services/adminService'
import { authService } from '../services/authService'
import { QRCodeSVG } from 'qrcode.react'

export default function UserSettingsPage() {
  const { t } = useTranslation()
  const { user, refreshUser } = useAuth()
  const [dateFormat, setDateFormat] = useState(user?.dateFormat || 'DD.MM.YYYY')
  const [timeFormat, setTimeFormat] = useState(user?.timeFormat || '24h')
  const [saving, setSaving] = useState(false)
  const [success, setSuccess] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  // TOTP 2FA
  const [totpSetupData, setTotpSetupData] = useState<{ secret: string; qrCodeUri: string } | null>(null)
  const [totpCode, setTotpCode] = useState('')
  const [totpSaving, setTotpSaving] = useState(false)
  const [totpSuccess, setTotpSuccess] = useState<string | null>(null)
  const [totpError, setTotpError] = useState<string | null>(null)

  // Password change
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [passwordSaving, setPasswordSaving] = useState(false)
  const [passwordSuccess, setPasswordSuccess] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)

  if (!user) return null

  const handleSavePreferences = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      await adminService.updateOwnProfile({ dateFormat, timeFormat })
      setSuccess(t('settings.preferences_saved'))
      if (refreshUser) refreshUser()
    } catch {
      setError(t('admin.errors.save_failed'))
    } finally {
      setSaving(false)
    }
  }

  const handleSetupTotp = async () => {
    setTotpSaving(true)
    setTotpError(null)
    setTotpSuccess(null)
    try {
      const data = await authService.setupTotp()
      setTotpSetupData(data)
    } catch {
      setTotpError(t('settings.totp_error'))
    } finally {
      setTotpSaving(false)
    }
  }

  const handleEnableTotp = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!totpSetupData) return
    setTotpSaving(true)
    setTotpError(null)
    setTotpSuccess(null)
    try {
      await authService.enableTotp(totpSetupData.secret, totpCode)
      setTotpSuccess(t('settings.totp_enabled_success'))
      setTotpSetupData(null)
      setTotpCode('')
      if (refreshUser) refreshUser()
    } catch {
      setTotpError(t('settings.totp_error'))
    } finally {
      setTotpSaving(false)
    }
  }

  const handleDisableTotp = async () => {
    setTotpSaving(true)
    setTotpError(null)
    setTotpSuccess(null)
    try {
      await authService.disableTotp()
      setTotpSuccess(t('settings.totp_disabled_success'))
      if (refreshUser) refreshUser()
    } catch {
      setTotpError(t('settings.totp_error'))
    } finally {
      setTotpSaving(false)
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()
    if (newPassword !== confirmPassword) {
      setPasswordError(t('settings.passwords_no_match'))
      return
    }
    setPasswordSaving(true)
    setPasswordError(null)
    setPasswordSuccess(null)
    try {
      await adminService.changeOwnPassword(user.id, currentPassword, newPassword, confirmPassword)
      setPasswordSuccess(t('settings.password_changed'))
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
    } catch {
      setPasswordError(t('settings.password_change_failed'))
    } finally {
      setPasswordSaving(false)
    }
  }

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('settings.preferences')}</h1>

      {/* Display Preferences */}
      <div className="bg-white rounded-xl border p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('settings.display_preferences')}</h2>
        <form onSubmit={handleSavePreferences} className="space-y-4">
          <div>
            <label htmlFor="dateFormat" className="block text-sm font-medium text-gray-700 mb-1">
              {t('settings.date_format')}
            </label>
            <select
              id="dateFormat"
              value={dateFormat}
              onChange={(e) => setDateFormat(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
            >
              <option value="DD.MM.YYYY">{t('settings.date_format_options.DD.MM.YYYY')}</option>
              <option value="YYYY-MM-DD">{t('settings.date_format_options.YYYY-MM-DD')}</option>
              <option value="MM/DD/YYYY">{t('settings.date_format_options.MM/DD/YYYY')}</option>
            </select>
          </div>
          <div>
            <label htmlFor="timeFormat" className="block text-sm font-medium text-gray-700 mb-1">
              {t('settings.time_format')}
            </label>
            <select
              id="timeFormat"
              value={timeFormat}
              onChange={(e) => setTimeFormat(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
            >
              <option value="24h">{t('settings.time_format_options.24h')}</option>
              <option value="12h">{t('settings.time_format_options.12h')}</option>
            </select>
          </div>
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700" role="alert">
              {error}
            </div>
          )}
          {success && (
            <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
              {success}
            </div>
          )}
          <button
            type="submit"
            disabled={saving}
            className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {saving ? t('common.loading') : t('common.save')}
          </button>
        </form>
      </div>

      {/* Two-Factor Authentication */}
      <div className="bg-white rounded-xl border p-6 mb-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('settings.totp_title')}</h2>
        <p className="text-sm text-gray-600 mb-4">
          {user.totpEnabled ? t('settings.totp_enabled') : t('settings.totp_disabled')}
        </p>

        {!user.totpEnabled && !totpSetupData && (
          <button
            type="button"
            onClick={handleSetupTotp}
            disabled={totpSaving}
            className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {totpSaving ? t('common.loading') : t('settings.totp_setup')}
          </button>
        )}

        {totpSetupData && (
          <form onSubmit={handleEnableTotp} className="space-y-4">
            <p className="text-sm text-gray-600">{t('settings.totp_setup_instructions')}</p>
            <div className="flex justify-center p-4 bg-white border rounded-lg">
              <QRCodeSVG value={totpSetupData.qrCodeUri} size={200} level="M" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t('settings.totp_secret_label')}
              </label>
              <div className="text-sm font-mono bg-gray-50 border rounded-lg p-2">
                {totpSetupData.secret}
              </div>
            </div>
            <div>
              <label htmlFor="totpCode" className="block text-sm font-medium text-gray-700 mb-1">
                {t('settings.totp_verify_code')}
              </label>
              <input
                id="totpCode"
                type="text"
                value={totpCode}
                onChange={(e) => setTotpCode(e.target.value)}
                className="w-full border rounded-lg px-3 py-2 text-sm"
                required
                autoComplete="one-time-code"
              />
            </div>
            <button
              type="submit"
              disabled={totpSaving}
              className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
            >
              {totpSaving ? t('common.loading') : t('common.save')}
            </button>
          </form>
        )}

        {user.totpEnabled && (
          <button
            type="button"
            onClick={handleDisableTotp}
            disabled={totpSaving}
            className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
          >
            {totpSaving ? t('common.loading') : t('settings.totp_disable')}
          </button>
        )}

        {totpError && (
          <div className="mt-3 p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700" role="alert">
            {totpError}
          </div>
        )}
        {totpSuccess && (
          <div className="mt-3 p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
            {totpSuccess}
          </div>
        )}
      </div>

      {/* Change Password */}
      <div className="bg-white rounded-xl border p-6">
        <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('users.change_password')}</h2>
        <form onSubmit={handleChangePassword} className="space-y-4">
          <div>
            <label htmlFor="currentPassword" className="block text-sm font-medium text-gray-700 mb-1">
              {t('users.current_password')}
            </label>
            <input
              id="currentPassword"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              required
            />
          </div>
          <div>
            <label htmlFor="newPassword" className="block text-sm font-medium text-gray-700 mb-1">
              {t('users.new_password')}
            </label>
            <input
              id="newPassword"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              required
              minLength={8}
            />
          </div>
          <div>
            <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-1">
              {t('settings.confirm_password')}
            </label>
            <input
              id="confirmPassword"
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              required
              minLength={8}
            />
          </div>
          {passwordError && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700" role="alert">
              {passwordError}
            </div>
          )}
          {passwordSuccess && (
            <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
              {passwordSuccess}
            </div>
          )}
          <button
            type="submit"
            disabled={passwordSaving}
            className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {passwordSaving ? t('common.loading') : t('users.change_password')}
          </button>
        </form>
      </div>
    </div>
  )
}
