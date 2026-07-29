import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'
import { getApiErrorCode } from '@/shared/api/errors'
import type { components } from '@/shared/api/schema'

type DoctorProfile = components['schemas']['DoctorProfileResponse']

const DOCUMENT_TYPE_KEYS: Record<string, string> = {
  MEDICAL_LICENSE: 'doctorOnboarding.documents.typeMedicalLicense',
  ID_CARD: 'doctorOnboarding.documents.typeIdCard',
  DIPLOMA: 'doctorOnboarding.documents.typeDiploma',
}

export function VerificationQueueItem({ profile }: { profile: DoctorProfile }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const [documentsOpen, setDocumentsOpen] = useState(false)

  const documentsQuery = useQuery({
    queryKey: ['verification-queue', 'documents', profile.id],
    queryFn: async () => {
      const { data, error } = await apiClient.GET(
        '/api/v1/admin/platform/verification-queue/{doctorProfileId}/documents',
        { params: { path: { doctorProfileId: profile.id! } } }
      )
      if (error) {
        throw error
      }
      return data ?? []
    },
    enabled: documentsOpen,
  })

  const decisionMutation = useMutation({
    mutationFn: async (decision: 'approve' | 'reject') => {
      const path =
        decision === 'approve'
          ? '/api/v1/admin/platform/verification-queue/{doctorProfileId}/approve'
          : '/api/v1/admin/platform/verification-queue/{doctorProfileId}/reject'
      const { data, error } = await apiClient.POST(path, {
        params: { path: { doctorProfileId: profile.id! } },
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['verification-queue'] })
    },
  })

  const errorCode = getApiErrorCode(decisionMutation.error)
  const errorMessage =
    decisionMutation.isError &&
    (errorCode === 'CONFLICT'
      ? t('verificationQueue.errors.alreadyReviewed')
      : t('verificationQueue.errors.generic'))

  return (
    <li className="grid gap-3 rounded-md border border-border p-4">
      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
        <span className="text-muted-foreground">{t('verificationQueue.specialtyLabel')}</span>
        <span>{profile.specialty}</span>
        <span className="text-muted-foreground">{t('verificationQueue.cityLabel')}</span>
        <span>{profile.city}</span>
        <span className="text-muted-foreground">{t('verificationQueue.feeLabel')}</span>
        <span>{profile.consultationFeeMad}</span>
      </div>

      {errorMessage && <p className="text-sm text-destructive">{errorMessage}</p>}

      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" size="sm" onClick={() => setDocumentsOpen((v) => !v)}>
          {documentsOpen ? t('verificationQueue.hideDocuments') : t('verificationQueue.viewDocuments')}
        </Button>
        <Button
          type="button"
          size="sm"
          disabled={decisionMutation.isPending}
          onClick={() => decisionMutation.mutate('approve')}
        >
          {t('verificationQueue.approve')}
        </Button>
        <Button
          type="button"
          variant="destructive"
          size="sm"
          disabled={decisionMutation.isPending}
          onClick={() => decisionMutation.mutate('reject')}
        >
          {t('verificationQueue.reject')}
        </Button>
      </div>

      {documentsOpen && (
        <ul className="grid gap-1 text-sm">
          {documentsQuery.data && documentsQuery.data.length > 0 ? (
            documentsQuery.data.map((document) => (
              <li key={document.id} className="flex justify-between rounded-md bg-muted px-2 py-1">
                <span>
                  {document.documentType && DOCUMENT_TYPE_KEYS[document.documentType]
                    ? t(DOCUMENT_TYPE_KEYS[document.documentType])
                    : document.documentType}
                </span>
                <span className="text-muted-foreground">
                  {document.createdAt && new Date(document.createdAt).toLocaleDateString()}
                </span>
              </li>
            ))
          ) : (
            <li className="text-muted-foreground">{t('doctorOnboarding.documents.empty')}</li>
          )}
        </ul>
      )}
    </li>
  )
}
