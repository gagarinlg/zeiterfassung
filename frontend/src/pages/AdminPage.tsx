import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { Users, ClipboardList, Settings, Plus, Edit, Trash2, Key, Wifi } from 'lucide-react'
import adminService from '../services/adminService'
import type { AuditLogEntry, SystemSetting, CreateUserPayload, UpdateUserPayload } from '../services/adminService'
import type { User } from '../types'

type Tab = 'users' | 'audit' | 'settings'

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
                {allUsers.filter((u) => u.id !== user?.id).map((u) => (
                  <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">{t('users.roles')}</label>
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
      </div>

      {activeTab === 'users' && <UsersTab />}
      {activeTab === 'audit' && <AuditLogTab />}
      {activeTab === 'settings' && <SettingsTab />}
    </div>
  )
}
