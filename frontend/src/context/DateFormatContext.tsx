import { createContext, useContext, useMemo, type ReactNode } from 'react'
import { useAuth } from '../hooks/useAuth'
import type { DateFormatType, TimeFormatType } from '../utils/dateUtils'

interface DateFormatSettings {
  dateFormat: DateFormatType
  timeFormat: TimeFormatType
}

const DateFormatContext = createContext<DateFormatSettings>({
  dateFormat: 'DD.MM.YYYY',
  timeFormat: '24h',
})

export function DateFormatProvider({ children }: { children: ReactNode }) {
  const { user } = useAuth()

  const settings = useMemo<DateFormatSettings>(() => {
    if (user) {
      const u = user as { dateFormat?: string; timeFormat?: string }
      return {
        dateFormat: (u.dateFormat as DateFormatType) || 'DD.MM.YYYY',
        timeFormat: (u.timeFormat as TimeFormatType) || '24h',
      }
    }
    return { dateFormat: 'DD.MM.YYYY', timeFormat: '24h' }
  }, [user])

  return (
    <DateFormatContext.Provider value={settings}>
      {children}
    </DateFormatContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useDateFormat() {
  return useContext(DateFormatContext)
}
