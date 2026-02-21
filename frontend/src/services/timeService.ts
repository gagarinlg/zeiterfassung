import apiClient from './apiClient'
import type { TimeEntry, DailySummary } from '../types'

export const timeService = {
  async clockIn(notes?: string): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time-entries/clock-in', { notes })
    return response.data
  },

  async clockOut(notes?: string): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time-entries/clock-out', { notes })
    return response.data
  },

  async startBreak(): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time-entries/break-start')
    return response.data
  },

  async endBreak(): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time-entries/break-end')
    return response.data
  },

  async getEntries(userId: string, from: string, to: string): Promise<TimeEntry[]> {
    const response = await apiClient.get<TimeEntry[]>('/time-entries', {
      params: { userId, from, to },
    })
    return response.data
  },

  async getDailySummary(userId: string, date: string): Promise<DailySummary> {
    const response = await apiClient.get<DailySummary>(`/time-entries/summary/${userId}/${date}`)
    return response.data
  },
}
