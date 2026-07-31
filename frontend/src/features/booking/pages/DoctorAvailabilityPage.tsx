import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { AvailabilityRuleForm } from '../components/AvailabilityRuleForm'
import { AvailabilityRulesList } from '../components/AvailabilityRulesList'
import { AvailabilityExceptionForm } from '../components/AvailabilityExceptionForm'
import { AvailabilityExceptionsList } from '../components/AvailabilityExceptionsList'
import { GenerateSlotsButton } from '../components/GenerateSlotsButton'

// Shares the ['doctor-profile', 'me'] query key with DoctorOnboardingPage's own hook of the same
// shape, so the two features benefit from one shared cache entry instead of double-fetching.
function useMyDoctorProfile() {
  return useQuery({
    queryKey: ['doctor-profile', 'me'],
    queryFn: async () => {
      const { data, error, response } = await apiClient.GET('/api/v1/clinic/doctor-profiles/me')
      if (response.status === 404) {
        return null
      }
      if (error) {
        throw error
      }
      return data ?? null
    },
  })
}

export function DoctorAvailabilityPage() {
  const { t } = useTranslation()
  const profileQuery = useMyDoctorProfile()

  if (profileQuery.isLoading) {
    return null
  }

  if (!profileQuery.data) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-4 px-4 py-12">
        <p className="text-sm text-muted-foreground">{t('booking.availability.needsProfile')}</p>
        <Link to="/doctor/onboarding" className="text-sm font-medium text-primary hover:underline">
          {t('booking.availability.goToOnboarding')}
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-12">
      <h1 className="text-xl font-semibold text-foreground" role="heading" aria-level={1}>
        {t('booking.availability.title')}
      </h1>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg" role="heading" aria-level={2}>
            {t('booking.availability.rules.title')}
          </CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4">
          <AvailabilityRulesList />
          <AvailabilityRuleForm />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg" role="heading" aria-level={2}>
            {t('booking.availability.exceptions.title')}
          </CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4">
          <AvailabilityExceptionsList />
          <AvailabilityExceptionForm />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-lg" role="heading" aria-level={2}>
            {t('booking.availability.generate.title')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <GenerateSlotsButton />
        </CardContent>
      </Card>
    </div>
  )
}
