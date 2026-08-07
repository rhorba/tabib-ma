import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'
import { useAuth } from '@/features/auth/AuthContext'
import { useConsultationCall } from '../hooks/useConsultationCall'
import { VideoConsultationRoom } from '../components/VideoConsultationRoom'
import { CompleteConsultationForm } from '../components/CompleteConsultationForm'

// Story 6.1's AC needs joinable to flip from false to true as the clock reaches the join
// window without a manual page reload.
const STATUS_REFETCH_INTERVAL_MS = 30_000

function useConsultationStatus(appointmentId: string | undefined) {
  return useQuery({
    queryKey: ['consultation', 'by-appointment', appointmentId],
    queryFn: async () => {
      const { data, error, response } = await apiClient.GET('/api/v1/consultations/by-appointment/{appointmentId}', {
        params: { path: { appointmentId: appointmentId! } },
      })
      if (response.status === 404 || response.status === 403) {
        return null
      }
      if (error) {
        throw error
      }
      return data ?? null
    },
    enabled: appointmentId !== undefined,
    refetchInterval: STATUS_REFETCH_INTERVAL_MS,
  })
}

export function ConsultationPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { appointmentId } = useParams<{ appointmentId: string }>()
  const statusQuery = useConsultationStatus(appointmentId)
  const consultationId = statusQuery.data?.id
  const call = useConsultationCall(consultationId ?? '')
  const [completed, setCompleted] = useState<'no' | 'prescribed' | 'skipped'>('no')

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 py-12">
      <h1 className="text-xl font-semibold text-foreground" role="heading" aria-level={1}>
        {t('consultation.room.title')}
      </h1>

      {statusQuery.isLoading && <p className="text-sm text-muted-foreground">{t('consultation.room.loadingStatus')}</p>}

      {statusQuery.isSuccess && !statusQuery.data && (
        <Alert variant="destructive">
          <AlertDescription>{t('consultation.room.notFound')}</AlertDescription>
        </Alert>
      )}

      {completed !== 'no' && (
        <Alert role="status">
          <AlertDescription>
            {completed === 'prescribed' ? t('consultation.complete.success') : t('consultation.complete.successNoPrescription')}
          </AlertDescription>
        </Alert>
      )}

      {statusQuery.data && completed === 'no' && (
        <>
          {!statusQuery.data.joinable && call.phase === 'idle' && (
            <Alert role="status">
              <AlertDescription>
                <p>{t('consultation.room.joinTooEarly')}</p>
                <p>{t('consultation.room.joinOpensNote')}</p>
              </AlertDescription>
            </Alert>
          )}

          {statusQuery.data.joinable && call.phase === 'idle' && (
            <Button type="button" onClick={() => void call.join()}>
              {t('consultation.room.join')}
            </Button>
          )}

          {call.phase !== 'idle' && <VideoConsultationRoom call={call} />}

          {user?.role === 'DOCTOR' && call.phase === 'connected' && statusQuery.data.status !== 'COMPLETED' && (
            <CompleteConsultationForm
              consultationId={statusQuery.data.id!}
              onCompleted={(prescribed) => setCompleted(prescribed ? 'prescribed' : 'skipped')}
            />
          )}
        </>
      )}

      <Link to="/appointments" className="text-sm text-primary hover:underline">
        {t('consultation.room.backToAppointments')}
      </Link>
    </div>
  )
}
