import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { workHourChangeService } from '../services/workHourChangeService'
import type { WorkHourChangeResponse } from '../services/workHourChangeService'
import { XCircle } from 'lucide-react'

type Tab = 'list' | 'new_request'
type WHCStatus = WorkHourChangeResponse['status']

function StatusBadge({ status }: { status: WHCStatus }) {
  const { t } = useTranslation()
  const styles: Record<WHCStatus, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
    CANCELLED: 'bg-gray-100 text-gray-700',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status]}`}>
      {t(`work_hour_change.status.${status.toLowerCase()}`)}
    </span>
  )
}

export default function WorkHourChangePage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('list')
  const [requests, setRequests] = useState<WorkHourChangeResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const [formData, setFormData] = useState({
    requestedWeeklyHours: '',
    requestedDailyHours: '',
    effectiveDate: '',
    reason: '',
  })

  const loadRequests = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await workHourChangeService.getMyRequests(0, 100)
      setRequests(result.content)
    } catch {
      setError(t('work_hour_change.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadRequests()
  }, [loadRequests])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await workHourChangeService.createRequest({
        requestedWeeklyHours: parseFloat(formData.requestedWeeklyHours),
        requestedDailyHours: formData.requestedDailyHours ? parseFloat(formData.requestedDailyHours) : undefined,
        effectiveDate: formData.effectiveDate,
        reason: formData.reason || undefined,
      })
      setSuccess(t('work_hour_change.success.created'))
      setFormData({ requestedWeeklyHours: '', requestedDailyHours: '', effectiveDate: '', reason: '' })
      setActiveTab('list')
      await loadRequests()
    } catch {
      setError(t('work_hour_change.errors.create_failed'))
    }
  }

  const handleCancel = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await workHourChangeService.cancelRequest(id)
      setSuccess(t('work_hour_change.success.cancelled'))
      await loadRequests()
    } catch {
      setError(t('work_hour_change.errors.cancel_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: 'list', label: t('work_hour_change.tabs.list') },
    { key: 'new_request', label: t('work_hour_change.tabs.new_request') },
  ]

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('work_hour_change.title')}</h1>

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}
      {success && (
        <div className="mb-4 p-3 bg-green-50 border border-green-200 rounded text-green-700 text-sm">
          {success}
        </div>
      )}

      <div className="flex items-center gap-2 mb-6">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`px-4 py-2 text-sm font-medium rounded-lg transition-colors ${
              activeTab === tab.key ? 'bg-primary-600 text-white' : 'text-gray-600 hover:bg-gray-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'list' && (
        loading ? (
          <p aria-live="polite" className="text-gray-500">{t('common.loading')}</p>
        ) : requests.length === 0 ? (
          <p className="text-gray-500">{t('work_hour_change.no_entries')}</p>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {['current_hours', 'requested_hours', 'effective_date', 'status', 'reason', 'actions'].map((col) => (
                    <th key={col} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t(`work_hour_change.columns.${col}`)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {requests.map((req) => (
                  <tr key={req.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{req.currentWeeklyHours}h</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{req.requestedWeeklyHours}h</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{req.effectiveDate}</td>
                    <td className="px-4 py-3"><StatusBadge status={req.status} /></td>
                    <td className="px-4 py-3 text-sm text-gray-500">{req.reason ?? '—'}</td>
                    <td className="px-4 py-3">
                      {req.status === 'PENDING' && (
                        <button
                          onClick={() => handleCancel(req.id)}
                          disabled={actionLoading === req.id}
                          className="inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                        >
                          <XCircle size={14} />
                          {t('common.cancel')}
                        </button>
                      )}
                      {req.status === 'REJECTED' && req.rejectionReason && (
                        <span className="text-xs text-gray-500" title={req.rejectionReason}>
                          {t('work_hour_change.see_reason')}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {activeTab === 'new_request' && (
        <form onSubmit={handleSubmit} className="bg-white shadow rounded-lg p-6 max-w-lg space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="reqWeeklyHours" className="block text-sm font-medium text-gray-700 mb-1">
                {t('work_hour_change.requested_weekly_hours')} *
              </label>
              <input
                id="reqWeeklyHours"
                type="number"
                step="0.5"
                min="0"
                max="60"
                required
                value={formData.requestedWeeklyHours}
                onChange={(e) => setFormData((f) => ({ ...f, requestedWeeklyHours: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label htmlFor="reqDailyHours" className="block text-sm font-medium text-gray-700 mb-1">
                {t('work_hour_change.requested_daily_hours')}
              </label>
              <input
                id="reqDailyHours"
                type="number"
                step="0.5"
                min="0"
                max="12"
                value={formData.requestedDailyHours}
                onChange={(e) => setFormData((f) => ({ ...f, requestedDailyHours: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div>
            <label htmlFor="effectiveDate" className="block text-sm font-medium text-gray-700 mb-1">
              {t('work_hour_change.effective_date')} *
            </label>
            <input
              id="effectiveDate"
              type="date"
              required
              value={formData.effectiveDate}
              onChange={(e) => setFormData((f) => ({ ...f, effectiveDate: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="reason" className="block text-sm font-medium text-gray-700 mb-1">
              {t('work_hour_change.reason')}
            </label>
            <textarea
              id="reason"
              value={formData.reason}
              onChange={(e) => setFormData((f) => ({ ...f, reason: e.target.value }))}
              rows={3}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <button
            type="submit"
            className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded hover:bg-primary-700"
          >
            {t('work_hour_change.submit')}
          </button>
        </form>
      )}
    </div>
  )
}
