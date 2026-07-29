import { useTranslation } from 'react-i18next'
import { Link, Outlet } from 'react-router'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { Button } from '@/shared/components/ui/button'
import { useAuth } from '@/features/auth/AuthContext'

function AuthNav() {
  const { t } = useTranslation()
  const { status, user, logout } = useAuth()

  if (status === 'loading') {
    return null
  }

  if (status === 'authenticated' && user) {
    return (
      <div className="flex items-center gap-3">
        {user.role === 'DOCTOR' && (
          <Link to="/doctor/onboarding" className="text-sm font-medium text-foreground hover:text-primary">
            {t('nav.doctorOnboarding')}
          </Link>
        )}
        {user.role === 'PLATFORM_ADMIN' && (
          <Link
            to="/platform-admin/verification-queue"
            className="text-sm font-medium text-foreground hover:text-primary"
          >
            {t('nav.verificationQueue')}
          </Link>
        )}
        {user.role === 'CLINIC_ADMIN' && (
          <Link to="/clinic-admin" className="text-sm font-medium text-foreground hover:text-primary">
            {t('nav.clinicAdmin')}
          </Link>
        )}
        <span className="text-sm text-muted-foreground">
          {t('nav.greeting', { name: user.firstName })}
        </span>
        <Button variant="outline" size="sm" onClick={logout}>
          {t('nav.logout')}
        </Button>
      </div>
    )
  }

  return (
    <div className="flex items-center gap-3">
      <Link to="/login" className="text-sm font-medium text-foreground hover:text-primary">
        {t('nav.login')}
      </Link>
      <Button asChild size="sm">
        <Link to="/register">{t('nav.register')}</Link>
      </Button>
    </div>
  )
}

export function RootLayout() {
  const { t } = useTranslation()

  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b border-border">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
          <Link to="/" className="text-lg font-semibold text-primary">
            {t('app.name')}
          </Link>
          <div className="flex items-center gap-4">
            <AuthNav />
            <LanguageSwitcher />
          </div>
        </div>
      </header>
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
    </div>
  )
}
