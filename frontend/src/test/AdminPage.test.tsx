import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { BrowserRouter } from 'react-router-dom'
import { AuthProvider } from '../context/AuthContext'
import AdminPage from '../pages/AdminPage'

vi.mock('../services/adminService', () => ({
  default: {
    getUsers: vi.fn(),
    getUserById: vi.fn(),
    createUser: vi.fn(),
    updateUser: vi.fn(),
    deleteUser: vi.fn(),
    assignRoles: vi.fn(),
    updateRfid: vi.fn(),
    resetPassword: vi.fn(),
    getAuditLog: vi.fn(),
    getAuditLogByUser: vi.fn(),
    getSettings: vi.fn(),
    updateSetting: vi.fn(),
  },
}))

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
    i18n: { language: 'de', changeLanguage: vi.fn() },
  }),
}))

const mockUsers = {
  content: [
    {
      id: '1',
      email: 'alice@test.com',
      firstName: 'Alice',
      lastName: 'Smith',
      isActive: true,
      roles: ['EMPLOYEE'],
      permissions: [],
    },
    {
      id: '2',
      email: 'bob@test.com',
      firstName: 'Bob',
      lastName: 'Jones',
      isActive: false,
      roles: ['ADMIN', 'EMPLOYEE'],
      permissions: [],
    },
  ],
  totalElements: 2,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 20,
}

const mockAuditLog = {
  content: [
    {
      id: 'a1',
      userId: '1',
      userEmail: 'alice@test.com',
      userFullName: 'Alice Smith',
      action: 'LOGIN',
      entityType: 'User',
      entityId: '1',
      ipAddress: '127.0.0.1',
      userAgent: null,
      createdAt: '2026-03-01T09:00:00Z',
    },
    {
      id: 'a2',
      userId: null,
      userEmail: null,
      userFullName: null,
      action: 'SYSTEM_INIT',
      entityType: null,
      entityId: null,
      ipAddress: null,
      userAgent: null,
      createdAt: '2026-03-01T08:00:00Z',
    },
  ],
  totalElements: 2,
  totalPages: 1,
  pageNumber: 0,
  pageSize: 50,
}

const mockSettings = [
  { key: 'company.name', value: 'Test GmbH', description: 'Company name', updatedAt: '2026-03-01T00:00:00Z' },
  { key: 'working.hours.default', value: '8', description: 'Default daily hours', updatedAt: '2026-03-01T00:00:00Z' },
]

function renderAdminPage() {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <AdminPage />
      </AuthProvider>
    </BrowserRouter>,
  )
}

describe('AdminPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    const { default: adminService } = await import('../services/adminService')
    vi.mocked(adminService.getUsers).mockResolvedValue(mockUsers)
    vi.mocked(adminService.getAuditLog).mockResolvedValue(mockAuditLog)
    vi.mocked(adminService.getSettings).mockResolvedValue(mockSettings)
  })

  // ─── Tab Navigation ───────────────────────────────────────────────────────

  it('should render admin page title', () => {
    renderAdminPage()
    expect(screen.getByText('admin.title')).toBeInTheDocument()
  })

  it('should render three tab buttons', () => {
    renderAdminPage()
    expect(screen.getByText('admin.users_tab')).toBeInTheDocument()
    expect(screen.getByText('admin.audit_log')).toBeInTheDocument()
    expect(screen.getByText('admin.settings_tab')).toBeInTheDocument()
  })

  it('should default to users tab', () => {
    renderAdminPage()
    // The Users tab should be active (rendered) by default
    expect(screen.getByRole('button', { name: 'admin.users.create' })).toBeInTheDocument()
  })

  it('should switch to audit log tab when clicked', async () => {
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.audit_log'))
    await waitFor(() => {
      expect(screen.getByText('admin.audit.action')).toBeInTheDocument()
    })
  })

  it('should switch to settings tab when clicked', async () => {
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.settings_tab'))
    await waitFor(() => {
      expect(screen.getByText('admin.settings.key')).toBeInTheDocument()
    })
  })

  // ─── Users Tab ────────────────────────────────────────────────────────────

  it('should load and display users', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
      expect(screen.getByText('Bob Jones')).toBeInTheDocument()
    })
  })

  it('should show active/inactive status badges', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('users.active')).toBeInTheDocument()
      expect(screen.getByText('users.inactive')).toBeInTheDocument()
    })
  })

  it('should show role badges', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getAllByText('EMPLOYEE').length).toBeGreaterThan(0)
      expect(screen.getByText('ADMIN')).toBeInTheDocument()
    })
  })

  it('should filter users by search input', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    const searchInput = screen.getByRole('searchbox', { name: 'common.search' })
    fireEvent.change(searchInput, { target: { value: 'alice' } })
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
      expect(screen.queryByText('Bob Jones')).not.toBeInTheDocument()
    })
  })

  it('should show no-users message when filter matches nothing', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    const searchInput = screen.getByRole('searchbox', { name: 'common.search' })
    fireEvent.change(searchInput, { target: { value: 'xyznonexistent' } })
    await waitFor(() => {
      expect(screen.getByText('admin.users.no_users')).toBeInTheDocument()
    })
  })

  it('should open create user modal when Create User button is clicked', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'admin.users.create' }))
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText('users.first_name')).toBeInTheDocument()
    })
  })

  it('should close modal when cancel is clicked', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'admin.users.create' }))
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'common.cancel' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })

  it('should open edit user modal when edit button is clicked', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    const editButtons = screen.getAllByRole('button', { name: 'common.edit' })
    fireEvent.click(editButtons[0])
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText('admin.users.edit')).toBeInTheDocument()
    })
  })

  it('should open RFID modal when RFID button is clicked', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    const rfidButtons = screen.getAllByRole('button', { name: 'users.rfid_tag' })
    fireEvent.click(rfidButtons[0])
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText('admin.users.update_rfid')).toBeInTheDocument()
    })
  })

  it('should open reset password modal when reset password button is clicked', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    const pwButtons = screen.getAllByRole('button', { name: 'users.reset_password' })
    fireEvent.click(pwButtons[0])
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
      // Dialog should have the heading
      expect(screen.getAllByText('users.reset_password').length).toBeGreaterThan(0)
    })
  })

  it('should show delete confirmation when delete button is clicked', async () => {
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    const deleteButtons = screen.getAllByRole('button', { name: 'common.delete' })
    fireEvent.click(deleteButtons[0])
    await waitFor(() => {
      expect(screen.getByText('users.delete_confirm')).toBeInTheDocument()
    })
  })

  it('should show error when user creation fails', async () => {
    const { default: adminService } = await import('../services/adminService')
    vi.mocked(adminService.createUser).mockRejectedValue(new Error('Network error'))
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'admin.users.create' }))
    await waitFor(() => {
      expect(screen.getByRole('dialog')).toBeInTheDocument()
    })
    // Use the form input IDs added in the modal (htmlFor/id pairs)
    fireEvent.change(screen.getByLabelText('users.first_name'), { target: { value: 'New' } })
    fireEvent.change(screen.getByLabelText('users.last_name'), { target: { value: 'User' } })
    fireEvent.change(screen.getByLabelText('common.email'), { target: { value: 'new@test.com' } })
    fireEvent.change(screen.getByLabelText('users.new_password'), { target: { value: 'password123' } })
    fireEvent.click(screen.getByRole('button', { name: 'common.save' }))
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })
  })

  it('should show error banner when users fail to load', async () => {
    const { default: adminService } = await import('../services/adminService')
    vi.mocked(adminService.getUsers).mockRejectedValue(new Error('Server error'))
    renderAdminPage()
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
      expect(screen.getByText('admin.errors.load_failed')).toBeInTheDocument()
    })
  })

  // ─── Audit Log Tab ────────────────────────────────────────────────────────

  it('should display audit log entries', async () => {
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.audit_log'))
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument()
      expect(screen.getByText('alice@test.com')).toBeInTheDocument()
      expect(screen.getByText('LOGIN')).toBeInTheDocument()
      expect(screen.getByText('SYSTEM_INIT')).toBeInTheDocument()
    })
  })

  it('should show "unknown" for audit entries without a user', async () => {
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.audit_log'))
    await waitFor(() => {
      expect(screen.getByText('SYSTEM_INIT')).toBeInTheDocument()
      expect(screen.getByText('common.unknown')).toBeInTheDocument()
    })
  })

  it('should show IP address in audit log', async () => {
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.audit_log'))
    await waitFor(() => {
      expect(screen.getByText('127.0.0.1')).toBeInTheDocument()
    })
  })

  it('should show error banner when audit log fails to load', async () => {
    const { default: adminService } = await import('../services/adminService')
    vi.mocked(adminService.getAuditLog).mockRejectedValue(new Error('Server error'))
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.audit_log'))
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
      expect(screen.getByText('admin.errors.load_failed')).toBeInTheDocument()
    })
  })

  // ─── Settings Tab ─────────────────────────────────────────────────────────

  it('should display system settings', async () => {
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.settings_tab'))
    await waitFor(() => {
      expect(screen.getByText('company.name')).toBeInTheDocument()
      expect(screen.getByText('Test GmbH')).toBeInTheDocument()
      expect(screen.getByText('working.hours.default')).toBeInTheDocument()
      expect(screen.getByText('8')).toBeInTheDocument()
    })
  })

  it('should allow editing a setting inline', async () => {
    const { default: adminService } = await import('../services/adminService')
    vi.mocked(adminService.updateSetting).mockResolvedValue({
      key: 'company.name',
      value: 'New GmbH',
      description: 'Company name',
      updatedAt: '2026-03-01T01:00:00Z',
    })
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.settings_tab'))
    await waitFor(() => {
      expect(screen.getByText('company.name')).toBeInTheDocument()
    })
    // Click the edit button for the first setting (they have no text content - just an icon)
    const editButtons = screen.getAllByRole('button')
    const firstEditButton = editButtons.find((b) => b.querySelector('svg') && b.textContent === '')
    if (firstEditButton) fireEvent.click(firstEditButton)
    await waitFor(() => {
      expect(screen.getByDisplayValue('Test GmbH')).toBeInTheDocument()
    })
  })

  it('should show error banner when settings fail to load', async () => {
    const { default: adminService } = await import('../services/adminService')
    vi.mocked(adminService.getSettings).mockRejectedValue(new Error('Server error'))
    renderAdminPage()
    fireEvent.click(screen.getByText('admin.settings_tab'))
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })
  })
})
