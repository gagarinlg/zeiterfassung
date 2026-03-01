import { useState, useEffect, useCallback } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'
import { projectService } from '../services/projectService'
import type { ProjectResponse, TimeAllocationResponse } from '../services/projectService'
import { Trash2, Plus } from 'lucide-react'

type Tab = 'allocations' | 'new_allocation' | 'projects'

export default function ProjectsPage() {
  const { t } = useTranslation()
  const { hasPermission } = useAuth()
  const [activeTab, setActiveTab] = useState<Tab>('allocations')
  const [projects, setProjects] = useState<ProjectResponse[]>([])
  const [allocations, setAllocations] = useState<TimeAllocationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [actionLoading, setActionLoading] = useState<string | null>(null)

  const [allocationForm, setAllocationForm] = useState({
    projectId: '',
    date: new Date().toISOString().split('T')[0],
    minutes: '',
    notes: '',
  })

  const [projectForm, setProjectForm] = useState({
    name: '',
    code: '',
    description: '',
    costCenter: '',
  })

  const isAdmin = hasPermission('admin.users.manage')

  const loadData = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [projectsResult, allocationsResult] = await Promise.all([
        projectService.getProjects(true, 0, 100),
        projectService.getMyAllocations(0, 100),
      ])
      setProjects(projectsResult.content)
      setAllocations(allocationsResult.content)
    } catch {
      setError(t('projects.errors.load_failed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleCreateAllocation = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await projectService.createAllocation({
        projectId: allocationForm.projectId,
        date: allocationForm.date,
        minutes: parseInt(allocationForm.minutes, 10),
        notes: allocationForm.notes || undefined,
      })
      setSuccess(t('projects.success.allocation_created'))
      setAllocationForm({ projectId: '', date: new Date().toISOString().split('T')[0], minutes: '', notes: '' })
      setActiveTab('allocations')
      await loadData()
    } catch {
      setError(t('projects.errors.allocation_create_failed'))
    }
  }

  const handleDeleteAllocation = async (id: string) => {
    setActionLoading(id)
    setError(null)
    try {
      await projectService.deleteAllocation(id)
      setSuccess(t('projects.success.allocation_deleted'))
      await loadData()
    } catch {
      setError(t('projects.errors.allocation_delete_failed'))
    } finally {
      setActionLoading(null)
    }
  }

  const handleCreateProject = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    try {
      await projectService.createProject({
        name: projectForm.name,
        code: projectForm.code,
        description: projectForm.description || undefined,
        costCenter: projectForm.costCenter || undefined,
      })
      setSuccess(t('projects.success.project_created'))
      setProjectForm({ name: '', code: '', description: '', costCenter: '' })
      await loadData()
    } catch {
      setError(t('projects.errors.project_create_failed'))
    }
  }

  const formatMinutes = (minutes: number): string => {
    const h = Math.floor(minutes / 60)
    const m = minutes % 60
    return `${h}h ${m.toString().padStart(2, '0')}m`
  }

  const tabs: { key: Tab; label: string; show: boolean }[] = [
    { key: 'allocations', label: t('projects.tabs.allocations'), show: true },
    { key: 'new_allocation', label: t('projects.tabs.new_allocation'), show: true },
    { key: 'projects', label: t('projects.tabs.manage_projects'), show: isAdmin },
  ]

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('projects.title')}</h1>

      {error && (
        <div role="alert" className="mb-4 p-3 bg-red-50 border border-red-200 rounded text-red-700 text-sm">
          {error}
        </div>
      )}
      {success && (
        <div role="status" className="mb-4 p-3 bg-green-50 border border-green-200 rounded text-green-700 text-sm">
          {success}
        </div>
      )}

      <nav className="border-b border-gray-200 mb-6">
        <div className="flex gap-6">
          {tabs.filter((tab) => tab.show).map((tab) => (
            <button
              key={tab.key}
              onClick={() => { setActiveTab(tab.key); setSuccess(null) }}
              className={`pb-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-primary-600 text-primary-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </nav>

      {/* Allocations List */}
      {activeTab === 'allocations' && (
        loading ? (
          <p aria-live="polite" className="text-gray-500">{t('common.loading')}</p>
        ) : allocations.length === 0 ? (
          <p className="text-gray-500">{t('projects.no_allocations')}</p>
        ) : (
          <div className="bg-white shadow rounded-lg overflow-hidden">
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  {['date', 'project', 'code', 'duration', 'notes', 'actions'].map((col) => (
                    <th
                      key={col}
                      className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                    >
                      {t(`projects.columns.${col}`)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {allocations.map((alloc) => (
                  <tr key={alloc.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{alloc.date}</td>
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">{alloc.projectName}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">{alloc.projectCode}</td>
                    <td className="px-4 py-3 text-sm text-gray-900">{formatMinutes(alloc.minutes)}</td>
                    <td className="px-4 py-3 text-sm text-gray-500">{alloc.notes ?? '—'}</td>
                    <td className="px-4 py-3">
                      <button
                        onClick={() => handleDeleteAllocation(alloc.id)}
                        disabled={actionLoading === alloc.id}
                        className="inline-flex items-center gap-1 text-xs font-medium text-red-600 hover:text-red-800 disabled:opacity-50"
                        title={t('common.delete')}
                      >
                        <Trash2 size={14} />
                        {t('common.delete')}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}

      {/* New Allocation Form */}
      {activeTab === 'new_allocation' && (
        <form onSubmit={handleCreateAllocation} className="bg-white shadow rounded-lg p-6 max-w-lg space-y-4">
          <div>
            <label htmlFor="allocProject" className="block text-sm font-medium text-gray-700 mb-1">
              {t('projects.project')} *
            </label>
            <select
              id="allocProject"
              required
              value={allocationForm.projectId}
              onChange={(e) => setAllocationForm((f) => ({ ...f, projectId: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            >
              <option value="">{t('projects.select_project')}</option>
              {projects.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.code})
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="allocDate" className="block text-sm font-medium text-gray-700 mb-1">
              {t('common.date')} *
            </label>
            <input
              id="allocDate"
              type="date"
              required
              value={allocationForm.date}
              onChange={(e) => setAllocationForm((f) => ({ ...f, date: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="allocMinutes" className="block text-sm font-medium text-gray-700 mb-1">
              {t('projects.minutes')} *
            </label>
            <input
              id="allocMinutes"
              type="number"
              required
              min="1"
              value={allocationForm.minutes}
              onChange={(e) => setAllocationForm((f) => ({ ...f, minutes: e.target.value }))}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="allocNotes" className="block text-sm font-medium text-gray-700 mb-1">
              {t('projects.notes')}
            </label>
            <textarea
              id="allocNotes"
              value={allocationForm.notes}
              onChange={(e) => setAllocationForm((f) => ({ ...f, notes: e.target.value }))}
              rows={3}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
          </div>
          <button
            type="submit"
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded hover:bg-primary-700"
          >
            <Plus size={16} />
            {t('projects.add_allocation')}
          </button>
        </form>
      )}

      {/* Admin: Project Management */}
      {activeTab === 'projects' && isAdmin && (
        <div className="space-y-6">
          <form onSubmit={handleCreateProject} className="bg-white shadow rounded-lg p-6 max-w-lg space-y-4">
            <h2 className="text-lg font-semibold text-gray-900">{t('projects.create_project')}</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label htmlFor="projName" className="block text-sm font-medium text-gray-700 mb-1">
                  {t('projects.project_name')} *
                </label>
                <input
                  id="projName"
                  type="text"
                  required
                  value={projectForm.name}
                  onChange={(e) => setProjectForm((f) => ({ ...f, name: e.target.value }))}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                />
              </div>
              <div>
                <label htmlFor="projCode" className="block text-sm font-medium text-gray-700 mb-1">
                  {t('projects.project_code')} *
                </label>
                <input
                  id="projCode"
                  type="text"
                  required
                  value={projectForm.code}
                  onChange={(e) => setProjectForm((f) => ({ ...f, code: e.target.value }))}
                  className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
                />
              </div>
            </div>
            <div>
              <label htmlFor="projDesc" className="block text-sm font-medium text-gray-700 mb-1">
                {t('projects.description')}
              </label>
              <textarea
                id="projDesc"
                value={projectForm.description}
                onChange={(e) => setProjectForm((f) => ({ ...f, description: e.target.value }))}
                rows={2}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
            <div>
              <label htmlFor="projCostCenter" className="block text-sm font-medium text-gray-700 mb-1">
                {t('projects.cost_center')}
              </label>
              <input
                id="projCostCenter"
                type="text"
                value={projectForm.costCenter}
                onChange={(e) => setProjectForm((f) => ({ ...f, costCenter: e.target.value }))}
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
            </div>
            <button
              type="submit"
              className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded hover:bg-primary-700"
            >
              {t('projects.create_project')}
            </button>
          </form>

          {/* Projects list */}
          {projects.length > 0 && (
            <div className="bg-white shadow rounded-lg overflow-hidden">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    {['name', 'code', 'cost_center', 'status'].map((col) => (
                      <th
                        key={col}
                        className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
                      >
                        {t(`projects.project_columns.${col}`)}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {projects.map((project) => (
                    <tr key={project.id} className="hover:bg-gray-50">
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">{project.name}</td>
                      <td className="px-4 py-3 text-sm text-gray-500">{project.code}</td>
                      <td className="px-4 py-3 text-sm text-gray-500">{project.costCenter ?? '—'}</td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                          project.isActive ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-700'
                        }`}>
                          {project.isActive ? t('users.active') : t('users.inactive')}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
