import { useTranslation } from 'react-i18next'

export function HomePage() {
  const { t } = useTranslation()

  return (
    <div className="mx-auto flex max-w-6xl flex-1 items-center justify-center px-4 text-center">
      <div>
        <h1 className="text-3xl font-semibold text-foreground">{t('home.title')}</h1>
        <p className="mt-2 text-muted-foreground">{t('home.subtitle')}</p>
      </div>
    </div>
  )
}
