import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { timeModificationService } from '../services/timeModificationService'
import type { TimeModificationResponse } from '../services/timeModificationService'
import { XCircle } from 'lucide-react'
import { useDateFormat } from '../context/DateFormatContext'
import { formatDateTime } from '../utils/dateUtils'

type Tab = 'list' | 'new_request'
type TMStatus = TimeModificationResponse['status']

function StatusBadge({ status }: { status: TMStatus }) {
  const { t } = useTranslation()
  const styles: Record<TMStatus, string> = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
    CANCELLED: 'bg-gray-100 text-gray-700',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status]}`}>
      {t(`time_modification.status.${status.toLowerCase()}`)}
    </span>
  )
}

export default function TimeModificationPage() {
  const { t } = useTranslation()
  const { dateFormat, timeFormat } = useDateFormat()
  const [activeTab, setActiveTab] = useState<Tab>('list')
  const [requests, setRequests] = useState<TimeModificationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const [formData, setFormData] = useState({
    timeEntryId: '',
    requestedTimestamp: '',
    requestedNotes: '',
    reason: '',
  })

  const loadRequests = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await timeModificationService.getMyRequests(0, 100)
      setRequests(result.content)
    } catch {
      setError(t('time_modification.errors.load_failed'))
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
      await timeModificationService.createRequest({
        timeEntryId: formData.timeEntryId,
        requestedTimestamp: new Date(formData.requestedTimestamp).toISOString(),
        requestedNotes: formData.requestedNotes || undefined,
        reason: formData.reason,
      })
      setSuccess(t('time_modification.success.created'))
      setFormData({ timeEntryId: '', requestedTimestamp: '', requestedNotes: '', reason: '' })
      setActiveTab('list')
      await loadRequests()
    } catch {
      setError(t('time_modification.errors.create_failed'))
    }
  }

  const handleCancel = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await timeModificationService.cancelRequest(id)
      setSuccess(t('time_modification.success.cancelled'))
      await loadRequests()
    } catch {
      setError(t('time_modification.errors.cancel_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: 'list', label: t('time_modification.tabs.list') },
    { key: 'new_request', label: t('time_modification.tabs.new_request') },
  ]

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('time_modification.title')}</h1>

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
          <p className="text-gray-500">{t('time_modification.no_entries')}</p>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {['entry_type', 'original_timestamp', 'requested_timestamp', 'reason', 'status', 'actions'].map((col) => (
                    <th key={col} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      {t(`time_modification.columns.${col}`)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {requests.map((req) => (
                  <tr key={req.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{req.entryType}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{formatDateTime(req.originalTimestamp, dateFormat, timeFormat)}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{formatDateTime(req.requestedTimestamp, dateFormat, timeFormat)}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">{req.reason}</td>
                    <td className="px-4 py-3"><StatusBadge status={req.status} /></td>
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
                          {t('time_modification.see_reason')}
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
          <div>
            <label htmlFor="timeEntryId" className="block text-sm font-medium text-gray-700 mb-1">
              {t('time_modification.time_entry')} *
            </label>
            <input
              id="timeEntryId"
              type="text"
              required
              placeholder="Time entry UUID"
              value={formData.timeEntryId}
              onChange={(e) => setFormData((f) => ({ ...f, timeEntryId: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="requestedTimestamp" className="block text-sm font-medium text-gray-700 mb-1">
              {t('time_modification.requested_timestamp')} *
            </label>
            <input
              id="requestedTimestamp"
              type="datetime-local"
              required
              value={formData.requestedTimestamp}
              onChange={(e) => setFormData((f) => ({ ...f, requestedTimestamp: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="requestedNotes" className="block text-sm font-medium text-gray-700 mb-1">
              {t('time_modification.requested_notes')}
            </label>
            <input
              id="requestedNotes"
              type="text"
              value={formData.requestedNotes}
              onChange={(e) => setFormData((f) => ({ ...f, requestedNotes: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="reason" className="block text-sm font-medium text-gray-700 mb-1">
              {t('time_modification.reason')} *
            </label>
            <textarea
              id="reason"
              required
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
            {t('time_modification.submit')}
          </button>
        </form>
      )}
    </div>
  )
}
