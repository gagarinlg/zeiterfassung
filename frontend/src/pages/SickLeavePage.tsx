import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { sickLeaveService } from '../services/sickLeaveService'
import type { SickLeaveResponse } from '../services/sickLeaveService'
import { FileText, XCircle, Upload } from 'lucide-react'

type Tab = 'list' | 'report'
type SickLeaveStatus = SickLeaveResponse['status']

function StatusBadge({ status }: { status: SickLeaveStatus }) {
  const { t } = useTranslation()
  const styles: Record<SickLeaveStatus, string> = {
    REPORTED: 'bg-blue-100 text-blue-800',
    CERTIFICATE_PENDING: 'bg-yellow-100 text-yellow-800',
    CERTIFICATE_RECEIVED: 'bg-green-100 text-green-800',
    CANCELLED: 'bg-gray-100 text-gray-700',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status]}`}>
      {t(`sick_leave.status.${status.toLowerCase()}`)}
    </span>
  )
}

export default function SickLeavePage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('list')
  const [sickLeaves, setSickLeaves] = useState<SickLeaveResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const [formData, setFormData] = useState({
    startDate: '',
    endDate: '',
    notes: '',
  })

  const loadSickLeaves = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await sickLeaveService.getMySickLeaves(0, 100)
      setSickLeaves(result.content)
    } catch {
      setError(t('sick_leave.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadSickLeaves()
  }, [loadSickLeaves])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await sickLeaveService.reportSickLeave({
        startDate: formData.startDate,
        endDate: formData.endDate,
        notes: formData.notes || undefined,
      })
      setSuccess(t('sick_leave.success.reported'))
      setFormData({ startDate: '', endDate: '', notes: '' })
      setActiveTab('list')
      await loadSickLeaves()
    } catch {
      setError(t('sick_leave.errors.report_failed'))
    }
  }

  const handleCancel = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await sickLeaveService.cancelSickLeave(id)
      setSuccess(t('sick_leave.success.cancelled'))
      await loadSickLeaves()
    } catch {
      setError(t('sick_leave.errors.cancel_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const handleSubmitCertificate = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await sickLeaveService.submitCertificate(id)
      setSuccess(t('sick_leave.success.certificate_submitted'))
      await loadSickLeaves()
    } catch {
      setError(t('sick_leave.errors.certificate_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: 'list', label: t('sick_leave.tabs.list') },
    { key: 'report', label: t('sick_leave.tabs.report') },
  ]

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('sick_leave.title')}</h1>

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}
      {success && (
        <div role="status" className="mb-4 p-3 bg-green-50 border border-green-200 rounded text-green-700 text-sm">
          {success}
        </div>
      )}

      <nav className="border-b border-gray-200 mb-6">
        <div className="flex gap-6">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => { setActiveTab(tab.key); setSuccess(null) }}
              className={`pb-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </nav>

      {activeTab === 'list' && (
        loading ? (
          <p aria-live="polite" className="text-gray-500">{t('common.loading')}</p>
        ) : sickLeaves.length === 0 ? (
          <p className="text-gray-500">{t('sick_leave.no_entries')}</p>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {['start_date', 'end_date', 'status', 'certificate', 'notes', 'actions'].map((col) => (
                    <th
                      key={col}
                      className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      {t(`sick_leave.columns.${col}`)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {sickLeaves.map((sl) => (
                  <tr key={sl.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{sl.startDate}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{sl.endDate}</td>
                    <td className="px-4 py-3"><StatusBadge status={sl.status} /></td>
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {sl.hasCertificate ? (
                        <span className="inline-flex items-center gap-1 text-green-700">
                          <FileText size={14} />
                          {t('sick_leave.certificate_yes')}
                        </span>
                      ) : (
                        <span className="text-gray-400">{t('sick_leave.certificate_no')}</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500">{sl.notes ?? '—'}</td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {sl.status !== 'CANCELLED' && sl.status !== 'CERTIFICATE_RECEIVED' && !sl.hasCertificate && (
                          <button
                            onClick={() => handleSubmitCertificate(sl.id)}
                            disabled={actionLoading === sl.id}
                            className="inline-flex items-center gap-1 text-xs font-medium text-blue-700 hover:text-blue-900 disabled:opacity-50"
                            title={t('sick_leave.submit_certificate')}
                          >
                            <Upload size={14} />
                            {t('sick_leave.submit_certificate')}
                          </button>
                        )}
                        {sl.status !== 'CANCELLED' && (
                          <button
                            onClick={() => handleCancel(sl.id)}
                            disabled={actionLoading === sl.id}
                            className="inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                            title={t('common.cancel')}
                          >
                            <XCircle size={14} />
                            {t('common.cancel')}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {activeTab === 'report' && (
        <form onSubmit={handleSubmit} className="bg-white shadow rounded-lg p-6 max-w-lg space-y-4">
          <div>
            <label htmlFor="startDate" className="block text-sm font-medium text-gray-700 mb-1">
              {t('sick_leave.start_date')}
            </label>
            <input
              id="startDate"
              type="date"
              required
              value={formData.startDate}
              onChange={(e) => setFormData((f) => ({ ...f, startDate: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="endDate" className="block text-sm font-medium text-gray-700 mb-1">
              {t('sick_leave.end_date')}
            </label>
            <input
              id="endDate"
              type="date"
              required
              value={formData.endDate}
              onChange={(e) => setFormData((f) => ({ ...f, endDate: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="notes" className="block text-sm font-medium text-gray-700 mb-1">
              {t('sick_leave.notes')}
            </label>
            <textarea
              id="notes"
              value={formData.notes}
              onChange={(e) => setFormData((f) => ({ ...f, notes: e.target.value }))}
              rows={3}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <button
            type="submit"
            className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded hover:bg-primary-700"
          >
            {t('sick_leave.report_button')}
          </button>
        </form>
      )}
    </div>
  )
}
