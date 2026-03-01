import { useState, useEffect, useCallback, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { timeService } from '../services/timeService'
import type { TrackingStatusResponse, TimeEntry, TimeSheetResponse } from '../types'
import { formatTime, formatDate } from '../utils/dateUtils'
import { useDateFormat } from '../context/DateFormatContext'

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

function getFirstDayOfMonth(date: Date): string {
  return new Date(date.getFullYear(), date.getMonth(), 1).toISOString().split('T')[0]
}

function getLastDayOfMonth(date: Date): string {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0).toISOString().split('T')[0]
}

function getTodayInstant(): string {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  return start.toISOString()
}

function getTomorrowInstant(): string {
  const now = new Date()
  const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
  return end.toISOString()
}

export default function TimeTrackingPage() {
  const { t } = useTranslation()
  const { dateFormat, timeFormat } = useDateFormat()

  const [status, setStatus] = useState<TrackingStatusResponse | null>(null)
  const [entries, setEntries] = useState<TimeEntry[]>([])
  const [monthSheet, setMonthSheet] = useState<TimeSheetResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [elapsed, setElapsed] = useState<number>(0)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const now = new Date()
      const [st, ents, sheet] = await Promise.all([
        timeService.getStatus(),
        timeService.getEntries(getTodayInstant(), getTomorrowInstant()),
        timeService.getMonthlySummary(now.getFullYear(), now.getMonth() + 1),
      ])
      setStatus(st)
      setEntries(ents)
      setMonthSheet(sheet)
      setElapsed(st.elapsedWorkMinutes)
    } catch {
      setError(t('time_tracking.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Recalculate elapsed time from clockedInSince on each interval tick to avoid drift
  useEffect(() => {
    if (intervalRef.current) clearInterval(intervalRef.current)
    if (status && status.status === 'CLOCKED_IN' && status.clockedInSince) {
      const calcElapsed = () => {
        const diffMs = Date.now() - new Date(status.clockedInSince!).getTime()
        setElapsed(Math.floor(diffMs / 60000))
      }
      calcElapsed()
      intervalRef.current = setInterval(calcElapsed, 60000)
    }
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [status])

  const handleClockIn = async () => {
    setActionError(null)
    try {
      await timeService.clockIn(undefined, 'WEB')
      await loadData()
    } catch {
      setActionError(t('time_tracking.errors.clock_in_failed'))
    }
  }

  const handleClockOut = async () => {
    setActionError(null)
    try {
      await timeService.clockOut(undefined, 'WEB')
      await loadData()
    } catch {
      setActionError(t('time_tracking.errors.clock_out_failed'))
    }
  }

  const handleStartBreak = async () => {
    setActionError(null)
    try {
      await timeService.startBreak(undefined, 'WEB')
      await loadData()
    } catch {
      setActionError(t('time_tracking.errors.break_start_failed'))
    }
  }

  const handleEndBreak = async () => {
    setActionError(null)
    try {
      await timeService.endBreak(undefined, 'WEB')
      await loadData()
    } catch {
      setActionError(t('time_tracking.errors.break_end_failed'))
    }
  }

  const handleExportCsv = async () => {
    const now = new Date()
    const start = getFirstDayOfMonth(now)
    const end = getLastDayOfMonth(now)
    try {
      const blob = await timeService.exportCsv(start, end)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `timesheet_${start}_${end}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setActionError(t('time_tracking.errors.load_failed'))
    }
  }

  const statusColor =
    status?.status === 'CLOCKED_IN'
      ? 'bg-green-100 text-green-800'
      : status?.status === 'ON_BREAK'
        ? 'bg-yellow-100 text-yellow-800'
        : 'bg-gray-100 text-gray-700'

  const entryTypeLabel = (type: TimeEntry['entryType']) => {
    const map: Record<TimeEntry['entryType'], string> = {
      CLOCK_IN: t('time_tracking.clock_in'),
      CLOCK_OUT: t('time_tracking.clock_out'),
      BREAK_START: t('time_tracking.break_start'),
      BREAK_END: t('time_tracking.break_end'),
    }
    return map[type]
  }

  if (loading) {
    return (
      <div className="p-8">
        <p className="text-gray-500">{t('common.loading')}</p>
      </div>
    )
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('time_tracking.title')}</h1>

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}

      {/* Current Status Card */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100 mb-6">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div>
            <p className="text-sm text-gray-500 mb-1">{t('time_tracking.current_status')}</p>
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold ${statusColor}`}>
              {status ? t(`time_tracking.status.${status.status.toLowerCase()}`) : '—'}
            </span>
          </div>
          <div className="flex items-center gap-2">
            {status?.status === 'CLOCKED_OUT' && (
              <button
                onClick={handleClockIn}
                className="bg-green-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-green-700 transition-colors"
              >
                {t('time_tracking.clock_in')}
              </button>
            )}
            {status?.status === 'CLOCKED_IN' && (
              <>
                <button
                  onClick={handleStartBreak}
                  className="bg-yellow-500 text-white px-4 py-2 rounded-lg font-medium hover:bg-yellow-600 transition-colors"
                >
                  {t('time_tracking.break_start')}
                </button>
                <button
                  onClick={handleClockOut}
                  className="bg-red-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-red-700 transition-colors"
                >
                  {t('time_tracking.clock_out')}
                </button>
              </>
            )}
            {status?.status === 'ON_BREAK' && (
              <>
                <button
                  onClick={handleEndBreak}
                  className="bg-blue-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-blue-700 transition-colors"
                >
                  {t('time_tracking.break_end')}
                </button>
                <button
                  onClick={handleClockOut}
                  className="bg-red-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-red-700 transition-colors"
                >
                  {t('time_tracking.clock_out')}
                </button>
              </>
            )}
          </div>
        </div>

        {actionError && (
          <div role="alert" className="mt-3 p-2 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
            {actionError}
          </div>
        )}
      </div>

      {/* Today Summary */}
      {status && (
        <div className="grid grid-cols-2 md:grid-cols-3 gap-4 mb-6">
          <div className="bg-white rounded-lg shadow-sm p-4 border border-gray-100">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{t('time_tracking.work_time')}</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{formatMinutes(status.todayWorkMinutes)}</p>
          </div>
          <div className="bg-white rounded-lg shadow-sm p-4 border border-gray-100">
            <p className="text-xs text-gray-500 uppercase tracking-wide">{t('time_tracking.break_time')}</p>
            <p className="text-2xl font-bold text-gray-900 mt-1">{formatMinutes(status.todayBreakMinutes)}</p>
          </div>
          {status.status !== 'CLOCKED_OUT' && (
            <div className="bg-white rounded-lg shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500 uppercase tracking-wide">{t('time_tracking.elapsed')}</p>
              <p className="text-2xl font-bold text-primary-700 mt-1">{formatMinutes(elapsed)}</p>
            </div>
          )}
        </div>
      )}

      {/* Today's Entries */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 mb-6">
        <div className="px-6 py-4 border-b border-gray-100">
          <h2 className="text-base font-semibold text-gray-900">{t('time_tracking.entries')}</h2>
        </div>
        {entries.length === 0 ? (
          <p className="px-6 py-4 text-sm text-gray-500">{t('time_tracking.no_entries_today')}</p>
        ) : (
          <ul className="divide-y divide-gray-100">
            {entries.map((entry) => (
              <li key={entry.id} className="px-6 py-3 flex items-center justify-between">
                <div>
                  <span className="text-sm font-medium text-gray-900">{entryTypeLabel(entry.entryType)}</span>
                  {entry.notes && <span className="ml-2 text-xs text-gray-500">{entry.notes}</span>}
                </div>
                <div className="text-right">
                  <p className="text-sm text-gray-700">
                    {formatTime(entry.timestamp, timeFormat)}
                  </p>
                  <p className="text-xs text-gray-400">{entry.source}</p>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Monthly Timesheet */}
      {monthSheet && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-100">
          <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
            <h2 className="text-base font-semibold text-gray-900">{t('time_tracking.timesheet')}</h2>
            <button
              onClick={handleExportCsv}
              className="text-sm text-primary-600 hover:text-primary-800 font-medium flex items-center gap-1"
            >
              {t('time_tracking.export_csv')}
            </button>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {(() => {
                    const colLabels: Record<string, string> = {
                      date: t('common.date'),
                      work_time: t('time_tracking.work_time'),
                      break_time: t('time_tracking.break_time'),
                      overtime: t('time_tracking.overtime'),
                      status: t('common.status'),
                    }
                    return ['date', 'work_time', 'break_time', 'overtime', 'status'].map((col) => (
                      <th
                        key={col}
                        className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                      >
                        {colLabels[col]}
                      </th>
                    ))
                  })()}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {monthSheet.dailySummaries.map((day) => (
                  <tr key={day.date} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{formatDate(day.date, dateFormat)}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{formatMinutes(day.totalWorkMinutes)}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{formatMinutes(day.totalBreakMinutes)}</td>
                    <td className={`px-4 py-3 text-sm font-medium ${overtimeColorClass(day.overtimeMinutes)}`}>
                      {formatOvertime(day.overtimeMinutes)}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${day.isCompliant ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                        {day.isCompliant ? t('time_tracking.compliant') : t('time_tracking.non_compliant')}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
              <tfoot className="bg-gray-50">
                <tr>
                  <td className="px-4 py-3 text-sm font-semibold text-gray-900">{t('common.total')}</td>
                  <td className="px-4 py-3 text-sm font-semibold text-gray-900">{formatMinutes(monthSheet.totalWorkMinutes)}</td>
                  <td className="px-4 py-3 text-sm font-semibold text-gray-900">{formatMinutes(monthSheet.totalBreakMinutes)}</td>
                  <td className={`px-4 py-3 text-sm font-semibold ${overtimeColorClass(monthSheet.totalOvertimeMinutes)}`}>
                    {formatOvertime(monthSheet.totalOvertimeMinutes)}
                  </td>
                  <td />
                </tr>
              </tfoot>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}
