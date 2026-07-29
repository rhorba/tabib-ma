import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { CreateClinicForm } from '../components/CreateClinicForm'
import { InviteDoctorForm } from '../components/InviteDoctorForm'

const STATUS_STYLES: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  ACCEPTED: 'bg-green-100 text-green-800',
  DECLINED: 'bg-red-100 text-red-800',
}

function useMyClinic() {
  return useQuery({
    queryKey: ['clinic', 'me'],
    queryFn: async () => {
      const { data, error, response } = await apiClient.GET('/api/v1/clinic/clinics/me')
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

function useClinicInvitations(clinicId: string | undefined) {
  return useQuery({
    queryKey: ['clinic', 'invitations', clinicId],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/clinics/{clinicId}/invitations', {
        params: { path: { clinicId: clinicId! } },
      })
      if (error) {
        throw error
      }
      return data ?? []
    },
    enabled: clinicId !== undefined,
  })
}

export function ClinicAdminPage() {
  const { t } = useTranslation()
  const clinicQuery = useMyClinic()
  const invitationsQuery = useClinicInvitations(clinicQuery.data?.id)

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('clinicAdmin.title')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {!clinicQuery.isLoading && !clinicQuery.data && <CreateClinicForm />}
          {!clinicQuery.isLoading && clinicQuery.data && (
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <dt className="text-muted-foreground">{t('clinicAdmin.form.nameLabel')}</dt>
              <dd>{clinicQuery.data.name}</dd>
              <dt className="text-muted-foreground">{t('clinicAdmin.form.cityLabel')}</dt>
              <dd>{clinicQuery.data.city}</dd>
            </dl>
          )}
        </CardContent>
      </Card>

      {clinicQuery.data && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg" role="heading" aria-level={2}>
              {t('clinicAdmin.invitations.title')}
            </CardTitle>
            <CardDescription>{t('clinicAdmin.invitations.description')}</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-6">
            {invitationsQuery.data && invitationsQuery.data.length > 0 ? (
              <ul className="grid gap-2 text-sm">
                {invitationsQuery.data.map((invitation) => (
                  <li
                    key={invitation.id}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2"
                  >
                    <span>{invitation.invitedEmail}</span>
                    <span
                      className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[invitation.status ?? 'PENDING']}`}
                    >
                      {t(`clinicAdmin.invitations.status.${(invitation.status ?? 'pending').toLowerCase()}`)}
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted-foreground">{t('clinicAdmin.invitations.empty')}</p>
            )}
            <InviteDoctorForm clinicId={clinicQuery.data.id!} />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
