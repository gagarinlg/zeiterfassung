import apiClient from './apiClient'
import type { PageResponse } from '../types'

export interface TimeModificationResponse {
  id: string
  userId: string
  userName: string
  timeEntryId: string
  entryType: string
  originalTimestamp: string
  requestedTimestamp: string
  requestedNotes: string | null
  reason: string
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'
  reviewedById: string | null
  reviewedByName: string | null
  rejectionReason: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateTimeModificationRequest {
  timeEntryId: string
  requestedTimestamp: string
  requestedNotes?: string
  reason: string
}

export const timeModificationService = {
  async createRequest(data: CreateTimeModificationRequest): Promise<TimeModificationResponse> {
    const response = await apiClient.post<TimeModificationResponse>('/time-modifications', data)
    return response.data
  },

  async getMyRequests(page = 0, size = 20): Promise<PageResponse<TimeModificationResponse>> {
    const response = await apiClient.get<PageResponse<TimeModificationResponse>>('/time-modifications', {
      params: { page, size },
    })
    return response.data
  },

  async getPendingRequests(page = 0, size = 100): Promise<PageResponse<TimeModificationResponse>> {
    const response = await apiClient.get<PageResponse<TimeModificationResponse>>('/time-modifications/pending', {
      params: { page, size },
    })
    return response.data
  },

  async approveRequest(id: string): Promise<TimeModificationResponse> {
    const response = await apiClient.post<TimeModificationResponse>(`/time-modifications/${id}/approve`)
    return response.data
  },

  async rejectRequest(id: string, rejectionReason: string): Promise<TimeModificationResponse> {
    const response = await apiClient.post<TimeModificationResponse>(`/time-modifications/${id}/reject`, { rejectionReason })
    return response.data
  },

  async cancelRequest(id: string): Promise<void> {
    await apiClient.delete(`/time-modifications/${id}`)
  },
}
