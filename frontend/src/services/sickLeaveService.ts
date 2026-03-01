import apiClient from './apiClient'
import type { PageResponse } from '../types'

export interface SickLeaveResponse {
  id: string
  userId: string
  userName: string
  startDate: string
  endDate: string
  status: 'REPORTED' | 'CERTIFICATE_PENDING' | 'CERTIFICATE_RECEIVED' | 'CANCELLED'
  hasCertificate: boolean
  certificateSubmittedAt: string | null
  notes: string | null
  reportedById: string | null
  reportedByName: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateSickLeaveRequest {
  startDate: string
  endDate: string
  notes?: string
}

export interface UpdateSickLeaveRequest {
  endDate?: string
  hasCertificate?: boolean
  notes?: string
}

export const sickLeaveService = {
  async reportSickLeave(data: CreateSickLeaveRequest): Promise<SickLeaveResponse> {
    const response = await apiClient.post<SickLeaveResponse>('/sick-leave', data)
    return response.data
  },

  async updateSickLeave(id: string, data: UpdateSickLeaveRequest): Promise<SickLeaveResponse> {
    const response = await apiClient.put<SickLeaveResponse>(`/sick-leave/${id}`, data)
    return response.data
  },

  async cancelSickLeave(id: string): Promise<void> {
    await apiClient.delete(`/sick-leave/${id}`)
  },

  async getMySickLeaves(page = 0, size = 20): Promise<PageResponse<SickLeaveResponse>> {
    const response = await apiClient.get<PageResponse<SickLeaveResponse>>('/sick-leave', {
      params: { page, size },
    })
    return response.data
  },

  async getSickLeave(id: string): Promise<SickLeaveResponse> {
    const response = await apiClient.get<SickLeaveResponse>(`/sick-leave/${id}`)
    return response.data
  },

  async submitCertificate(id: string): Promise<SickLeaveResponse> {
    const response = await apiClient.post<SickLeaveResponse>(`/sick-leave/${id}/certificate`)
    return response.data
  },
}
