import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { useAuth } from '@/features/auth/AuthContext'

function useDoctorPublicProfile(doctorProfileId: string | undefined) {
  return useQuery({
    queryKey: ['doctor-public-profile', doctorProfileId],
    queryFn: async () => {
      const { data, error, response } = await apiClient.GET(
        '/api/v1/clinic/doctor-profiles/{doctorProfileId}/public',
        { params: { path: { doctorProfileId: doctorProfileId! } } },
      )
      if (response.status === 404) {
        return null
      }
      if (error) {
        throw error
      }
      return data ?? null
    },
    enabled: doctorProfileId !== undefined,
  })
}

export function DoctorPublicProfilePage() {
  const { t } = useTranslation()
  const { doctorProfileId } = useParams<{ doctorProfileId: string }>()
  const profileQuery = useDoctorPublicProfile(doctorProfileId)
  const { status, user } = useAuth()

  if (profileQuery.isLoading) {
    return null
  }

  if (!profileQuery.data) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-4 px-4 py-12">
        <p className="text-sm text-muted-foreground">{t('doctorPublicProfile.notFound')}</p>
        <Link to="/search" className="text-sm font-medium text-primary hover:underline">
          {t('doctorPublicProfile.backToSearch')}
        </Link>
      </div>
    )
  }

  const profile = profileQuery.data

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {profile.firstName} {profile.lastName}
          </CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 text-sm">
          <dl className="grid grid-cols-2 gap-x-4 gap-y-2">
            <dt className="text-muted-foreground">{t('search.filters.specialtyLabel')}</dt>
            <dd>{profile.specialty}</dd>
            <dt className="text-muted-foreground">{t('doctorPublicProfile.cityLabel')}</dt>
            <dd>{profile.city}</dd>
            <dt className="text-muted-foreground">{t('doctorPublicProfile.feeLabel')}</dt>
            <dd>{profile.consultationFeeMad}</dd>
            <dt className="text-muted-foreground">{t('doctorPublicProfile.ratingLabel')}</dt>
            <dd>
              {profile.averageRating != null
                ? t('doctorPublicProfile.reviewCount', { count: profile.reviewCount ?? 0 })
                : t('doctorPublicProfile.noRating')}
            </dd>
          </dl>
          {profile.bio && <p className="text-muted-foreground">{profile.bio}</p>}
        </CardContent>
      </Card>
      {status === 'authenticated' && user?.role === 'PATIENT' && (
        <Button asChild>
          <Link to={`/doctors/${doctorProfileId}/book`}>{t('doctorPublicProfile.bookAppointment')}</Link>
        </Button>
      )}
      <Link to="/search" className="text-sm font-medium text-primary hover:underline">
        {t('doctorPublicProfile.backToSearch')}
      </Link>
    </div>
  )
}
