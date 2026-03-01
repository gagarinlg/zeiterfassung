import { Outlet, NavLink, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'
import { Clock, Calendar, LayoutDashboard, Settings, LogOut, User, CheckCircle, UserCog } from 'lucide-react'

export default function Layout() {
  const { t } = useTranslation()
  const { user, logout, hasPermission } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="flex h-screen bg-gray-50">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:p-4 focus:bg-primary-700 focus:text-white"
      >
        {t('nav.skip_to_content')}
      </a>
      <aside className="w-64 bg-white shadow-md flex flex-col" aria-label={t('nav.sidebar')}>
        <div className="p-6 border-b">
          <h1 className="text-xl font-bold text-primary-700">{t('app.name')}</h1>
          <p className="text-sm text-gray-500">{t('app.tagline')}</p>
        </div>
        <nav className="flex-1 p-4 space-y-1">
          <NavLink
            to="/dashboard"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
              }`
            }
          >
            <LayoutDashboard size={18} />
            {t('nav.dashboard')}
          </NavLink>
          <NavLink
            to="/time-tracking"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
              }`
            }
          >
            <Clock size={18} />
            {t('nav.time_tracking')}
          </NavLink>
          <NavLink
            to="/vacation"
            end
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
              }`
            }
          >
            <Calendar size={18} />
            {t('nav.vacation')}
          </NavLink>
          {hasPermission('vacation.approve') && (
            <NavLink
              to="/vacation/approvals"
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              <CheckCircle size={18} />
              {t('nav.vacation_approvals')}
            </NavLink>
          )}
          {hasPermission('admin.users.manage') && (
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
                }`
              }
            >
              <Settings size={18} />
              {t('nav.admin')}
            </NavLink>
          )}
        </nav>
        <div className="p-4 border-t">
          <div className="flex items-center gap-3 px-3 py-2">
            <User size={18} className="text-gray-500" />
            <span className="text-sm text-gray-700 truncate">
              {user?.firstName} {user?.lastName}
            </span>
          </div>
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors ${
                isActive ? 'bg-primary-50 text-primary-700' : 'text-gray-600 hover:bg-gray-100'
              }`
            }
          >
            <UserCog size={18} />
            {t('nav.settings')}
          </NavLink>
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-600 hover:bg-gray-100 transition-colors"
          >
            <LogOut size={18} />
            {t('nav.logout')}
          </button>
        </div>
      </aside>
      <main id="main-content" className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
