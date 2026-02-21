import { useTranslation } from 'react-i18next'
import { useAuth } from '../hooks/useAuth'

export default function DashboardPage() {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">{t('dashboard.title')}</h1>
      <p className="text-gray-600 mb-8">
        {user?.firstName} {user?.lastName}
      </p>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <p className="text-sm text-gray-500">{t('dashboard.today_hours')}</p>
          <p className="text-3xl font-bold text-primary-700 mt-1">0:00</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <p className="text-sm text-gray-500">{t('dashboard.week_hours')}</p>
          <p className="text-3xl font-bold text-primary-700 mt-1">0:00</p>
        </div>
        <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
          <p className="text-sm text-gray-500">{t('dashboard.vacation_remaining')}</p>
          <p className="text-3xl font-bold text-primary-700 mt-1">30</p>
        </div>
      </div>
    </div>
  )
}
