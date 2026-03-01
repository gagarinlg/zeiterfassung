import { format, parse } from 'date-fns'
import { de, enUS } from 'date-fns/locale'
import i18n from '../i18n'

export type DateFormatType = 'DD.MM.YYYY' | 'YYYY-MM-DD' | 'MM/DD/YYYY'
export type TimeFormatType = '24h' | '12h'

// Map our format strings to date-fns format strings
const DATE_FORMAT_MAP: Record<DateFormatType, string> = {
  'DD.MM.YYYY': 'dd.MM.yyyy',
  'YYYY-MM-DD': 'yyyy-MM-dd',
  'MM/DD/YYYY': 'MM/dd/yyyy',
}

const TIME_FORMAT_MAP: Record<TimeFormatType, string> = {
  '24h': 'HH:mm',
  '12h': 'hh:mm a',
}

function getLocale() {
  const lang = i18n.language || 'de'
  return lang.startsWith('de') ? de : enUS
}

function getDateFormatString(dateFormat?: DateFormatType | string | null): string {
  if (dateFormat && dateFormat in DATE_FORMAT_MAP) {
    return DATE_FORMAT_MAP[dateFormat as DateFormatType]
  }
  // Default based on current language
  const lang = i18n.language || 'de'
  return lang.startsWith('de') ? 'dd.MM.yyyy' : 'yyyy-MM-dd'
}

function getTimeFormatString(timeFormat?: TimeFormatType | string | null): string {
  if (timeFormat && timeFormat in TIME_FORMAT_MAP) {
    return TIME_FORMAT_MAP[timeFormat as TimeFormatType]
  }
  return 'HH:mm'
}

/**
 * Format a date string (ISO format YYYY-MM-DD or ISO timestamp) for display.
 * Uses the user's preferred format or falls back to locale default.
 */
export function formatDate(dateStr: string, dateFormat?: DateFormatType | string | null): string {
  if (!dateStr) return ''
  try {
    const date = dateStr.includes('T') ? new Date(dateStr) : parse(dateStr, 'yyyy-MM-dd', new Date())
    return format(date, getDateFormatString(dateFormat), { locale: getLocale() })
  } catch {
    return dateStr
  }
}

/**
 * Format a time string or Date for display.
 */
export function formatTime(dateStr: string, timeFormat?: TimeFormatType | string | null): string {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    return format(date, getTimeFormatString(timeFormat), { locale: getLocale() })
  } catch {
    return dateStr
  }
}

/**
 * Format a date-time string for display.
 */
export function formatDateTime(dateStr: string, dateFormat?: DateFormatType | string | null, timeFormat?: TimeFormatType | string | null): string {
  if (!dateStr) return ''
  try {
    const date = new Date(dateStr)
    const datePart = format(date, getDateFormatString(dateFormat), { locale: getLocale() })
    const timePart = format(date, getTimeFormatString(timeFormat), { locale: getLocale() })
    return `${datePart} ${timePart}`
  } catch {
    return dateStr
  }
}

/**
 * Get the locale-aware month name.
 */
export function formatMonthYear(year: number, month: number): string {
  const date = new Date(year, month - 1)
  return format(date, 'LLLL yyyy', { locale: getLocale() })
}

/**
 * Get the first day of week based on locale (1=Monday for German, 0=Sunday for US).
 */
export function getFirstDayOfWeek(): number {
  const lang = i18n.language || 'de'
  return lang.startsWith('de') ? 1 : 0
}

/**
 * Get weekday headers starting from the locale's first day.
 */
export function getWeekdayHeaders(): string[] {
  const locale = getLocale()
  const firstDay = getFirstDayOfWeek()
  const headers: string[] = []
  // Start from a known Monday (2024-01-01 is a Monday)
  const baseDate = new Date(2024, 0, 1)
  for (let i = 0; i < 7; i++) {
    const d = new Date(baseDate)
    d.setDate(baseDate.getDate() + ((i + firstDay - 1 + 7) % 7))
    headers.push(format(d, 'EEEEEE', { locale }))
  }
  return headers
}
