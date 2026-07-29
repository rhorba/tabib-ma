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
import { DoctorProfileForm } from '../components/DoctorProfileForm'
import { DocumentUploadForm } from '../components/DocumentUploadForm'

const STATUS_STYLES: Record<string, string> = {
  PENDING: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
}

const DOCUMENT_TYPE_KEYS: Record<string, string> = {
  MEDICAL_LICENSE: 'doctorOnboarding.documents.typeMedicalLicense',
  ID_CARD: 'doctorOnboarding.documents.typeIdCard',
  DIPLOMA: 'doctorOnboarding.documents.typeDiploma',
}

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

function useMyDocuments(doctorProfileId: string | undefined) {
  return useQuery({
    queryKey: ['doctor-profile', 'documents', doctorProfileId],
    queryFn: async () => {
      const { data, error } = await apiClient.GET(
        '/api/v1/clinic/doctor-profiles/{doctorProfileId}/documents',
        { params: { path: { doctorProfileId: doctorProfileId! } } }
      )
      if (error) {
        throw error
      }
      return data ?? []
    },
    enabled: doctorProfileId !== undefined,
  })
}

export function DoctorOnboardingPage() {
  const { t } = useTranslation()
  const profileQuery = useMyDoctorProfile()
  const documentsQuery = useMyDocuments(profileQuery.data?.id)

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('doctorOnboarding.title')}
          </CardTitle>
          {profileQuery.data && (
            <CardDescription>
              <span
                className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[profileQuery.data.verificationStatus ?? 'PENDING']}`}
              >
                {t(`doctorOnboarding.status.${(profileQuery.data.verificationStatus ?? 'pending').toLowerCase()}`)}
              </span>
            </CardDescription>
          )}
        </CardHeader>
        <CardContent>
          {profileQuery.isLoading && null}
          {!profileQuery.isLoading && !profileQuery.data && <DoctorProfileForm />}
          {!profileQuery.isLoading && profileQuery.data && (
            <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
              <dt className="text-muted-foreground">{t('doctorOnboarding.form.specialtyLabel')}</dt>
              <dd>{profileQuery.data.specialty}</dd>
              <dt className="text-muted-foreground">{t('doctorOnboarding.form.cityLabel')}</dt>
              <dd>{profileQuery.data.city}</dd>
            </dl>
          )}
        </CardContent>
      </Card>

      {profileQuery.data && (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg" role="heading" aria-level={2}>
              {t('doctorOnboarding.documents.title')}
            </CardTitle>
          </CardHeader>
          <CardContent className="grid gap-6">
            {documentsQuery.data && documentsQuery.data.length > 0 ? (
              <ul className="grid gap-2 text-sm">
                {documentsQuery.data.map((document) => (
                  <li key={document.id} className="flex justify-between rounded-md border border-border px-3 py-2">
                    <span>
                      {document.documentType && DOCUMENT_TYPE_KEYS[document.documentType]
                        ? t(DOCUMENT_TYPE_KEYS[document.documentType])
                        : document.documentType}
                    </span>
                    <span className="text-muted-foreground">
                      {document.createdAt && new Date(document.createdAt).toLocaleDateString()}
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-muted-foreground">{t('doctorOnboarding.documents.empty')}</p>
            )}
            <DocumentUploadForm doctorProfileId={profileQuery.data.id!} />
          </CardContent>
        </Card>
      )}
    </div>
  )
}
