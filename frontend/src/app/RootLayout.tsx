import { Suspense, useState } from 'react'
import { MenuIcon, XIcon } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Link, Outlet, useLocation } from 'react-router'
import { LanguageSwitcher } from '@/shared/components/LanguageSwitcher'
import { Button } from '@/shared/components/ui/button'
import { useAuth } from '@/features/auth/AuthContext'

function AuthNav({ vertical = false }: { vertical?: boolean }) {
  const { t } = useTranslation()
  const { status, user, logout } = useAuth()

  if (status === 'loading') {
    return null
  }

  if (status === 'authenticated' && user) {
    return (
      <div className={vertical ? 'flex flex-col items-start gap-3' : 'flex items-center gap-3'}>
        <Link to="/search" className="text-sm font-medium text-foreground hover:text-primary">
          {t('nav.search')}
        </Link>
        {user.role === 'DOCTOR' && (
          <>
            <Link to="/doctor/onboarding" className="text-sm font-medium text-foreground hover:text-primary">
              {t('nav.doctorOnboarding')}
            </Link>
            <Link to="/doctor/availability" className="text-sm font-medium text-foreground hover:text-primary">
              {t('nav.availability')}
            </Link>
            <Link to="/appointments" className="text-sm font-medium text-foreground hover:text-primary">
              {t('nav.myAppointments')}
            </Link>
          </>
        )}
        {user.role === 'PATIENT' && (
          <>
            <Link to="/appointments" className="text-sm font-medium text-foreground hover:text-primary">
              {t('nav.myAppointments')}
            </Link>
            <Link to="/prescriptions" className="text-sm font-medium text-foreground hover:text-primary">
              {t('nav.myPrescriptions')}
            </Link>
          </>
        )}
        {user.role === 'PLATFORM_ADMIN' && (
          <>
            <Link
              to="/platform-admin/verification-queue"
              className="text-sm font-medium text-foreground hover:text-primary"
            >
              {t('nav.verificationQueue')}
            </Link>
            <Link
              to="/platform-admin/disputes"
              className="text-sm font-medium text-foreground hover:text-primary"
            >
              {t('nav.disputeQueue')}
            </Link>
            <Link
              to="/platform-admin/health"
              className="text-sm font-medium text-foreground hover:text-primary"
            >
              {t('nav.platformHealth')}
            </Link>
          </>
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
    <div className={vertical ? 'flex flex-col items-start gap-3' : 'flex items-center gap-3'}>
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
  const location = useLocation()
  // Remember which path the menu was opened on: navigating closes it without an effect.
  const [menuOpenOn, setMenuOpenOn] = useState<string | null>(null)
  const menuOpen = menuOpenOn === location.pathname

  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b border-border">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4">
          <Link to="/" className="text-lg font-semibold text-primary">
            {t('app.name')}
          </Link>
          <div className="hidden items-center gap-4 md:flex">
            <AuthNav />
            <LanguageSwitcher />
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="md:hidden"
            aria-label={t('nav.menu')}
            aria-expanded={menuOpen}
            aria-controls="mobile-nav"
            onClick={() => setMenuOpenOn(menuOpen ? null : location.pathname)}
          >
            {menuOpen ? <XIcon /> : <MenuIcon />}
          </Button>
        </div>
        {menuOpen && (
          <nav id="mobile-nav" className="flex flex-col gap-4 border-t border-border px-4 py-4 md:hidden">
            <AuthNav vertical />
            <LanguageSwitcher />
          </nav>
        )}
      </header>
      <main className="flex flex-1 flex-col">
        <Suspense fallback={<p className="p-4 text-sm text-muted-foreground">{t('app.loadingPage')}</p>}>
          <Outlet />
        </Suspense>
      </main>
    </div>
  )
}
