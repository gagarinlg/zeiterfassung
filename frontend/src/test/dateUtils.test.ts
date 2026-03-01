import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

let formatDate: typeof import('../utils/dateUtils').formatDate
let formatTime: typeof import('../utils/dateUtils').formatTime
let formatDateTime: typeof import('../utils/dateUtils').formatDateTime
let formatMonthYear: typeof import('../utils/dateUtils').formatMonthYear
let getFirstDayOfWeek: typeof import('../utils/dateUtils').getFirstDayOfWeek

async function loadModule() {
  const mod = await import('../utils/dateUtils')
  formatDate = mod.formatDate
  formatTime = mod.formatTime
  formatDateTime = mod.formatDateTime
  formatMonthYear = mod.formatMonthYear
  getFirstDayOfWeek = mod.getFirstDayOfWeek
}

describe('dateUtils (German locale)', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.doMock('../i18n', () => ({
      default: { language: 'de' },
    }))
    await loadModule()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('formatDate', () => {
    it('formats with DD.MM.YYYY format', () => {
      expect(formatDate('2024-12-25', 'DD.MM.YYYY')).toBe('25.12.2024')
    })

    it('formats with YYYY-MM-DD format', () => {
      expect(formatDate('2024-12-25', 'YYYY-MM-DD')).toBe('2024-12-25')
    })

    it('formats with MM/DD/YYYY format', () => {
      expect(formatDate('2024-12-25', 'MM/DD/YYYY')).toBe('12/25/2024')
    })

    it('returns empty string for empty input', () => {
      expect(formatDate('')).toBe('')
    })

    it('returns original string for invalid date', () => {
      expect(formatDate('not-a-date')).toBe('not-a-date')
    })

    it('correctly extracts date from ISO timestamp', () => {
      expect(formatDate('2024-12-25T14:30:00Z', 'DD.MM.YYYY')).toBe('25.12.2024')
    })

    it('uses DD.MM.YYYY as default for German locale', () => {
      expect(formatDate('2024-12-25')).toBe('25.12.2024')
    })
  })

  describe('formatTime', () => {
    it('formats time in 24h format', () => {
      expect(formatTime('2024-12-25T14:30:00', '24h')).toBe('14:30')
    })

    it('formats time in 12h format', () => {
      const result = formatTime('2024-12-25T14:30:00', '12h')
      expect(result).toMatch(/02:30\s*(PM|pm|nachm\.)/)
    })

    it('returns empty string for empty input', () => {
      expect(formatTime('')).toBe('')
    })
  })

  describe('formatDateTime', () => {
    it('combines date and time', () => {
      const result = formatDateTime('2024-12-25T14:30:00', 'DD.MM.YYYY', '24h')
      expect(result).toBe('25.12.2024 14:30')
    })

    it('returns empty string for empty input', () => {
      expect(formatDateTime('')).toBe('')
    })
  })

  describe('formatMonthYear', () => {
    it('returns locale-aware month name for German', () => {
      const result = formatMonthYear(2024, 12)
      expect(result).toBe('Dezember 2024')
    })

    it('returns correct month for January', () => {
      const result = formatMonthYear(2024, 1)
      expect(result).toBe('Januar 2024')
    })
  })

  describe('getFirstDayOfWeek', () => {
    it('returns 1 (Monday) for German locale', () => {
      expect(getFirstDayOfWeek()).toBe(1)
    })
  })
})

describe('dateUtils (English locale)', () => {
  beforeEach(async () => {
    vi.resetModules()
    vi.doMock('../i18n', () => ({
      default: { language: 'en' },
    }))
    await loadModule()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('formatDate', () => {
    it('uses YYYY-MM-DD as default for English locale', () => {
      expect(formatDate('2024-12-25')).toBe('2024-12-25')
    })
  })

  describe('formatMonthYear', () => {
    it('returns English month name', () => {
      const result = formatMonthYear(2024, 12)
      expect(result).toBe('December 2024')
    })
  })

  describe('getFirstDayOfWeek', () => {
    it('returns 0 (Sunday) for English locale', () => {
      expect(getFirstDayOfWeek()).toBe(0)
    })
  })
})
