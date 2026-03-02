import { useState, useCallback, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { timeModificationService } from '../services/timeModificationService'
import type { TimeModificationResponse } from '../services/timeModificationService'
import { CheckCircle, XCircle } from 'lucide-react'
import { useDateFormat } from '../context/DateFormatContext'
import { formatDateTime } from '../utils/dateUtils'

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

export default function TimeModificationApprovalPage() {
  const { t } = useTranslation()
  const { dateFormat, timeFormat } = useDateFormat()
  const [requests, setRequests] = useState<TimeModificationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [rejectModal, setRejectModal] = useState<{ id: string; reason: string } | null>(null)

  const loadRequests = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await timeModificationService.getPendingRequests()
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

  const handleApprove = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await timeModificationService.approveRequest(id)
      setSuccess(t('time_modification.success.approved'))
      await loadRequests()
    } catch {
      setError(t('time_modification.errors.approve_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const handleReject = async () => {
    if (!rejectModal || !rejectModal.reason.trim()) return
    setActionLoading(rejectModal.id)
    setError(null)
    try {
      await timeModificationService.rejectRequest(rejectModal.id, rejectModal.reason)
      setRejectModal(null)
      setSuccess(t('time_modification.success.rejected'))
      await loadRequests()
    } catch {
      setError(t('time_modification.errors.reject_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('time_modification.approvals_title')}</h1>

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

      {loading ? (
        <p aria-live="polite" className="text-gray-500">{t('common.loading')}</p>
      ) : requests.length === 0 ? (
        <p className="text-gray-500">{t('time_modification.no_pending')}</p>
      ) : (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {['employee', 'entry_type', 'original_timestamp', 'requested_timestamp', 'reason', 'status', 'actions'].map((col) => (
                  <th key={col} className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    {t(`time_modification.columns.${col}`)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {requests.map((req) => (
                <tr key={req.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{req.userName}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{req.entryType}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{formatDateTime(req.originalTimestamp, dateFormat, timeFormat)}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{formatDateTime(req.requestedTimestamp, dateFormat, timeFormat)}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">{req.reason}</td>
                  <td className="px-4 py-3"><StatusBadge status={req.status} /></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleApprove(req.id)}
                        disabled={actionLoading === req.id}
                        className="inline-flex items-center gap-1 text-xs font-medium text-green-700 hover:text-green-900 disabled:opacity-50"
                        title={t('common.approve')}
                      >
                        <CheckCircle size={16} />
                        {t('common.approve')}
                      </button>
                      <button
                        onClick={() => setRejectModal({ id: req.id, reason: '' })}
                        disabled={actionLoading === req.id}
                        className="inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                        title={t('common.reject')}
                      >
                        <XCircle size={16} />
                        {t('common.reject')}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {rejectModal && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('time_modification.reject_title')}</h2>
            <textarea
              value={rejectModal.reason}
              onChange={(e) => setRejectModal({ ...rejectModal, reason: e.target.value })}
              rows={3}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm mb-4"
              placeholder={t('time_modification.reject_reason_placeholder')}
              autoFocus
            />
            <div className="flex justify-end gap-3">
              <button onClick={() => setRejectModal(null)} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">
                {t('common.cancel')}
              </button>
              <button
                onClick={handleReject}
                disabled={!rejectModal.reason.trim() || actionLoading === rejectModal.id}
                className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
              >
                {t('common.reject')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
