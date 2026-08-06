import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'
import { getApiErrorCode } from '@/shared/api/errors'
import type { components } from '@/shared/api/schema'

type Dispute = components['schemas']['DisputeResponse']

const DISPUTE_TYPE_KEYS: Record<string, string> = {
  NO_SHOW: 'disputeQueue.types.noShow',
  PAYMENT_ISSUE: 'disputeQueue.types.paymentIssue',
  COMPLAINT: 'disputeQueue.types.complaint',
}

const LOCATION_TYPE_KEYS: Record<string, string> = {
  IN_PERSON: 'booking.availability.locationTypes.inPerson',
  VIDEO: 'booking.availability.locationTypes.video',
}

export function DisputeQueueItem({ dispute }: { dispute: Dispute }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const resolveMutation = useMutation({
    mutationFn: async () => {
      const { error } = await apiClient.POST('/api/v1/admin/platform/disputes/{disputeId}/resolve', {
        params: { path: { disputeId: dispute.id! } },
      })
      if (error) {
        throw error
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dispute-queue'] })
    },
  })

  const refundMutation = useMutation({
    mutationFn: async () => {
      const { error } = await apiClient.POST('/api/v1/admin/platform/appointments/{appointmentId}/refund', {
        params: { path: { appointmentId: dispute.appointmentId! } },
      })
      if (error) {
        throw error
      }
    },
  })

  const forceCancelMutation = useMutation({
    mutationFn: async () => {
      const { error } = await apiClient.POST(
        '/api/v1/admin/platform/appointments/{appointmentId}/force-cancel',
        { params: { path: { appointmentId: dispute.appointmentId! } } }
      )
      if (error) {
        throw error
      }
    },
  })

  const resolveErrorCode = getApiErrorCode(resolveMutation.error)
  const refundErrorCode = getApiErrorCode(refundMutation.error)
  const forceCancelErrorCode = getApiErrorCode(forceCancelMutation.error)

  return (
    <li className="grid gap-3 rounded-md border border-border p-4">
      <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-sm">
        <span className="text-muted-foreground">{t('disputeQueue.typeLabel')}</span>
        <span>{dispute.type && t(DISPUTE_TYPE_KEYS[dispute.type] ?? dispute.type)}</span>
        <span className="text-muted-foreground">{t('disputeQueue.patientLabel')}</span>
        <span>{dispute.patientName}</span>
        <span className="text-muted-foreground">{t('disputeQueue.doctorLabel')}</span>
        <span>{dispute.doctorName}</span>
        <span className="text-muted-foreground">{t('disputeQueue.appointmentLabel')}</span>
        <span>
          {dispute.appointmentStartsAt && new Date(dispute.appointmentStartsAt).toLocaleString()}
          {dispute.locationType && ` — ${t(LOCATION_TYPE_KEYS[dispute.locationType])}`}
        </span>
        <span className="text-muted-foreground">{t('disputeQueue.reasonLabel')}</span>
        <span>{dispute.reason}</span>
      </div>

      {!dispute.reportedByUserId && (
        <p className="text-xs text-muted-foreground">{t('disputeQueue.reportedBySystem')}</p>
      )}

      {refundMutation.isError && (
        <p className="text-sm text-destructive">
          {refundErrorCode === 'CONFLICT'
            ? t('disputeQueue.errors.notRefundable')
            : refundErrorCode === 'NOT_FOUND'
              ? t('disputeQueue.errors.appointmentNotFound')
              : t('disputeQueue.errors.generic')}
        </p>
      )}
      {refundMutation.isSuccess && <p className="text-sm text-primary">{t('disputeQueue.refundSuccess')}</p>}

      {forceCancelMutation.isError && (
        <p className="text-sm text-destructive">
          {forceCancelErrorCode === 'CONFLICT'
            ? t('disputeQueue.errors.alreadyCancelled')
            : forceCancelErrorCode === 'NOT_FOUND'
              ? t('disputeQueue.errors.appointmentNotFound')
              : t('disputeQueue.errors.generic')}
        </p>
      )}
      {forceCancelMutation.isSuccess && (
        <p className="text-sm text-primary">{t('disputeQueue.forceCancelSuccess')}</p>
      )}

      {resolveMutation.isError && (
        <p className="text-sm text-destructive">
          {resolveErrorCode === 'CONFLICT'
            ? t('disputeQueue.errors.alreadyResolved')
            : t('disputeQueue.errors.generic')}
        </p>
      )}

      <div className="flex flex-wrap gap-2">
        <Button
          type="button"
          variant="outline"
          size="sm"
          disabled={refundMutation.isPending || refundMutation.isSuccess}
          onClick={() => refundMutation.mutate()}
        >
          {t('disputeQueue.refund')}
        </Button>
        <Button
          type="button"
          variant="destructive"
          size="sm"
          disabled={forceCancelMutation.isPending || forceCancelMutation.isSuccess}
          onClick={() => forceCancelMutation.mutate()}
        >
          {t('disputeQueue.forceCancel')}
        </Button>
        <Button
          type="button"
          size="sm"
          disabled={resolveMutation.isPending}
          onClick={() => resolveMutation.mutate()}
        >
          {t('disputeQueue.resolve')}
        </Button>
      </div>
    </li>
  )
}
