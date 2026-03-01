import apiClient from './apiClient'
import type { User, PageResponse } from '../types'

export interface AuditLogEntry {
  id: string
  userId: string | null
  userEmail: string | null
  userFullName: string | null
  action: string
  entityType: string | null
  entityId: string | null
  ipAddress: string | null
  userAgent: string | null
  createdAt: string
}

export interface SystemSetting {
  key: string
  value: string
  description: string | null
  updatedAt: string | null
}

export interface CreateUserPayload {
  email: string
  password: string
  firstName: string
  lastName: string
  employeeNumber?: string
  phone?: string
  managerId?: string
  roles: string[]
}

export interface UpdateUserPayload {
  firstName?: string
  lastName?: string
  phone?: string
  managerId?: string
  substituteId?: string
  isActive?: boolean
  dateFormat?: string
  timeFormat?: string
  employeeNumber?: string
}

export interface BackupInfo {
  filename: string
  sizeBytes: number
  createdAt: string
}

export interface RestoreResponse {
  status: string
  message: string
}

const adminService = {
  // Audit log
  getAuditLog: (page = 0, size = 50) =>
    apiClient
      .get<PageResponse<AuditLogEntry>>('/admin/audit-log', { params: { page, size } })
      .then((r) => r.data),

  getAuditLogByUser: (userId: string, page = 0, size = 50) =>
    apiClient
      .get<PageResponse<AuditLogEntry>>(`/admin/audit-log/user/${userId}`, { params: { page, size } })
      .then((r) => r.data),

  // System settings
  getSettings: () =>
    apiClient.get<SystemSetting[]>('/admin/settings').then((r) => r.data),

  updateSetting: (key: string, value: string) =>
    apiClient.put<SystemSetting>(`/admin/settings/${key}`, { value }).then((r) => r.data),

  // User management (proxies to /users endpoints)
  getUsers: (page = 0, size = 20, sortBy = 'lastName', sortDir = 'asc') =>
    apiClient
      .get<PageResponse<User>>('/users', { params: { page, size, sortBy, sortDir } })
      .then((r) => r.data),

  getUserById: (id: string) =>
    apiClient.get<User>(`/users/${id}`).then((r) => r.data),

  createUser: (payload: CreateUserPayload) =>
    apiClient.post<User>('/users', payload).then((r) => r.data),

  updateUser: (id: string, payload: UpdateUserPayload) =>
    apiClient.put<User>(`/users/${id}`, payload).then((r) => r.data),

  deleteUser: (id: string) =>
    apiClient.delete(`/users/${id}`).then((r) => r.data),

  assignRoles: (id: string, roles: string[]) =>
    apiClient.put<User>(`/users/${id}/roles`, { roles }).then((r) => r.data),

  updateRfid: (id: string, rfidTagId: string | null) =>
    apiClient.put<User>(`/users/${id}/rfid`, { rfidTagId }).then((r) => r.data),

  resetPassword: (id: string, newPassword: string, confirmPassword: string) =>
    apiClient.put(`/users/${id}/password/reset`, { newPassword, confirmPassword }).then((r) => r.data),

  // LDAP config
  getLdapConfig: () =>
    apiClient.get('/admin/ldap').then((r) => r.data),

  updateLdapConfig: (config: Record<string, unknown>) =>
    apiClient.put('/admin/ldap', config).then((r) => r.data),

  // Backup management
  listBackups: () =>
    apiClient.get<BackupInfo[]>('/admin/backups').then((r) => r.data),

  createBackup: () =>
    apiClient.post<BackupInfo>('/admin/backups').then((r) => r.data),

  downloadBackup: (filename: string) =>
    apiClient.get(`/admin/backups/${encodeURIComponent(filename)}`, { responseType: 'blob' }).then((r) => {
      const url = window.URL.createObjectURL(new Blob([r.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', filename)
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    }),

  restoreBackup: (filename: string) =>
    apiClient.post<RestoreResponse>(`/admin/backups/restore/${encodeURIComponent(filename)}`).then((r) => r.data),

  restoreFromUpload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<RestoreResponse>('/admin/backups/restore/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },

  deleteBackup: (filename: string) =>
    apiClient.delete(`/admin/backups/${encodeURIComponent(filename)}`).then((r) => r.data),

  // Self-service profile update
  updateOwnProfile: (payload: UpdateUserPayload) =>
    apiClient.put<User>('/users/me', payload).then((r) => r.data),

  // Self-service password change
  changeOwnPassword: (userId: string, currentPassword: string, newPassword: string, confirmPassword: string) =>
    apiClient.put(`/users/${userId}/password`, { currentPassword, newPassword, confirmPassword }).then((r) => r.data),

  // Test mail
  sendTestMail: (recipientEmail: string) =>
    apiClient.post<{ status: string; message: string }>('/admin/mail/test', { recipientEmail }).then((r) => r.data),
}

export default adminService
