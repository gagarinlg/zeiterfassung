import { useTranslation } from 'react-i18next'

export default function VacationPage() {
  const { t } = useTranslation()

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-6">{t('vacation.title')}</h1>
      <p className="text-gray-500">{t('common.loading')}</p>
    </div>
  )
}
