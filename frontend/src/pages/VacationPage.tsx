import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'
import { vacationService, type CreateVacationRequestData, type PublicHoliday } from '../services/vacationService'
import type { VacationRequest, VacationBalance } from '../types'

type Tab = 'requests' | 'new_request' | 'calendar'

interface BalanceWithPending extends VacationBalance {
  pendingDays: number
}

function StatusBadge({ status }: { status: VacationRequest['status'] }) {
  const { t } = useTranslation()
  const styles: Record<VacationRequest['status'], string> = {
    PENDING: 'bg-yellow-100 text-yellow-800',
    APPROVED: 'bg-green-100 text-green-800',
    REJECTED: 'bg-red-100 text-red-800',
    CANCELLED: 'bg-gray-100 text-gray-700',
  }
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${styles[status]}`}>
      {t(`vacation.request.status.${status.toLowerCase()}`)}
    </span>
  )
}

export default function VacationPage() {
  const { t } = useTranslation()
  const { user, hasPermission } = useAuth()
  const currentYear = new Date().getFullYear()

  const [activeTab, setActiveTab] = useState<Tab>('requests')
  const [balance, setBalance] = useState<BalanceWithPending | null>(null)
  const [requests, setRequests] = useState<VacationRequest[]>([])
  const [holidays, setHolidays] = useState<PublicHoliday[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [yearFilter, setYearFilter] = useState<number>(currentYear)

  // New request form state
  const [formData, setFormData] = useState<CreateVacationRequestData>({
    startDate: '',
    endDate: '',
    isHalfDayStart: false,
    isHalfDayEnd: false,
    notes: '',
  })
  const [formError, setFormError] = useState<string | null>(null)
  const [formLoading, setFormLoading] = useState(false)
  const [calculatedDays, setCalculatedDays] = useState<number | null>(null)

  // Calendar state
  const [calendarMonth, setCalendarMonth] = useState(new Date().getMonth() + 1)
  const [calendarYear, setCalendarYear] = useState(currentYear)
  const [calendarData, setCalendarData] = useState<{
    ownRequests: VacationRequest[]
    teamRequests: VacationRequest[]
    publicHolidays: PublicHoliday[]
  } | null>(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [bal, reqs, hols] = await Promise.all([
        vacationService.getBalance(yearFilter),
        vacationService.getRequests({ year: yearFilter, size: 100 }),
        vacationService.getHolidays(yearFilter),
      ])
      setBalance(bal)
      setRequests(reqs.content)
      setHolidays(hols)
    } catch {
      setError(t('vacation.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [yearFilter, t])

  useEffect(() => {
    loadData()
  }, [loadData])

  useEffect(() => {
    if (activeTab === 'calendar') {
      const canViewTeam = hasPermission('vacation.view.team')
      if (canViewTeam) {
        vacationService
          .getTeamCalendar(calendarYear, calendarMonth)
          .then((data) => setCalendarData(data))
          .catch(() => {
            setCalendarData({ ownRequests: requests, teamRequests: [], publicHolidays: holidays })
          })
      } else {
        // DSGVO: employees only see their own data
        setCalendarData({ ownRequests: requests, teamRequests: [], publicHolidays: holidays })
      }
    }
  }, [activeTab, calendarYear, calendarMonth, requests, holidays, hasPermission])

  // Auto-calculate days when dates change
  useEffect(() => {
    if (formData.startDate && formData.endDate) {
      const start = new Date(formData.startDate)
      const end = new Date(formData.endDate)
      if (start <= end) {
        let days = 0
        const cur = new Date(start)
        const holidayDates = new Set(holidays.map((h) => h.date))
        while (cur <= end) {
          const dow = cur.getDay() // 0=Sun, 1=Mon..5=Fri, 6=Sat
          const dateStr = cur.toISOString().split('T')[0]
          if (dow >= 1 && dow <= 5 && !holidayDates.has(dateStr)) {
            days++
          }
          cur.setDate(cur.getDate() + 1)
        }
        let adjustment = 0
        if (formData.isHalfDayStart && start.getDay() >= 1 && start.getDay() <= 5) adjustment -= 0.5
        if (
          formData.isHalfDayEnd &&
          end.getDay() >= 1 &&
          end.getDay() <= 5 &&
          start.getTime() !== end.getTime()
        )
          adjustment -= 0.5
        setCalculatedDays(Math.max(0, days + adjustment))
      } else {
        setCalculatedDays(null)
      }
    } else {
      setCalculatedDays(null)
    }
  }, [formData.startDate, formData.endDate, formData.isHalfDayStart, formData.isHalfDayEnd, holidays])

  const handleCancelRequest = async (id: string) => {
    if (!confirm(t('vacation.request.cancel_confirm'))) return
    try {
      await vacationService.cancelRequest(id)
      await loadData()
    } catch {
      setError(t('vacation.errors.cancel_failed'))
    }
  }

  const handleSubmitRequest = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError(null)
    setFormLoading(true)
    try {
      await vacationService.createRequest(formData)
      setFormData({ startDate: '', endDate: '', isHalfDayStart: false, isHalfDayEnd: false, notes: '' })
      setActiveTab('requests')
      await loadData()
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { detail?: string } } }
      setFormError(axiosErr?.response?.data?.detail ?? t('vacation.errors.create_failed'))
    } finally {
      setFormLoading(false)
    }
  }

  const years = [currentYear - 1, currentYear, currentYear + 1]

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('vacation.title')}</h1>

      {/* Balance Card */}
      {balance && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-white rounded-lg shadow p-4">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{t('vacation.balance.total')}</p>
            <p className="text-2xl font-bold text-gray-900">
              {Number(balance.totalDays) + Number(balance.carriedOverDays)}
            </p>
          </div>
          <div className="bg-white rounded-lg shadow p-4">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{t('vacation.balance.used')}</p>
            <p className="text-2xl font-bold text-red-600">{Number(balance.usedDays)}</p>
          </div>
          <div className="bg-white rounded-lg shadow p-4">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{t('vacation.balance.pending')}</p>
            <p className="text-2xl font-bold text-yellow-600">{Number(balance.pendingDays)}</p>
          </div>
          <div className="bg-white rounded-lg shadow p-4">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{t('vacation.balance.remaining')}</p>
            <p className="text-2xl font-bold text-green-600">{Number(balance.remainingDays)}</p>
          </div>
        </div>
      )}

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex gap-6">
          {(['requests', 'new_request', 'calendar'] as Tab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`py-2 px-1 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab
                  ? 'border-primary-600 text-primary-700'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {t(`vacation.tabs.${tab}`)}
            </button>
          ))}
        </nav>
      </div>

      {/* Requests Tab */}
      {activeTab === 'requests' && (
        <div>
          <div className="flex items-center gap-4 mb-4">
            <label className="text-sm text-gray-700">{t('common.filter')}:</label>
            <select
              value={yearFilter}
              onChange={(e) => setYearFilter(Number(e.target.value))}
              className="border border-gray-300 rounded px-3 py-1.5 text-sm"
            >
              {years.map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>

          {loading ? (
            <p className="text-gray-500">{t('common.loading')}</p>
          ) : requests.length === 0 ? (
            <p className="text-gray-500">{t('vacation.request.no_requests')}</p>
          ) : (
            <div className="bg-white shadow rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    {['start_date', 'end_date', 'total_days', 'status', 'notes', 'actions'].map((col) => (
                      <th
                        key={col}
                        className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                      >
                        {t(`vacation.request.${col}`)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {requests.map((req) => (
                    <tr key={req.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm text-gray-900">{req.startDate}</td>
                      <td className="px-4 py-3 text-sm text-gray-900">{req.endDate}</td>
                      <td className="px-4 py-3 text-sm text-gray-900">{req.totalDays}</td>
                      <td className="px-4 py-3">
                        <StatusBadge status={req.status} />
                      </td>
                      <td className="px-4 py-3 text-sm text-gray-500">{req.notes ?? '—'}</td>
                      <td className="px-4 py-3">
                        {(req.status === 'PENDING' || req.status === 'APPROVED') && (
                          <button
                            onClick={() => handleCancelRequest(req.id)}
                            className="text-xs text-red-600 hover:text-red-800 font-medium"
                          >
                            {t('vacation.request.cancel')}
                          </button>
                        )}
                        {req.status === 'REJECTED' && req.rejectionReason && (
                          <span className="text-xs text-gray-500" title={req.rejectionReason}>
                            {t('vacation.request.see_reason')}
                          </span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* New Request Tab */}
      {activeTab === 'new_request' && (
        <form onSubmit={handleSubmitRequest} className="bg-white shadow rounded-lg p-6 max-w-lg">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('vacation.request.new')}</h2>

          {formError && (
            <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
              {formError}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t('vacation.request.start_date')}
              </label>
              <input
                type="date"
                required
                value={formData.startDate}
                min={new Date().toISOString().split('T')[0]}
                onChange={(e) => setFormData((p) => ({ ...p, startDate: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
              <label className="flex items-center gap-2 mt-1 text-sm text-gray-600">
                <input
                  type="checkbox"
                  checked={formData.isHalfDayStart}
                  onChange={(e) => setFormData((p) => ({ ...p, isHalfDayStart: e.target.checked }))}
                />
                {t('vacation.request.half_day_start')}
              </label>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t('vacation.request.end_date')}
              </label>
              <input
                type="date"
                required
                value={formData.endDate}
                min={formData.startDate || new Date().toISOString().split('T')[0]}
                onChange={(e) => setFormData((p) => ({ ...p, endDate: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
              <label className="flex items-center gap-2 mt-1 text-sm text-gray-600">
                <input
                  type="checkbox"
                  checked={formData.isHalfDayEnd}
                  onChange={(e) => setFormData((p) => ({ ...p, isHalfDayEnd: e.target.checked }))}
                />
                {t('vacation.request.half_day_end')}
              </label>
            </div>

            {calculatedDays !== null && (
              <p className="text-sm text-gray-600">
                {t('vacation.request.calculated_days')}: <strong>{calculatedDays}</strong>
              </p>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                {t('vacation.request.notes')}
              </label>
              <textarea
                value={formData.notes}
                onChange={(e) => setFormData((p) => ({ ...p, notes: e.target.value }))}
                rows={3}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>

            <button
              type="submit"
              disabled={formLoading}
              className="w-full bg-primary-600 text-white py-2 rounded font-medium hover:bg-primary-700 disabled:opacity-50 transition-colors"
            >
              {formLoading ? t('common.loading') : t('vacation.request.submit')}
            </button>
          </div>
        </form>
      )}

      {/* Calendar Tab */}
      {activeTab === 'calendar' && (
        <div>
          <div className="flex items-center gap-4 mb-4">
            <button
              onClick={() => {
                if (calendarMonth === 1) {
                  setCalendarMonth(12)
                  setCalendarYear((y) => y - 1)
                } else {
                  setCalendarMonth((m) => m - 1)
                }
              }}
              className="px-3 py-1 text-sm border rounded hover:bg-gray-50"
            >
              ←
            </button>
            <span className="font-medium">
              {new Date(calendarYear, calendarMonth - 1).toLocaleString('default', {
                month: 'long',
                year: 'numeric',
              })}
            </span>
            <button
              onClick={() => {
                if (calendarMonth === 12) {
                  setCalendarMonth(1)
                  setCalendarYear((y) => y + 1)
                } else {
                  setCalendarMonth((m) => m + 1)
                }
              }}
              className="px-3 py-1 text-sm border rounded hover:bg-gray-50"
            >
              →
            </button>
          </div>

          {calendarData && (
            <div className="space-y-3">
              {calendarData.publicHolidays.length > 0 && (
                <div className="bg-blue-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-blue-900 mb-2">{t('vacation.holidays.title')}</h3>
                  <ul className="space-y-1">
                    {calendarData.publicHolidays.map((h) => (
                      <li key={h.id} className="text-sm text-blue-700">
                        {h.date} — {h.name}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {calendarData.ownRequests.length > 0 && (
                <div className="bg-green-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-green-900 mb-2">
                    {t('vacation.calendar.own_requests')}
                  </h3>
                  <ul className="space-y-1">
                    {calendarData.ownRequests.map((r) => (
                      <li key={r.id} className="text-sm text-green-700 flex items-center gap-2">
                        {r.startDate} — {r.endDate}
                        <StatusBadge status={r.status} />
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {calendarData.teamRequests.length > 0 && (
                <div className="bg-purple-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-purple-900 mb-2">
                    {t('vacation.calendar.team_requests')}
                  </h3>
                  <ul className="space-y-1">
                    {calendarData.teamRequests.map((r) => (
                      <li key={r.id} className="text-sm text-purple-700">
                        {(r as VacationRequest & { userName?: string }).userName ?? t('common.unknown')} —{' '}
                        {r.startDate} — {r.endDate}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {calendarData.publicHolidays.length === 0 &&
                calendarData.ownRequests.length === 0 &&
                calendarData.teamRequests.length === 0 && (
                  <p className="text-gray-500 text-sm">{t('vacation.calendar.no_events')}</p>
                )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
