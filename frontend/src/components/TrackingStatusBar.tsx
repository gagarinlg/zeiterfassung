import { useTranslation } from 'react-i18next'
import type { TrackingStatusResponse } from '../types'
import { formatTime } from '../utils/dateUtils'

interface TrackingStatusBarProps {
  status: TrackingStatusResponse
  timeFormat: string
  actionLoading: boolean
  actionError: string | null
  onClockIn: () => void
  onClockOut: () => void
  onStartBreak: () => void
  onEndBreak: () => void
}

export default function TrackingStatusBar({
  status,
  timeFormat,
  actionLoading,
  actionError,
  onClockIn,
  onClockOut,
  onStartBreak,
  onEndBreak,
}: TrackingStatusBarProps) {
  const { t } = useTranslation()

  return (
    <div className={`mb-6 p-4 rounded-lg border ${
      status.status === 'ON_BREAK' ? 'bg-yellow-50 border-yellow-200' :
      status.status === 'CLOCKED_IN' ? 'bg-green-50 border-green-200' :
      'bg-white border-gray-200'
    }`}>
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <p className="text-sm text-gray-500 mb-1">{t('time_tracking.current_status')}</p>
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-semibold ${
            status.status === 'CLOCKED_IN' ? 'bg-green-100 text-green-800' :
            status.status === 'ON_BREAK' ? 'bg-yellow-100 text-yellow-800' :
            'bg-gray-100 text-gray-700'
          }`}>
            {t(`time_tracking.status.${status.status.toLowerCase()}`)}
          </span>
          {status.status === 'CLOCKED_IN' && status.clockedInSince && (
            <p className="text-xs text-green-700 mt-1">
              {t('dashboard.clocked_in_since')} {formatTime(status.clockedInSince, timeFormat)}
            </p>
          )}
          {status.status === 'ON_BREAK' && status.breakStartedAt && (
            <p className="text-xs text-yellow-700 mt-1">
              {t('dashboard.on_break_since')} {formatTime(status.breakStartedAt, timeFormat)}
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {status.status === 'CLOCKED_OUT' && (
            <button
              onClick={onClockIn}
              disabled={actionLoading}
              className="bg-green-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-green-700 transition-colors disabled:opacity-50"
            >
              {t('time_tracking.clock_in')}
            </button>
          )}
          {status.status === 'CLOCKED_IN' && (
            <>
              <button
                onClick={onStartBreak}
                disabled={actionLoading}
                className="bg-yellow-500 text-white px-4 py-2 rounded-lg font-medium hover:bg-yellow-600 transition-colors disabled:opacity-50"
              >
                {t('time_tracking.break_start')}
              </button>
              <button
                onClick={onClockOut}
                disabled={actionLoading}
                className="bg-red-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                {t('time_tracking.clock_out')}
              </button>
            </>
          )}
          {status.status === 'ON_BREAK' && (
              <button
                onClick={onEndBreak}
                disabled={actionLoading}
                className="bg-green-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-green-700 transition-colors disabled:opacity-50"
              >
                {t('time_tracking.break_end')}
              </button>
          )}
        </div>
      </div>
      {actionError && (
        <div role="alert" aria-live="polite" className="mt-3 p-2 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {actionError}
        </div>
      )}
    </div>
  )
}
