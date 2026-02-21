import { Navigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'

interface ProtectedRouteProps {
  children: React.ReactNode
  requiredPermission?: string
  requiredRole?: string
}

function ForbiddenPage() {
  const { t } = useTranslation()
  return (
    <div className="flex items-center justify-center min-h-screen bg-gray-50">
      <div className="text-center p-8">
        <h1 className="text-6xl font-bold text-red-500">403</h1>
        <h2 className="mt-4 text-2xl font-semibold text-gray-800">{t('errors.forbidden')}</h2>
        <p className="mt-2 text-gray-600">{t('errors.forbidden_description')}</p>
        <a
          href="/dashboard"
          className="mt-6 inline-block px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
        >
          {t('common.back_to_dashboard')}
        </a>
      </div>
    </div>
  )
}

export default function ProtectedRoute({
  children,
  requiredPermission,
  requiredRole,
}: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, hasPermission, hasRole } = useAuth()

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600" />
      </div>
    )
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  if (requiredPermission && !hasPermission(requiredPermission)) {
    return <ForbiddenPage />
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return <ForbiddenPage />
  }

  return <>{children}</>
}
