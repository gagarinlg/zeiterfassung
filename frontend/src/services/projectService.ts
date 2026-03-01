import apiClient from './apiClient'
import type { PageResponse } from '../types'

export interface ProjectResponse {
  id: string
  name: string
  code: string
  description: string | null
  costCenter: string | null
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateProjectRequest {
  name: string
  code: string
  description?: string
  costCenter?: string
}

export interface UpdateProjectRequest {
  name?: string
  code?: string
  description?: string
  costCenter?: string
  isActive?: boolean
}

export interface TimeAllocationResponse {
  id: string
  userId: string
  userName: string
  projectId: string
  projectName: string
  projectCode: string
  date: string
  minutes: number
  notes: string | null
  createdAt: string
  updatedAt: string
}

export interface CreateTimeAllocationRequest {
  projectId: string
  date: string
  minutes: number
  notes?: string
}

export interface UpdateTimeAllocationRequest {
  projectId?: string
  date?: string
  minutes?: number
  notes?: string
}

export const projectService = {
  async createProject(data: CreateProjectRequest): Promise<ProjectResponse> {
    const response = await apiClient.post<ProjectResponse>('/projects', data)
    return response.data
  },

  async updateProject(id: string, data: UpdateProjectRequest): Promise<ProjectResponse> {
    const response = await apiClient.put<ProjectResponse>(`/projects/${id}`, data)
    return response.data
  },

  async getProject(id: string): Promise<ProjectResponse> {
    const response = await apiClient.get<ProjectResponse>(`/projects/${id}`)
    return response.data
  },

  async getProjects(activeOnly = true, page = 0, size = 20): Promise<PageResponse<ProjectResponse>> {
    const response = await apiClient.get<PageResponse<ProjectResponse>>('/projects', {
      params: { activeOnly, page, size },
    })
    return response.data
  },

  async createAllocation(data: CreateTimeAllocationRequest): Promise<TimeAllocationResponse> {
    const response = await apiClient.post<TimeAllocationResponse>('/projects/allocations', data)
    return response.data
  },

  async updateAllocation(id: string, data: UpdateTimeAllocationRequest): Promise<TimeAllocationResponse> {
    const response = await apiClient.put<TimeAllocationResponse>(`/projects/allocations/${id}`, data)
    return response.data
  },

  async deleteAllocation(id: string): Promise<void> {
    await apiClient.delete(`/projects/allocations/${id}`)
  },

  async getMyAllocations(page = 0, size = 20): Promise<PageResponse<TimeAllocationResponse>> {
    const response = await apiClient.get<PageResponse<TimeAllocationResponse>>('/projects/allocations', {
      params: { page, size },
    })
    return response.data
  },

  async getAllocationsByDate(date: string): Promise<TimeAllocationResponse[]> {
    const response = await apiClient.get<TimeAllocationResponse[]>('/projects/allocations/by-date', {
      params: { date },
    })
    return response.data
  },

  async getAllocationsByRange(start: string, end: string): Promise<TimeAllocationResponse[]> {
    const response = await apiClient.get<TimeAllocationResponse[]>('/projects/allocations/by-range', {
      params: { start, end },
    })
    return response.data
  },
}
