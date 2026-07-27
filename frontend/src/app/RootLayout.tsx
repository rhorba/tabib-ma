import { useTranslation } from 'react-i18next'
import { Link, Outlet } from 'react-router-dom'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'

export function RootLayout() {
  const { t } = useTranslation()

  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b border-border">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4">
          <Link to="/" className="text-lg font-semibold text-primary">
            {t('app.name')}
          </Link>
          <LanguageSwitcher />
        </div>
      </header>
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
    </div>
  )
}
