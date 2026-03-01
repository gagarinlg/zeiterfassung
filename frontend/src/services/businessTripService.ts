import apiClient from './apiClient'
import type { PageResponse } from '../types'

export interface BusinessTripResponse {
  id: string
  userId: string
  userName: string
  startDate: string
  endDate: string
  destination: string
  purpose: string
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED'
  approvedById: string | null
  approvedByName: string | null
  rejectionReason: string | null
  notes: string | null
  estimatedCost: number | null
  actualCost: number | null
  costCenter: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateBusinessTripRequest {
  startDate: string
  endDate: string
  destination: string
  purpose: string
  notes?: string
  estimatedCost?: number
  costCenter?: string
}

export interface UpdateBusinessTripRequest {
  startDate?: string
  endDate?: string
  destination?: string
  purpose?: string
  notes?: string
  estimatedCost?: number
  costCenter?: string
  actualCost?: number
}

export const businessTripService = {
  async createTrip(data: CreateBusinessTripRequest): Promise<BusinessTripResponse> {
    const response = await apiClient.post<BusinessTripResponse>('/business-trips', data)
    return response.data
  },

  async updateTrip(id: string, data: UpdateBusinessTripRequest): Promise<BusinessTripResponse> {
    const response = await apiClient.put<BusinessTripResponse>(`/business-trips/${id}`, data)
    return response.data
  },

  async cancelTrip(id: string): Promise<void> {
    await apiClient.delete(`/business-trips/${id}`)
  },

  async getMyTrips(page = 0, size = 20): Promise<PageResponse<BusinessTripResponse>> {
    const response = await apiClient.get<PageResponse<BusinessTripResponse>>('/business-trips', {
      params: { page, size },
    })
    return response.data
  },

  async getTrip(id: string): Promise<BusinessTripResponse> {
    const response = await apiClient.get<BusinessTripResponse>(`/business-trips/${id}`)
    return response.data
  },

  async approveTrip(id: string, notes?: string): Promise<BusinessTripResponse> {
    const response = await apiClient.post<BusinessTripResponse>(`/business-trips/${id}/approve`, notes ? { notes } : {})
    return response.data
  },

  async rejectTrip(id: string, rejectionReason: string): Promise<BusinessTripResponse> {
    const response = await apiClient.post<BusinessTripResponse>(`/business-trips/${id}/reject`, { rejectionReason })
    return response.data
  },

  async completeTrip(id: string, actualCost?: number): Promise<BusinessTripResponse> {
    const response = await apiClient.post<BusinessTripResponse>(`/business-trips/${id}/complete`, null, {
      params: actualCost != null ? { actualCost } : undefined,
    })
    return response.data
  },

  async getPendingTrips(page = 0, size = 20): Promise<PageResponse<BusinessTripResponse>> {
    const response = await apiClient.get<PageResponse<BusinessTripResponse>>('/business-trips/pending', {
      params: { page, size },
    })
    return response.data
  },
}
