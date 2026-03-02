import apiClient from './apiClient'
import type { PageResponse } from '../types'

export interface WorkHourChangeResponse {
  id: string
  userId: string
  userName: string
  currentWeeklyHours: number
  requestedWeeklyHours: number
  currentDailyHours: number | null
  requestedDailyHours: number | null
  effectiveDate: string
  reason: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  approvedById: string | null
  approvedByName: string | null
  rejectionReason: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateWorkHourChangeRequest {
  requestedWeeklyHours: number
  requestedDailyHours?: number
  effectiveDate: string
  reason?: string
}

export const workHourChangeService = {
  async createRequest(data: CreateWorkHourChangeRequest): Promise<WorkHourChangeResponse> {
    const response = await apiClient.post<WorkHourChangeResponse>('/work-hour-changes', data)
    return response.data
  },

  async getMyRequests(page = 0, size = 20): Promise<PageResponse<WorkHourChangeResponse>> {
    const response = await apiClient.get<PageResponse<WorkHourChangeResponse>>('/work-hour-changes', {
      params: { page, size },
    })
    return response.data
  },

  async getPendingRequests(page = 0, size = 100): Promise<PageResponse<WorkHourChangeResponse>> {
    const response = await apiClient.get<PageResponse<WorkHourChangeResponse>>('/work-hour-changes/pending', {
      params: { page, size },
    })
    return response.data
  },

  async approveRequest(id: string): Promise<WorkHourChangeResponse> {
    const response = await apiClient.post<WorkHourChangeResponse>(`/work-hour-changes/${id}/approve`)
    return response.data
  },

  async rejectRequest(id: string, rejectionReason: string): Promise<WorkHourChangeResponse> {
    const response = await apiClient.post<WorkHourChangeResponse>(`/work-hour-changes/${id}/reject`, { rejectionReason })
    return response.data
  },

  async cancelRequest(id: string): Promise<void> {
    await apiClient.delete(`/work-hour-changes/${id}`)
  },
}
