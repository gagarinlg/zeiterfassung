import apiClient from './apiClient'
import type { VacationRequest, VacationBalance } from '../types'

export interface CreateVacationRequestData {
  startDate: string
  endDate: string
  isHalfDayStart: boolean
  isHalfDayEnd: boolean
  notes?: string
}

export interface UpdateVacationRequestData {
  startDate?: string
  endDate?: string
  isHalfDayStart?: boolean
  isHalfDayEnd?: boolean
  notes?: string
}

export interface VacationRequestParams {
  year?: number
  status?: string
  page?: number
  size?: number
}

export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  pageNumber: number
  pageSize: number
}

export interface PublicHoliday {
  id: string
  date: string
  name: string
  stateCode?: string
  isRecurring: boolean
}

export interface VacationCalendar {
  year: number
  month: number
  ownRequests: VacationRequest[]
  teamRequests: VacationRequest[]
  publicHolidays: PublicHoliday[]
}

export const vacationService = {
  async createRequest(data: CreateVacationRequestData): Promise<VacationRequest> {
    const response = await apiClient.post<VacationRequest>('/vacation/requests', data)
    return response.data
  },

  async updateRequest(id: string, data: UpdateVacationRequestData): Promise<VacationRequest> {
    const response = await apiClient.put<VacationRequest>(`/vacation/requests/${id}`, data)
    return response.data
  },

  async cancelRequest(id: string): Promise<void> {
    await apiClient.delete(`/vacation/requests/${id}`)
  },

  async getRequests(params?: VacationRequestParams): Promise<PagedResponse<VacationRequest>> {
    const response = await apiClient.get<PagedResponse<VacationRequest>>('/vacation/requests', { params })
    return response.data
  },

  async getRequest(id: string): Promise<VacationRequest> {
    const response = await apiClient.get<VacationRequest>(`/vacation/requests/${id}`)
    return response.data
  },

  async getBalance(year?: number): Promise<VacationBalance & { pendingDays: number }> {
    const response = await apiClient.get<VacationBalance & { pendingDays: number }>('/vacation/balance', {
      params: year ? { year } : undefined,
    })
    return response.data
  },

  async getUserBalance(userId: string, year?: number): Promise<VacationBalance & { pendingDays: number }> {
    const response = await apiClient.get<VacationBalance & { pendingDays: number }>(`/vacation/balance/${userId}`, {
      params: year ? { year } : undefined,
    })
    return response.data
  },

  async getPendingRequests(params?: { page?: number; size?: number; allRequests?: boolean }): Promise<PagedResponse<VacationRequest>> {
    const response = await apiClient.get<PagedResponse<VacationRequest>>('/vacation/pending', { params })
    return response.data
  },

  async approveRequest(id: string): Promise<VacationRequest> {
    const response = await apiClient.post<VacationRequest>(`/vacation/requests/${id}/approve`)
    return response.data
  },

  async rejectRequest(id: string, rejectionReason: string): Promise<VacationRequest> {
    const response = await apiClient.post<VacationRequest>(`/vacation/requests/${id}/reject`, { rejectionReason })
    return response.data
  },

  async getHolidays(year: number, stateCode?: string): Promise<PublicHoliday[]> {
    const response = await apiClient.get<PublicHoliday[]>('/vacation/holidays', {
      params: { year, stateCode },
    })
    return response.data
  },

  async getTeamCalendar(year: number, month: number): Promise<VacationCalendar> {
    const response = await apiClient.get<VacationCalendar>('/vacation/calendar', { params: { year, month } })
    return response.data
  },
}
