import apiClient from './apiClient'
import type { TimeEntry, DailySummary, TrackingStatusResponse, TimeSheetResponse } from '../types'

export const timeService = {
  async clockIn(notes?: string, source?: string): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time/clock-in', { notes, source })
    return response.data
  },

  async clockOut(notes?: string, source?: string): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time/clock-out', { notes, source })
    return response.data
  },

  async startBreak(notes?: string, source?: string): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time/break/start', { notes, source })
    return response.data
  },

  async endBreak(notes?: string, source?: string): Promise<TimeEntry> {
    const response = await apiClient.post<TimeEntry>('/time/break/end', { notes, source })
    return response.data
  },

  async getStatus(): Promise<TrackingStatusResponse> {
    const response = await apiClient.get<TrackingStatusResponse>('/time/status')
    return response.data
  },

  async getToday(): Promise<DailySummary> {
    const response = await apiClient.get<DailySummary>('/time/today')
    return response.data
  },

  async getEntries(start: string, end: string): Promise<TimeEntry[]> {
    const response = await apiClient.get<TimeEntry[]>('/time/entries', { params: { start, end } })
    return response.data
  },

  async getDailySummary(date: string): Promise<DailySummary> {
    const response = await apiClient.get<DailySummary>(`/time/summary/daily/${date}`)
    return response.data
  },

  async getWeeklySummary(weekStart: string): Promise<TimeSheetResponse> {
    const response = await apiClient.get<TimeSheetResponse>('/time/summary/weekly', { params: { weekStart } })
    return response.data
  },

  async getMonthlySummary(year: number, month: number): Promise<TimeSheetResponse> {
    const response = await apiClient.get<TimeSheetResponse>('/time/summary/monthly', { params: { year, month } })
    return response.data
  },

  async getTimeSheet(start: string, end: string): Promise<TimeSheetResponse> {
    const response = await apiClient.get<TimeSheetResponse>('/time/timesheet', { params: { start, end } })
    return response.data
  },

  async getTeamStatus(): Promise<Record<string, TrackingStatusResponse>> {
    const response = await apiClient.get<Record<string, TrackingStatusResponse>>('/time/manage/team/status')
    return response.data
  },

  async exportCsv(start: string, end: string): Promise<Blob> {
    const response = await apiClient.get('/time/export/csv', {
      params: { start, end },
      responseType: 'blob',
    })
    return response.data as Blob
  },
}
