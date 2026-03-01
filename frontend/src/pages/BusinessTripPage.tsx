import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { businessTripService } from '../services/businessTripService'
import type { BusinessTripResponse } from '../services/businessTripService'
import { XCircle, CheckCircle } from 'lucide-react'

type Tab = 'list' | 'new_request'
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

export default function BusinessTripPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('list')
  const [trips, setTrips] = useState<BusinessTripResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)
  const [completeModal, setCompleteModal] = useState<{ id: string; actualCost: string } | null>(null)

  const [formData, setFormData] = useState({
    startDate: '',
    endDate: '',
    destination: '',
    purpose: '',
    notes: '',
    estimatedCost: '',
    costCenter: '',
  })

  const loadTrips = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await businessTripService.getMyTrips(0, 100)
      setTrips(result.content)
    } catch {
      setError(t('business_trip.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadTrips()
  }, [loadTrips])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await businessTripService.createTrip({
        startDate: formData.startDate,
        endDate: formData.endDate,
        destination: formData.destination,
        purpose: formData.purpose,
        notes: formData.notes || undefined,
        estimatedCost: formData.estimatedCost ? parseFloat(formData.estimatedCost) : undefined,
        costCenter: formData.costCenter || undefined,
      })
      setSuccess(t('business_trip.success.created'))
      setFormData({ startDate: '', endDate: '', destination: '', purpose: '', notes: '', estimatedCost: '', costCenter: '' })
      setActiveTab('list')
      await loadTrips()
    } catch {
      setError(t('business_trip.errors.create_failed'))
    }
  }

  const handleCancel = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await businessTripService.cancelTrip(id)
      setSuccess(t('business_trip.success.cancelled'))
      await loadTrips()
    } catch {
      setError(t('business_trip.errors.cancel_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const handleCompleteConfirm = async () => {
    if (!completeModal) return
    setActionLoading(completeModal.id)
    setError(null)
    try {
      const actualCost = completeModal.actualCost ? parseFloat(completeModal.actualCost) : undefined
      await businessTripService.completeTrip(completeModal.id, actualCost)
      setCompleteModal(null)
      setSuccess(t('business_trip.success.completed'))
      await loadTrips()
    } catch {
      setError(t('business_trip.errors.complete_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const tabs: { key: Tab; label: string }[] = [
    { key: 'list', label: t('business_trip.tabs.list') },
    { key: 'new_request', label: t('business_trip.tabs.new_request') },
  ]

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('business_trip.title')}</h1>

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
        ) : trips.length === 0 ? (
          <p className="text-gray-500">{t('business_trip.no_entries')}</p>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {['destination', 'start_date', 'end_date', 'purpose', 'status', 'estimated_cost', 'actions'].map((col) => (
                    <th
                      key={col}
                      className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      {t(`business_trip.columns.${col}`)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {trips.map((trip) => (
                  <tr key={trip.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">{trip.destination}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{trip.startDate}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{trip.endDate}</td>
                    <td className="px-4 py-3 text-sm text-gray-500 max-w-xs truncate">{trip.purpose}</td>
                    <td className="px-4 py-3"><StatusBadge status={trip.status} /></td>
                    <td className="px-4 py-3 text-sm text-gray-900">
                      {trip.estimatedCost != null ? `€${trip.estimatedCost}` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {trip.status === 'APPROVED' && (
                          <button
                            onClick={() => setCompleteModal({ id: trip.id, actualCost: '' })}
                            disabled={actionLoading === trip.id}
                            className="inline-flex items-center gap-1 text-xs font-medium text-blue-700 hover:text-blue-900 disabled:opacity-50"
                          >
                            <CheckCircle size={14} />
                            {t('business_trip.complete')}
                          </button>
                        )}
                        {(trip.status === 'REQUESTED' || trip.status === 'APPROVED') && (
                          <button
                            onClick={() => handleCancel(trip.id)}
                            disabled={actionLoading === trip.id}
                            className="inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
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

      {activeTab === 'new_request' && (
        <form onSubmit={handleSubmit} className="bg-white shadow rounded-lg p-6 max-w-lg space-y-4">
          <div>
            <label htmlFor="destination" className="block text-sm font-medium text-gray-700 mb-1">
              {t('business_trip.destination')} *
            </label>
            <input
              id="destination"
              type="text"
              required
              value={formData.destination}
              onChange={(e) => setFormData((f) => ({ ...f, destination: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="purpose" className="block text-sm font-medium text-gray-700 mb-1">
              {t('business_trip.purpose')} *
            </label>
            <input
              id="purpose"
              type="text"
              required
              value={formData.purpose}
              onChange={(e) => setFormData((f) => ({ ...f, purpose: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="tripStartDate" className="block text-sm font-medium text-gray-700 mb-1">
                {t('business_trip.start_date')} *
              </label>
              <input
                id="tripStartDate"
                type="date"
                required
                value={formData.startDate}
                onChange={(e) => setFormData((f) => ({ ...f, startDate: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label htmlFor="tripEndDate" className="block text-sm font-medium text-gray-700 mb-1">
                {t('business_trip.end_date')} *
              </label>
              <input
                id="tripEndDate"
                type="date"
                required
                value={formData.endDate}
                onChange={(e) => setFormData((f) => ({ ...f, endDate: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="estimatedCost" className="block text-sm font-medium text-gray-700 mb-1">
                {t('business_trip.estimated_cost')}
              </label>
              <input
                id="estimatedCost"
                type="number"
                step="0.01"
                min="0"
                value={formData.estimatedCost}
                onChange={(e) => setFormData((f) => ({ ...f, estimatedCost: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label htmlFor="costCenter" className="block text-sm font-medium text-gray-700 mb-1">
                {t('business_trip.cost_center')}
              </label>
              <input
                id="costCenter"
                type="text"
                value={formData.costCenter}
                onChange={(e) => setFormData((f) => ({ ...f, costCenter: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
          </div>
          <div>
            <label htmlFor="tripNotes" className="block text-sm font-medium text-gray-700 mb-1">
              {t('business_trip.notes')}
            </label>
            <textarea
              id="tripNotes"
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
            {t('business_trip.submit_button')}
          </button>
        </form>
      )}

      {/* Complete Trip Modal */}
      {completeModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
            <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('business_trip.complete_title')}</h2>
            <div className="mb-4">
              <label htmlFor="actualCost" className="block text-sm font-medium text-gray-700 mb-1">
                {t('business_trip.actual_cost')}
              </label>
              <input
                id="actualCost"
                type="number"
                step="0.01"
                min="0"
                value={completeModal.actualCost}
                onChange={(e) => setCompleteModal((m) => m ? { ...m, actualCost: e.target.value } : null)}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setCompleteModal(null)}
                className="px-4 py-2 text-sm font-medium text-gray-700 border border-gray-300 rounded hover:bg-gray-50"
              >
                {t('common.cancel')}
              </button>
              <button
                onClick={handleCompleteConfirm}
                disabled={actionLoading === completeModal.id}
                className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded hover:bg-primary-700 disabled:opacity-50"
              >
                {t('business_trip.complete')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
