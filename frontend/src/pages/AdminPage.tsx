import { useState, useEffect, useCallback, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Users, ClipboardList, Settings, Plus, Edit, Trash2, Key, Wifi, Mail, FolderTree, Database, Download, Upload, RefreshCw } from 'lucide-react'
import adminService from '../services/adminService'
import type { AuditLogEntry, SystemSetting, CreateUserPayload, UpdateUserPayload, BackupInfo } from '../services/adminService'
import type { User } from '../types'

type Tab = 'users' | 'audit' | 'settings' | 'ldap' | 'backups'

const AVAILABLE_ROLES = ['EMPLOYEE', 'MANAGER', 'ADMIN', 'SUPER_ADMIN']

function TabButton({
  active,
  onClick,
  icon: Icon,
  label,
}: {
  active: boolean
  onClick: () => void
  icon: React.ElementType
  label: string
}) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
        active ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
      }`}
    >
      <Icon size={16} />
      {label}
    </button>
  )
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="p-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700" role="alert">
      {message}
    </div>
  )
}

function LoadingSpinner() {
  const { t } = useTranslation()
  return (
    <div className="flex items-center justify-center py-12 text-gray-500">
      <div className="w-6 h-6 border-2 border-primary-500 border-t-transparent rounded-full animate-spin mr-2" />
      {t('common.loading')}
    </div>
  )
}

interface UserModalProps {
  user: User | null
  onClose: () => void
  onSave: () => void
}

function UserModal({ user, onClose, onSave }: UserModalProps) {
  const { t } = useTranslation()
  const [allUsers, setAllUsers] = useState<User[]>([])
  const [form, setForm] = useState({
    email: user?.email ?? '',
    password: '',
    firstName: user?.firstName ?? '',
    lastName: user?.lastName ?? '',
    employeeNumber: user?.employeeNumber ?? '',
    phone: user?.phone ?? '',
    roles: user?.roles ?? ['EMPLOYEE'],
    isActive: user?.isActive ?? true,
    managerId: user?.managerId ?? '',
    substituteId: user?.substituteId ?? '',
  })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const isNew = user === null

  useEffect(() => {
    adminService.getUsers(0, 100).then((data) => setAllUsers(data.content)).catch(() => {})
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      if (isNew) {
        const payload: CreateUserPayload = {
          email: form.email,
          password: form.password,
          firstName: form.firstName,
          lastName: form.lastName,
          employeeNumber: form.employeeNumber || undefined,
          phone: form.phone || undefined,
          roles: form.roles,
        }
        await adminService.createUser(payload)
      } else {
        const payload: UpdateUserPayload = {
          firstName: form.firstName,
          lastName: form.lastName,
          phone: form.phone || undefined,
          isActive: form.isActive,
          managerId: form.managerId || undefined,
          substituteId: form.substituteId || undefined,
          employeeNumber: form.employeeNumber || undefined,
        }
        await adminService.updateUser(user.id, payload)
        if (JSON.stringify([...form.roles].sort()) !== JSON.stringify([...user.roles].sort())) {
          await adminService.assignRoles(user.id, form.roles)
        }
      }
      onSave()
    } catch {
      setError(t('admin.errors.save_failed'))
    } finally {
      setSaving(false)
    }
  }

  const toggleRole = (role: string) => {
    setForm((prev) => ({
      ...prev,
      roles: prev.roles.includes(role) ? prev.roles.filter((r) => r !== role) : [...prev.roles, role],
    }))
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
        <div className="p-6 border-b">
          <h2 className="text-lg font-semibold text-gray-900">
            {isNew ? t('admin.users.create') : t('admin.users.edit')}
          </h2>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="p-6 space-y-4">
            {error && <ErrorBanner message={error} />}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label htmlFor="modal-firstName" className="block text-sm font-medium text-gray-700 mb-1">{t('users.first_name')}</label>
                <input
                  id="modal-firstName"
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={form.firstName}
                  onChange={(e) => setForm((p) => ({ ...p, firstName: e.target.value }))}
                  required
                />
              </div>
              <div>
                <label htmlFor="modal-lastName" className="block text-sm font-medium text-gray-700 mb-1">{t('users.last_name')}</label>
                <input
                  id="modal-lastName"
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={form.lastName}
                  onChange={(e) => setForm((p) => ({ ...p, lastName: e.target.value }))}
                  required
                />
              </div>
            </div>
            {isNew && (
              <>
                <div>
                  <label htmlFor="modal-email" className="block text-sm font-medium text-gray-700 mb-1">{t('common.email')}</label>
                  <input
                    id="modal-email"
                    type="email"
                    className="w-full border rounded-lg px-3 py-2 text-sm"
                    value={form.email}
                    onChange={(e) => setForm((p) => ({ ...p, email: e.target.value }))}
                    required
                  />
                </div>
                <div>
                  <label htmlFor="modal-password" className="block text-sm font-medium text-gray-700 mb-1">{t('users.new_password')}</label>
                  <input
                    id="modal-password"
                    type="password"
                    className="w-full border rounded-lg px-3 py-2 text-sm"
                    value={form.password}
                    onChange={(e) => setForm((p) => ({ ...p, password: e.target.value }))}
                    required
                    minLength={8}
                  />
                </div>
              </>
            )}
            <div>
              <label htmlFor="modal-phone" className="block text-sm font-medium text-gray-700 mb-1">{t('common.phone')}</label>
              <input
                id="modal-phone"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.phone}
                onChange={(e) => setForm((p) => ({ ...p, phone: e.target.value }))}
              />
            </div>
            <div>
              <label htmlFor="modal-employeeNumber" className="block text-sm font-medium text-gray-700 mb-1">{t('users.employee_number')}</label>
              <input
                id="modal-employeeNumber"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.employeeNumber}
                onChange={(e) => setForm((p) => ({ ...p, employeeNumber: e.target.value }))}
              />
            </div>
            <div>
              <label htmlFor="modal-manager" className="block text-sm font-medium text-gray-700 mb-1">{t('users.manager')}</label>
              <select
                id="modal-manager"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.managerId}
                onChange={(e) => setForm((p) => ({ ...p, managerId: e.target.value }))}
              >
                <option value="">—</option>
                {allUsers.filter((u) => u.id !== user?.id && (u.roles.includes('MANAGER') || u.roles.includes('ADMIN') || u.roles.includes('SUPER_ADMIN'))).map((u) => (
                  <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>
                ))}
              </select>
            </div>
            <div>
              <label htmlFor="modal-substitute" className="block text-sm font-medium text-gray-700 mb-1">{t('users.substitute')}</label>
              <select
                id="modal-substitute"
                className="w-full border rounded-lg px-3 py-2 text-sm"
                value={form.substituteId}
                onChange={(e) => setForm((p) => ({ ...p, substituteId: e.target.value }))}
              >
                <option value="">—</option>
                {allUsers.filter((u) => u.id !== user?.id && (u.roles.includes('MANAGER') || u.roles.includes('ADMIN') || u.roles.includes('SUPER_ADMIN'))).map((u) => (
                  <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>
                ))}
              </select>
            </div>
            <div>
              <div className="flex flex-wrap gap-2">
                {AVAILABLE_ROLES.map((role) => (
                  <label key={role} className="flex items-center gap-1.5 text-sm cursor-pointer">
                    <input
                      type="checkbox"
                      checked={form.roles.includes(role)}
                      onChange={() => toggleRole(role)}
                      className="rounded border-gray-300"
                    />
                    {role}
                  </label>
                ))}
              </div>
            </div>
            {!isNew && (
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={form.isActive}
                  onChange={(e) => setForm((p) => ({ ...p, isActive: e.target.checked }))}
                  className="rounded border-gray-300"
                />
                {t('users.active')}
              </label>
            )}
          </div>
          <div className="px-6 py-4 border-t flex justify-end gap-3">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
            >
              {saving ? t('common.loading') : t('common.save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function RfidModal({ user, onClose, onSave }: { user: User; onClose: () => void; onSave: () => void }) {
  const { t } = useTranslation()
  const [rfidTagId, setRfidTagId] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await adminService.updateRfid(user.id, rfidTagId || null)
      onSave()
    } catch {
      setError(t('admin.errors.save_failed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="p-6 border-b">
          <h2 className="text-lg font-semibold text-gray-900">{t('admin.users.update_rfid')}</h2>
          <p className="text-sm text-gray-500 mt-1">{user.firstName} {user.lastName}</p>
        </div>
        <form onSubmit={handleSubmit}>
          <div className="p-6 space-y-4">
            {error && <ErrorBanner message={error} />}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">{t('users.rfid_tag')}</label>
              <input
                className="w-full border rounded-lg px-3 py-2 text-sm font-mono"
                value={rfidTagId}
                onChange={(e) => setRfidTagId(e.target.value)}
                placeholder={t('admin.users.rfid_placeholder')}
              />
              <p className="mt-1 text-xs text-gray-500">{t('admin.users.rfid_hint')}</p>
            </div>
          </div>
          <div className="px-6 py-4 border-t flex justify-end gap-3">
            <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
            >
              {saving ? t('common.loading') : t('common.save')}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

function ResetPasswordModal({ user, onClose }: { user: User; onClose: () => void }) {
  const { t } = useTranslation()
  const [newPassword, setNewPassword] = useState('')
  const [confirmPwd, setConfirmPwd] = useState('')
  const [saving, setSaving] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (newPassword !== confirmPwd) {
      setError(t('settings.passwords_no_match'))
      return
    }
    setSaving(true)
    setError(null)
    try {
      await adminService.resetPassword(user.id, newPassword, confirmPwd)
      setSuccess(true)
    } catch {
      setError(t('admin.errors.save_failed'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
      <div className="bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="p-6 border-b">
          <h2 className="text-lg font-semibold text-gray-900">{t('users.reset_password')}</h2>
          <p className="text-sm text-gray-500 mt-1">{user.firstName} {user.lastName}</p>
        </div>
        {success ? (
          <div className="p-6">
            <p className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg p-3">
              {t('admin.users.password_reset_success')}
            </p>
            <div className="mt-4 flex justify-end">
              <button onClick={onClose} className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700">
                {t('common.confirm')}
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="p-6 space-y-4">
              {error && <ErrorBanner message={error} />}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('users.new_password')}</label>
                <input
                  type="password"
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={8}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('users.confirm_password')}</label>
                <input
                  type="password"
                  className="w-full border rounded-lg px-3 py-2 text-sm"
                  value={confirmPwd}
                  onChange={(e) => setConfirmPwd(e.target.value)}
                  required
                  minLength={8}
                />
              </div>
            </div>
            <div className="px-6 py-4 border-t flex justify-end gap-3">
              <button type="button" onClick={onClose} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                disabled={saving}
                className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
              >
                {saving ? t('common.loading') : t('users.reset_password')}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

function UsersTab() {
  const { t } = useTranslation()
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [modal, setModal] = useState<'create' | 'edit' | 'rfid' | 'password' | null>(null)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)

  const loadUsers = useCallback(
    async (pageNum = 0) => {
      setLoading(true)
      setError(null)
      try {
        const data = await adminService.getUsers(pageNum, 20)
        setUsers(data.content)
        setTotalPages(data.totalPages)
        setPage(pageNum)
      } catch {
        setError(t('admin.errors.load_failed'))
      } finally {
        setLoading(false)
      }
    },
    [t],
  )

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  const handleDelete = async (id: string) => {
    try {
      await adminService.deleteUser(id)
      setDeleteConfirm(null)
      await loadUsers(page)
    } catch {
      setError(t('admin.errors.delete_failed'))
    }
  }

  const filteredUsers = users.filter(
    (u) =>
      u.firstName.toLowerCase().includes(search.toLowerCase()) ||
      u.lastName.toLowerCase().includes(search.toLowerCase()) ||
      u.email.toLowerCase().includes(search.toLowerCase()),
  )

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <input
          type="search"
          placeholder={t('common.search')}
          aria-label={t('common.search')}
          className="border rounded-lg px-3 py-2 text-sm w-64"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button
          onClick={() => { setSelectedUser(null); setModal('create') }}
          className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700"
        >
          <Plus size={16} />
          {t('admin.users.create')}
        </button>
      </div>

      {error && <ErrorBanner message={error} />}

      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('common.name')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('common.email')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('users.roles')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('common.status')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredUsers.map((user) => (
                <tr key={user.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900">{user.firstName} {user.lastName}</td>
                  <td className="px-4 py-3 text-gray-500">{user.email}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      {user.roles.map((role) => (
                        <span key={role} className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                          {role}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${user.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-600'}`}>
                      {user.isActive ? t('users.active') : t('users.inactive')}
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      <button aria-label={t('common.edit')} onClick={() => { setSelectedUser(user); setModal('edit') }} className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded">
                        <Edit size={14} />
                      </button>
                      <button aria-label={t('users.rfid_tag')} onClick={() => { setSelectedUser(user); setModal('rfid') }} className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded">
                        <Wifi size={14} />
                      </button>
                      <button aria-label={t('users.reset_password')} onClick={() => { setSelectedUser(user); setModal('password') }} className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded">
                        <Key size={14} />
                      </button>
                      <button aria-label={t('common.delete')} onClick={() => setDeleteConfirm(user.id)} className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded">
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filteredUsers.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-500">{t('admin.users.no_users')}</td>
                </tr>
              )}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="px-4 py-3 border-t flex items-center justify-between">
              <button disabled={page === 0} onClick={() => loadUsers(page - 1)} className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 disabled:opacity-50">
                {t('common.previous')}
              </button>
              <span className="text-sm text-gray-500">{page + 1} / {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => loadUsers(page + 1)} className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 disabled:opacity-50">
                {t('common.next')}
              </button>
            </div>
          )}
        </div>
      )}

      {deleteConfirm && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">{t('common.confirm')}</h2>
            <p className="text-sm text-gray-500 mb-6">{t('users.delete_confirm')}</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setDeleteConfirm(null)} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">{t('common.cancel')}</button>
              <button onClick={() => handleDelete(deleteConfirm)} className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700">{t('common.delete')}</button>
            </div>
          </div>
        </div>
      )}

      {(modal === 'create' || modal === 'edit') && (
        <UserModal user={modal === 'edit' ? selectedUser : null} onClose={() => setModal(null)} onSave={() => { setModal(null); loadUsers(page) }} />
      )}
      {modal === 'rfid' && selectedUser && (
        <RfidModal user={selectedUser} onClose={() => setModal(null)} onSave={() => { setModal(null); loadUsers(page) }} />
      )}
      {modal === 'password' && selectedUser && (
        <ResetPasswordModal user={selectedUser} onClose={() => setModal(null)} />
      )}
    </div>
  )
}

function AuditLogTab() {
  const { t } = useTranslation()
  const [entries, setEntries] = useState<AuditLogEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)

  const loadLog = useCallback(
    async (pageNum = 0) => {
      setLoading(true)
      setError(null)
      try {
        const data = await adminService.getAuditLog(pageNum, 50)
        setEntries(data.content)
        setTotalPages(data.totalPages)
        setPage(pageNum)
      } catch {
        setError(t('admin.errors.load_failed'))
      } finally {
        setLoading(false)
      }
    },
    [t],
  )

  useEffect(() => {
    loadLog()
  }, [loadLog])

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'medium' })

  return (
    <div className="space-y-4">
      {error && <ErrorBanner message={error} />}
      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('common.date')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.audit.user')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.audit.action')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.audit.entity')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.audit.ip')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {entries.map((entry) => (
                <tr key={entry.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{formatDate(entry.createdAt)}</td>
                  <td className="px-4 py-3">
                    {entry.userFullName ? (
                      <div>
                        <div className="font-medium text-gray-900">{entry.userFullName}</div>
                        <div className="text-xs text-gray-500">{entry.userEmail}</div>
                      </div>
                    ) : (
                      <span className="text-gray-400">{t('common.unknown')}</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-700 font-mono">
                      {entry.action}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">
                    {entry.entityType ? `${entry.entityType} ${entry.entityId?.slice(0, 8) ?? ''}...` : '—'}
                  </td>
                  <td className="px-4 py-3 text-gray-500 font-mono text-xs">{entry.ipAddress ?? '—'}</td>
                </tr>
              ))}
              {entries.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-4 py-8 text-center text-gray-500">{t('admin.audit.no_entries')}</td>
                </tr>
              )}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="px-4 py-3 border-t flex items-center justify-between">
              <button disabled={page === 0} onClick={() => loadLog(page - 1)} className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 disabled:opacity-50">
                {t('common.previous')}
              </button>
              <span className="text-sm text-gray-500">{page + 1} / {totalPages}</span>
              <button disabled={page >= totalPages - 1} onClick={() => loadLog(page + 1)} className="px-3 py-1.5 text-sm border rounded hover:bg-gray-50 disabled:opacity-50">
                {t('common.next')}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function SettingsTab() {
  const { t } = useTranslation()
  const [settings, setSettings] = useState<SystemSetting[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState<{ key: string; value: string } | null>(null)
  const [saving, setSaving] = useState(false)
  const [testMailEmail, setTestMailEmail] = useState('')
  const [testMailSending, setTestMailSending] = useState(false)
  const [testMailResult, setTestMailResult] = useState<{ status: 'ok' | 'error'; message: string } | null>(null)

  const loadSettings = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setSettings(await adminService.getSettings())
    } catch {
      setError(t('admin.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadSettings()
  }, [loadSettings])

  const handleSave = async () => {
    if (!editing) return
    setSaving(true)
    setError(null)
    try {
      const updated = await adminService.updateSetting(editing.key, editing.value)
      setSettings((prev) => prev.map((s) => (s.key === editing.key ? updated : s)))
      setEditing(null)
    } catch {
      setError(t('admin.errors.save_failed'))
    } finally {
      setSaving(false)
    }
  }

  const handleSendTestMail = async (e: React.FormEvent) => {
    e.preventDefault()
    setTestMailSending(true)
    setTestMailResult(null)
    try {
      const result = await adminService.sendTestMail(testMailEmail)
      setTestMailResult({ status: 'ok', message: result.message })
    } catch (err: unknown) {
      let msg = t('admin.errors.save_failed')
      if (err && typeof err === 'object' && 'response' in err) {
        const axiosErr = err as { response?: { data?: { message?: string } } }
        if (axiosErr.response?.data?.message) {
          msg = axiosErr.response.data.message
        }
      } else if (err instanceof Error) {
        msg = err.message
      }
      setTestMailResult({ status: 'error', message: msg })
    } finally {
      setTestMailSending(false)
    }
  }

  return (
    <div className="space-y-6">
      {error && <ErrorBanner message={error} />}
      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.settings.key')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.settings.value')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('admin.settings.description')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('common.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {settings.map((setting) => (
                <tr key={setting.key} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs text-gray-700">{setting.key}</td>
                  <td className="px-4 py-3">
                    {editing?.key === setting.key ? (
                      <input
                        className="border rounded px-2 py-1 text-sm w-full"
                        value={editing.value}
                        onChange={(e) => setEditing((prev) => prev ? { ...prev, value: e.target.value } : null)}
                        autoFocus
                      />
                    ) : (
                      <span className="font-medium text-gray-900">{setting.value}</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-gray-500 text-xs">{setting.description ?? '—'}</td>
                  <td className="px-4 py-3">
                    {editing?.key === setting.key ? (
                      <div className="flex items-center gap-1">
                        <button onClick={handleSave} disabled={saving} className="px-2 py-1 text-xs bg-primary-600 text-white rounded hover:bg-primary-700 disabled:opacity-50">
                          {t('common.save')}
                        </button>
                        <button onClick={() => setEditing(null)} className="px-2 py-1 text-xs text-gray-600 border rounded hover:bg-gray-50">
                          {t('common.cancel')}
                        </button>
                      </div>
                    ) : (
                      <button onClick={() => setEditing({ key: setting.key, value: setting.value })} className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded">
                        <Edit size={14} />
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Test Mail */}
      <div className="bg-white rounded-xl border p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-3 flex items-center gap-2">
          <Mail size={16} />
          {t('admin.settings.test_mail')}
        </h3>
        <p className="text-xs text-gray-500 mb-4">{t('admin.settings.test_mail_description')}</p>
        <form onSubmit={handleSendTestMail} className="flex items-end gap-3">
          <div className="flex-1">
            <label htmlFor="test-mail-email" className="block text-sm font-medium text-gray-700 mb-1">
              {t('common.email')}
            </label>
            <input
              id="test-mail-email"
              type="email"
              value={testMailEmail}
              onChange={(e) => setTestMailEmail(e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
              placeholder="admin@example.com"
              required
            />
          </div>
          <button
            type="submit"
            disabled={testMailSending}
            className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-50 whitespace-nowrap"
          >
            <Mail size={14} />
            {testMailSending ? t('common.loading') : t('admin.settings.send_test')}
          </button>
        </form>
        {testMailResult && (
          <div className={`mt-3 p-3 rounded-lg text-sm ${
            testMailResult.status === 'ok'
              ? 'bg-green-50 border border-green-200 text-green-700'
              : 'bg-red-50 border border-red-200 text-red-700'
          }`}>
            {testMailResult.message}
          </div>
        )}
      </div>
    </div>
  )
}

interface LdapConfig {
  enabled: boolean
  url: string
  baseDn: string
  userSearchBase: string
  userSearchFilter: string
  groupSearchBase: string
  groupSearchFilter: string
  managerDn: string
  managerPassword: string
  activeDirectoryMode: boolean
  activeDirectoryDomain: string
  roleMapping: string
  emailAttribute: string
  firstNameAttribute: string
  lastNameAttribute: string
  employeeNumberAttribute: string
}

const EMPTY_LDAP_CONFIG: LdapConfig = {
  enabled: false,
  url: '',
  baseDn: '',
  userSearchBase: '',
  userSearchFilter: '',
  groupSearchBase: '',
  groupSearchFilter: '',
  managerDn: '',
  managerPassword: '',
  activeDirectoryMode: false,
  activeDirectoryDomain: '',
  roleMapping: '',
  emailAttribute: '',
  firstNameAttribute: '',
  lastNameAttribute: '',
  employeeNumberAttribute: '',
}

function LdapTab() {
  const { t } = useTranslation()
  const [config, setConfig] = useState<LdapConfig>(EMPTY_LDAP_CONFIG)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    const load = async () => {
      try {
        const data = await adminService.getLdapConfig()
        setConfig({ ...EMPTY_LDAP_CONFIG, ...data })
      } catch {
        setError(t('admin.ldap.load_error'))
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [t])

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      await adminService.updateLdapConfig(config as unknown as Record<string, unknown>)
      setSuccess(t('admin.ldap.save_success'))
    } catch {
      setError(t('admin.ldap.save_error'))
    } finally {
      setSaving(false)
    }
  }

  const updateField = (field: keyof LdapConfig, value: string | boolean) => {
    setConfig((prev) => ({ ...prev, [field]: value }))
  }

  if (loading) return <LoadingSpinner />

  const textFields: { key: keyof LdapConfig; label: string; type?: string }[] = [
    { key: 'url', label: t('admin.ldap.url') },
    { key: 'baseDn', label: t('admin.ldap.base_dn') },
    { key: 'userSearchBase', label: t('admin.ldap.user_search_base') },
    { key: 'userSearchFilter', label: t('admin.ldap.user_search_filter') },
    { key: 'groupSearchBase', label: t('admin.ldap.group_search_base') },
    { key: 'groupSearchFilter', label: t('admin.ldap.group_search_filter') },
    { key: 'managerDn', label: t('admin.ldap.manager_dn') },
    { key: 'managerPassword', label: t('admin.ldap.manager_password'), type: 'password' },
    { key: 'activeDirectoryDomain', label: t('admin.ldap.ad_domain') },
    { key: 'roleMapping', label: t('admin.ldap.role_mapping') },
    { key: 'emailAttribute', label: t('admin.ldap.email_attribute') },
    { key: 'firstNameAttribute', label: t('admin.ldap.first_name_attribute') },
    { key: 'lastNameAttribute', label: t('admin.ldap.last_name_attribute') },
    { key: 'employeeNumberAttribute', label: t('admin.ldap.employee_number_attribute') },
  ]

  return (
    <div className="bg-white rounded-xl border p-6">
      <h2 className="text-lg font-semibold text-gray-900 mb-4">{t('admin.ldap.title')}</h2>
      <form onSubmit={handleSave} className="space-y-4">
        <div className="flex items-center gap-2">
          <input
            id="ldapEnabled"
            type="checkbox"
            checked={config.enabled}
            onChange={(e) => updateField('enabled', e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <label htmlFor="ldapEnabled" className="text-sm font-medium text-gray-700">
            {t('admin.ldap.enabled')}
          </label>
        </div>

        {textFields.map(({ key, label, type }) => (
          <div key={key}>
            <label htmlFor={`ldap-${key}`} className="block text-sm font-medium text-gray-700 mb-1">
              {label}
            </label>
            <input
              id={`ldap-${key}`}
              type={type || 'text'}
              value={config[key] as string}
              onChange={(e) => updateField(key, e.target.value)}
              className="w-full border rounded-lg px-3 py-2 text-sm"
            />
          </div>
        ))}

        <div className="flex items-center gap-2">
          <input
            id="ldapAdMode"
            type="checkbox"
            checked={config.activeDirectoryMode}
            onChange={(e) => updateField('activeDirectoryMode', e.target.checked)}
            className="h-4 w-4 rounded border-gray-300"
          />
          <label htmlFor="ldapAdMode" className="text-sm font-medium text-gray-700">
            {t('admin.ldap.ad_mode')}
          </label>
        </div>

        {error && <ErrorBanner message={error} />}
        {success && (
          <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
            {success}
          </div>
        )}

        <button
          type="submit"
          disabled={saving}
          className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50"
        >
          {saving ? t('common.loading') : t('common.save')}
        </button>
      </form>
    </div>
  )
}

function BackupsTab() {
  const { t } = useTranslation()
  const [backups, setBackups] = useState<BackupInfo[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [restoring, setRestoring] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [confirmRestore, setConfirmRestore] = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<string | null>(null)
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const showSuccess = (msg: string) => {
    setSuccess(msg)
    setTimeout(() => setSuccess(null), 5000)
  }

  const loadBackups = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await adminService.listBackups()
      setBackups(data)
    } catch {
      setError(t('backup.error_list'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadBackups()
  }, [loadBackups])

  const handleCreate = async () => {
    setCreating(true)
    setError(null)
    try {
      await adminService.createBackup()
      showSuccess(t('backup.backup_created'))
      await loadBackups()
    } catch {
      setError(t('backup.error_create'))
    } finally {
      setCreating(false)
    }
  }

  const handleDownload = async (filename: string) => {
    try {
      await adminService.downloadBackup(filename)
    } catch {
      setError(t('backup.error_download'))
    }
  }

  const handleRestore = async (filename: string) => {
    setRestoring(filename)
    setConfirmRestore(null)
    setError(null)
    try {
      await adminService.restoreBackup(filename)
      showSuccess(t('backup.restore_started'))
    } catch {
      setError(t('backup.error_restore'))
    } finally {
      setRestoring(null)
    }
  }

  const handleDelete = async (filename: string) => {
    setDeleting(filename)
    setConfirmDelete(null)
    setError(null)
    try {
      await adminService.deleteBackup(filename)
      showSuccess(t('backup.backup_deleted'))
      await loadBackups()
    } catch {
      setError(t('backup.error_delete'))
    } finally {
      setDeleting(null)
    }
  }

  const handleUploadRestore = async () => {
    const file = fileInputRef.current?.files?.[0]
    if (!file) {
      setError(t('backup.error_no_file'))
      return
    }
    setUploading(true)
    setError(null)
    try {
      await adminService.restoreFromUpload(file)
      showSuccess(t('backup.upload_restore_success'))
      if (fileInputRef.current) fileInputRef.current.value = ''
      await loadBackups()
    } catch {
      setError(t('backup.error_upload'))
    } finally {
      setUploading(false)
    }
  }

  const formatSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  }

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleString(undefined, { dateStyle: 'short', timeStyle: 'medium' })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-gray-900">{t('backup.title')}</h2>
          <p className="text-sm text-gray-500 mt-1">{t('backup.subtitle')}</p>
        </div>
        <button
          onClick={handleCreate}
          disabled={creating}
          className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-50"
        >
          {creating ? (
            <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
          ) : (
            <Plus size={16} />
          )}
          {creating ? t('backup.creating') : t('backup.create')}
        </button>
      </div>

      {success && (
        <div className="p-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700" role="status">
          {success}
        </div>
      )}
      {error && <ErrorBanner message={error} />}

      {loading ? (
        <LoadingSpinner />
      ) : (
        <div className="bg-white rounded-xl border overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('backup.filename')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('backup.size')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('backup.date')}</th>
                <th className="px-4 py-3 text-left font-medium text-gray-700">{t('backup.actions')}</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {backups.map((backup) => (
                <tr key={backup.filename} className="hover:bg-gray-50">
                  <td className="px-4 py-3 font-medium text-gray-900 font-mono text-xs">{backup.filename}</td>
                  <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{formatSize(backup.sizeBytes)}</td>
                  <td className="px-4 py-3 text-gray-500 whitespace-nowrap">{formatDate(backup.createdAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      <button
                        aria-label={t('backup.download')}
                        onClick={() => handleDownload(backup.filename)}
                        className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded"
                      >
                        <Download size={14} />
                      </button>
                      <button
                        aria-label={t('backup.restore')}
                        onClick={() => setConfirmRestore(backup.filename)}
                        disabled={restoring === backup.filename}
                        className="p-1.5 text-gray-500 hover:text-primary-600 hover:bg-gray-100 rounded disabled:opacity-50"
                      >
                        {restoring === backup.filename ? (
                          <div className="w-3.5 h-3.5 border-2 border-primary-500 border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <RefreshCw size={14} />
                        )}
                      </button>
                      <button
                        aria-label={t('backup.delete')}
                        onClick={() => setConfirmDelete(backup.filename)}
                        disabled={deleting === backup.filename}
                        className="p-1.5 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded disabled:opacity-50"
                      >
                        {deleting === backup.filename ? (
                          <div className="w-3.5 h-3.5 border-2 border-red-500 border-t-transparent rounded-full animate-spin" />
                        ) : (
                          <Trash2 size={14} />
                        )}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {backups.length === 0 && (
                <tr>
                  <td colSpan={4} className="px-4 py-8 text-center text-gray-500">{t('backup.no_backups')}</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <div className="bg-white rounded-xl border p-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-1">{t('backup.upload_restore')}</h3>
        <p className="text-xs text-gray-500 mb-4">{t('backup.upload_hint')}</p>
        <div className="flex items-center gap-3">
          <input
            ref={fileInputRef}
            type="file"
            accept=".sql.gz,.sql,.gz"
            className="text-sm text-gray-500 file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-medium file:bg-gray-100 file:text-gray-700 hover:file:bg-gray-200"
            aria-label={t('backup.select_file')}
          />
          <button
            onClick={handleUploadRestore}
            disabled={uploading}
            className="flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700 disabled:opacity-50"
          >
            {uploading ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <Upload size={16} />
            )}
            {t('backup.upload_restore')}
          </button>
        </div>
      </div>

      {confirmRestore && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">{t('backup.confirm_restore_title')}</h2>
            <p className="text-sm text-gray-500 mb-6">{t('backup.confirm_restore_message')}</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmRestore(null)} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">{t('common.cancel')}</button>
              <button onClick={() => handleRestore(confirmRestore)} className="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700">{t('backup.restore')}</button>
            </div>
          </div>
        </div>
      )}

      {confirmDelete && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4" role="dialog" aria-modal="true">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
            <h2 className="text-lg font-semibold text-gray-900 mb-2">{t('backup.confirm_delete_title')}</h2>
            <p className="text-sm text-gray-500 mb-6">{t('backup.confirm_delete_message')}</p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setConfirmDelete(null)} className="px-4 py-2 text-sm text-gray-700 border rounded-lg hover:bg-gray-50">{t('common.cancel')}</button>
              <button onClick={() => handleDelete(confirmDelete)} className="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700">{t('common.delete')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default function AdminPage() {
  const { t } = useTranslation()
  const [activeTab, setActiveTab] = useState<Tab>('users')

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">{t('admin.title')}</h1>
        <p className="text-sm text-gray-500 mt-1">{t('admin.subtitle')}</p>
      </div>

      <div className="flex items-center gap-2 mb-6 border-b pb-4">
        <TabButton active={activeTab === 'users'} onClick={() => setActiveTab('users')} icon={Users} label={t('admin.users_tab')} />
        <TabButton active={activeTab === 'audit'} onClick={() => setActiveTab('audit')} icon={ClipboardList} label={t('admin.audit_log')} />
        <TabButton active={activeTab === 'settings'} onClick={() => setActiveTab('settings')} icon={Settings} label={t('admin.settings_tab')} />
        <TabButton active={activeTab === 'ldap'} onClick={() => setActiveTab('ldap')} icon={FolderTree} label={t('admin.ldap_tab')} />
        <TabButton active={activeTab === 'backups'} onClick={() => setActiveTab('backups')} icon={Database} label={t('admin.backups_tab')} />
      </div>

      {activeTab === 'users' && <UsersTab />}
      {activeTab === 'audit' && <AuditLogTab />}
      {activeTab === 'settings' && <SettingsTab />}
      {activeTab === 'ldap' && <LdapTab />}
      {activeTab === 'backups' && <BackupsTab />}
    </div>
  )
}
