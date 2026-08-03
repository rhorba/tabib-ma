import { useMutation, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'

function useMyPrescriptions() {
  return useQuery({
    queryKey: ['prescriptions', 'mine'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/prescriptions/mine')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

// Story 7.1: a correction is always a new row with supersedesId pointing at the
// original (never an UPDATE) — so "this prescription has been corrected" means
// some other row in the same list points back at it.
function useSupersededIds(prescriptions: { supersedesId?: string }[] | undefined) {
  return new Set((prescriptions ?? []).map((p) => p.supersedesId).filter((id): id is string => Boolean(id)))
}

export function MyPrescriptionsPage() {
  const { t } = useTranslation()
  const prescriptionsQuery = useMyPrescriptions()
  const supersededIds = useSupersededIds(prescriptionsQuery.data)

  const downloadMutation = useMutation({
    mutationFn: async (prescriptionId: string) => {
      const { data, error } = await apiClient.GET('/api/v1/prescriptions/{prescriptionId}/pdf', {
        params: { path: { prescriptionId } },
        parseAs: 'blob',
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob)
      const anchor = document.createElement('a')
      anchor.href = url
      anchor.download = 'prescription.pdf'
      anchor.click()
      URL.revokeObjectURL(url)
    },
  })

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 py-12">
      <h1 className="text-xl font-semibold text-foreground" role="heading" aria-level={1}>
        {t('prescription.myPrescriptions.title')}
      </h1>

      {downloadMutation.isError && (
        <Alert variant="destructive">
          <AlertCircleIcon />
          <AlertDescription>{t('prescription.myPrescriptions.errors.generic')}</AlertDescription>
        </Alert>
      )}

      {prescriptionsQuery.data && prescriptionsQuery.data.length > 0 ? (
        <ul className="grid gap-3">
          {prescriptionsQuery.data.map((prescription) => (
            <li key={prescription.id} className="grid gap-3 rounded-md border border-border px-4 py-3 text-sm">
              <div className="flex items-center justify-between gap-3">
                <div className="grid gap-0.5">
                  <span className="font-medium text-foreground">{t('prescription.detail.title')}</span>
                  <span className="text-xs text-muted-foreground">
                    {t('prescription.myPrescriptions.issuedOn', {
                      date: prescription.createdAt ? new Date(prescription.createdAt).toLocaleDateString() : '',
                    })}
                  </span>
                </div>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  disabled={downloadMutation.isPending}
                  onClick={() => downloadMutation.mutate(prescription.id!)}
                >
                  {t('prescription.myPrescriptions.download')}
                </Button>
              </div>

              {supersededIds.has(prescription.id ?? '') && (
                <p className="text-xs text-muted-foreground">{t('prescription.myPrescriptions.correctedNotice')}</p>
              )}

              <div className="grid gap-1 border-t border-border pt-2">
                <h2 className="text-xs font-semibold uppercase text-muted-foreground">
                  {t('prescription.detail.medications')}
                </h2>
                <ul className="grid gap-1">
                  {prescription.items?.map((item, index) => (
                    <li key={index} className="text-sm text-foreground">
                      <span className="font-medium">{item.medicationName}</span>
                      {' — '}
                      <span>
                        {t('prescription.detail.dosage')}: {item.dosage}
                      </span>
                      {item.instructions && (
                        <span className="text-muted-foreground">
                          {' · '}
                          {t('prescription.detail.instructions')}: {item.instructions}
                        </span>
                      )}
                    </li>
                  ))}
                </ul>
              </div>
            </li>
          ))}
        </ul>
      ) : (
        prescriptionsQuery.isSuccess && (
          <p className="text-sm text-muted-foreground">{t('prescription.myPrescriptions.empty')}</p>
        )
      )}
    </div>
  )
}
