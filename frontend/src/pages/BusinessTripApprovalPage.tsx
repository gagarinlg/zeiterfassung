import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { businessTripService } from '../services/businessTripService'
import type { BusinessTripResponse } from '../services/businessTripService'
import { CheckCircle, XCircle } from 'lucide-react'

type BusinessTripStatus = BusinessTripResponse['status']

function StatusBadge({ status }: { status: BusinessTripStatus }) {
  const { t } = useTranslation()
  const styles: Record<BusinessTripStatus, string> = {
    REQUESTED: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
    CANCELLED: 'bg-gray-100 text-gray-700',
    COMPLETED: 'bg-blue-100 text-blue-800',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status]}`}>
      {t(`business_trip.status.${status.toLowerCase()}`)}
    </span>
  )
}

export default function BusinessTripApprovalPage() {
  const { t } = useTranslation()
  const [trips, setTrips] = useState<BusinessTripResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [rejectModal, setRejectModal] = useState<{ id: string; reason: string } | null>(null)

  const loadPendingTrips = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await businessTripService.getPendingTrips(0, 100)
      setTrips(result.content)
    } catch {
      setError(t('business_trip.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadPendingTrips()
  }, [loadPendingTrips])

  const handleApprove = async (id: string) => {
    setActionLoading(id)
    try {
      await businessTripService.approveTrip(id)
      await loadPendingTrips()
    } catch {
      setError(t('business_trip.errors.approve_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const handleRejectConfirm = async () => {
    if (!rejectModal || !rejectModal.reason.trim()) return
    setActionLoading(rejectModal.id)
    try {
      await businessTripService.rejectTrip(rejectModal.id, rejectModal.reason)
      setRejectModal(null)
      await loadPendingTrips()
    } catch {
      setError(t('business_trip.errors.reject_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('business_trip.approval.title')}</h1>

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}

      {loading ? (
        <p aria-live="polite" className="text-gray-500">{t('common.loading')}</p>
      ) : trips.length === 0 ? (
        <p className="text-gray-500">{t('business_trip.approval.no_pending')}</p>
      ) : (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                {['employee', 'destination', 'start_date', 'end_date', 'purpose', 'estimated_cost', 'status', 'actions'].map((col) => (
                  <th
                    key={col}
                    className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                  >
                    {t(`business_trip.approval.${col}`)}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {trips.map((trip) => (
                <tr key={trip.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-sm font-medium text-gray-900">{trip.userName}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{trip.destination}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{trip.startDate}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">{trip.endDate}</td>
                  <td className="px-4 py-3 text-sm text-gray-500 max-w-xs truncate">{trip.purpose}</td>
                  <td className="px-4 py-3 text-sm text-gray-900">
                    {trip.estimatedCost != null ? `€${trip.estimatedCost}` : '—'}
                  </td>
                  <td className="px-4 py-3"><StatusBadge status={trip.status} /></td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleApprove(trip.id)}
                        disabled={actionLoading === trip.id}
                        className="inline-flex items-center gap-1 text-xs font-medium text-green-700 hover:text-green-900 disabled:opacity-50"
                        title={t('business_trip.approval.approve')}
                      >
                        <CheckCircle size={16} />
                        {t('business_trip.approval.approve')}
                      </button>
                      <button
                        onClick={() => setRejectModal({ id: trip.id, reason: '' })}
                        disabled={actionLoading === trip.id}
                        className="inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                        title={t('business_trip.approval.reject')}
                      >
                        <XCircle size={16} />
                        {t('business_trip.approval.reject')}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Reject Modal */}
      {rejectModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('business_trip.approval.reject_title')}</h2>
            <textarea
              value={rejectModal.reason}
              onChange={(e) => setRejectModal((m) => m ? { ...m, reason: e.target.value } : null)}
              placeholder={t('business_trip.approval.rejection_reason')}
              rows={4}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm mb-4"
            />
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setRejectModal(null)}
                className="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded hover:bg-gray-50"
              >
                {t('common.cancel')}
              </button>
              <button
                onClick={handleRejectConfirm}
                disabled={!rejectModal.reason.trim() || actionLoading === rejectModal.id}
                className="px-4 py-2 text-sm font-medium text-white bg-red-600 rounded hover:bg-red-700 disabled:opacity-50"
              >
                {t('business_trip.approval.reject')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
