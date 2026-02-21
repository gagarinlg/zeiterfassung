export interface User {
  id: string
  email: string
  firstName: string
  lastName: string
  employeeNumber?: string
  phone?: string
  photoUrl?: string
  managerId?: string
  isActive: boolean
  roles: string[]
  permissions: string[]
}

export interface TimeEntry {
  id: string
  userId: string
  entryType: 'CLOCK_IN' | 'CLOCK_OUT' | 'BREAK_START' | 'BREAK_END'
  timestamp: string
  source: 'WEB' | 'MOBILE' | 'TERMINAL'
  notes?: string
  isModified: boolean
}

export interface DailySummary {
  id: string
  userId: string
  date: string
  totalWorkMinutes: number
  totalBreakMinutes: number
  overtimeMinutes: number
  isCompliant: boolean
  complianceNotes?: string
}

export interface VacationRequest {
  id: string
  userId: string
  startDate: string
  endDate: string
  isHalfDayStart: boolean
  isHalfDayEnd: boolean
  totalDays: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  approvedBy?: string
  rejectionReason?: string
  notes?: string
  createdAt: string
}

export interface VacationBalance {
  id: string
  userId: string
  year: number
  totalDays: number
  usedDays: number
  carriedOverDays: number
  remainingDays: number
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export interface LoginRequest {
  email: string
  password: string
}

export interface ApiError {
  status: number
  message: string
  errors?: Record<string, string>
}
