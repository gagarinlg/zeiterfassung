import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'
import { timeService } from '../services/timeService'
import { vacationService } from '../services/vacationService'
import apiClient from '../services/apiClient'
import type { TrackingStatusResponse, TimeSheetResponse, VacationBalance, User } from '../types'

function formatMinutes(minutes: number): string {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `${h}:${String(m).padStart(2, '0')}`
}

function overtimeColorClass(minutes: number): string {
  if (minutes > 0) return 'text-blue-600'
  if (minutes < 0) return 'text-red-600'
  return 'text-gray-500'
}

function formatOvertime(minutes: number): string {
  if (minutes === 0) return '—'
  return (minutes > 0 ? '+' : '') + formatMinutes(Math.abs(minutes))
}

function getMonday(date: Date): string {
  const d = new Date(date)
  const day = d.getDay()
  const diff = d.getDate() - day + (day === 0 ? -6 : 1)
  d.setDate(diff)
  return d.toISOString().split('T')[0]
}

export default function DashboardPage() {
  const { t } = useTranslation()
  const { user, hasPermission } = useAuth()
  const currentYear = new Date().getFullYear()

  const [status, setStatus] = useState<TrackingStatusResponse | null>(null)
  const [weekSheet, setWeekSheet] = useState<TimeSheetResponse | null>(null)
  const [balance, setBalance] = useState<(VacationBalance & { pendingDays: number }) | null>(null)
  const [teamStatus, setTeamStatus] = useState<Record<string, TrackingStatusResponse> | null>(null)
  const [teamMembers, setTeamMembers] = useState<Record<string, string>>({})
  const [pendingCount, setPendingCount] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const isManager = hasPermission('time.view.team')

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const weekStart = getMonday(new Date())
      const [st, ws, bal] = await Promise.all([
        timeService.getStatus(),
        timeService.getWeeklySummary(weekStart),
        vacationService.getBalance(currentYear),
      ])
      setStatus(st)
      setWeekSheet(ws)
      setBalance(bal)

      if (isManager) {
        const [ts, pending, members] = await Promise.all([
          timeService.getTeamStatus(),
          vacationService.getPendingRequests({ size: 1 }),
          user?.id
            ? apiClient.get<User[]>(`/users/${user.id}/team`).then((r) => r.data)
            : Promise.resolve([] as User[]),
        ])
        setTeamStatus(ts)
        setPendingCount(pending.totalElements)
        const nameMap: Record<string, string> = {}
        members.forEach((m) => { nameMap[m.id] = `${m.firstName} ${m.lastName}` })
        setTeamMembers(nameMap)
      }
    } catch {
      setError(t('time_tracking.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [currentYear, isManager, user?.id, t])

  useEffect(() => {
    loadData()
  }, [loadData])

  const teamPresentCount = teamStatus
    ? Object.values(teamStatus).filter((s) => s.status === 'CLOCKED_IN' || s.status === 'ON_BREAK').length
    : null

  if (loading) {
    return (
      <div className="p-8">
        <p className="text-gray-500">{t('common.loading')}</p>
      </div>
    )
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{t('dashboard.title')}</h1>
      <p className="text-gray-600 mb-6">
        {user?.firstName} {user?.lastName}
      </p>

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}

      {/* Status indicator */}
      {status && status.status !== 'CLOCKED_OUT' && (
        <div className={`mb-6 p-4 rounded-lg border ${status.status === 'ON_BREAK' ? 'bg-yellow-50 border-yellow-200' : 'bg-green-50 border-green-200'}`}>
          <p className={`text-sm font-medium ${status.status === 'ON_BREAK' ? 'text-yellow-800' : 'text-green-800'}`}>
            {status.status === 'ON_BREAK'
              ? `${t('dashboard.on_break_since')} ${status.breakStartedAt ? new Date(status.breakStartedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}`
              : `${t('dashboard.clocked_in_since')} ${status.clockedInSince ? new Date(status.clockedInSince).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}`}
          </p>
        </div>
      )}

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <p className="text-sm text-gray-500">{t('dashboard.today_hours')}</p>
          <p className="text-3xl font-bold text-primary-700 mt-1">
            {status ? formatMinutes(status.todayWorkMinutes) : '—'}
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <p className="text-sm text-gray-500">{t('dashboard.week_hours')}</p>
          <p className="text-3xl font-bold text-primary-700 mt-1">
            {weekSheet ? formatMinutes(weekSheet.totalWorkMinutes) : '—'}
          </p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <p className="text-sm text-gray-500">{t('dashboard.vacation_remaining')}</p>
          <p className="text-3xl font-bold text-green-600 mt-1">
            {balance ? String(balance.remainingDays) : '—'}
          </p>
        </div>
        {isManager && (
          <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
            <p className="text-sm text-gray-500">{t('dashboard.team_present')}</p>
            <p className="text-3xl font-bold text-primary-700 mt-1">
              {teamPresentCount ?? '—'}
            </p>
          </div>
        )}
      </div>

      {/* Overtime balance */}
      {weekSheet && weekSheet.totalOvertimeMinutes !== 0 && (
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mb-6 max-w-xs">
          <p className="text-sm text-gray-500">{t('dashboard.overtime_balance')}</p>
          <p className={`text-3xl font-bold mt-1 ${overtimeColorClass(weekSheet.totalOvertimeMinutes)}`}>
            {formatOvertime(weekSheet.totalOvertimeMinutes)}
          </p>
        </div>
      )}

      {/* Compliance warning */}
      {weekSheet && weekSheet.dailySummaries.some((d) => !d.isCompliant) && (
        <div role="alert" className="mb-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
          <p className="font-medium text-yellow-800">{t('dashboard.compliance_warning')}</p>
          <ul className="mt-2 space-y-1">
            {weekSheet.dailySummaries
              .filter((d) => !d.isCompliant)
              .map((d) => (
                <li key={d.date} className="text-sm text-yellow-700">
                  {d.date}: {d.complianceNotes}
                </li>
              ))}
          </ul>
        </div>
      )}

      {/* Pending vacation requests for managers */}
      {isManager && pendingCount !== null && pendingCount > 0 && (
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mb-6">
          <p className="text-sm text-gray-500">{t('dashboard.pending_requests')}</p>
          <p className="text-3xl font-bold text-yellow-600 mt-1">{pendingCount}</p>
        </div>
      )}

      {/* Team status */}
      {isManager && teamStatus && (
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <h2 className="text-base font-semibold text-gray-900 mb-4">{t('dashboard.team_status')}</h2>
          {Object.keys(teamStatus).length === 0 ? (
            <p className="text-sm text-gray-500">{t('dashboard.no_team')}</p>
          ) : (
            <div className="space-y-2">
              {Object.entries(teamStatus).map(([uid, ts]) => (
                <div key={uid} className="flex items-center justify-between py-2 border-b border-gray-100 last:border-0">
                  <span className="text-sm text-gray-700">
                    {teamMembers[uid] ?? uid}
                  </span>
                  <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                    ts.status === 'CLOCKED_IN' ? 'bg-green-100 text-green-800' :
                    ts.status === 'ON_BREAK' ? 'bg-yellow-100 text-yellow-800' :
                    'bg-gray-100 text-gray-700'
                  }`}>
                    {t(`time_tracking.status.${ts.status.toLowerCase()}`)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
