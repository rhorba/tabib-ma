import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'

export function PlatformHealthPage() {
  const { t } = useTranslation()

  const healthQuery = useQuery({
    queryKey: ['platform-health'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/admin/platform/health')
      if (error) {
        throw error
      }
      return data ?? null
    },
  })

  const health = healthQuery.data

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('platformHealth.title')}
          </CardTitle>
        </CardHeader>
        {health && (
          <CardContent className="grid gap-6">
            <div>
              <h2 className="mb-2 text-sm font-medium">{t('platformHealth.appointments.title')}</h2>
              <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <dt className="text-muted-foreground">{t('platformHealth.appointments.total')}</dt>
                <dd>{health.totalAppointments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.appointments.confirmed')}</dt>
                <dd>{health.confirmedAppointments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.appointments.cancelled')}</dt>
                <dd>{health.cancelledAppointments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.appointments.completed')}</dt>
                <dd>{health.completedAppointments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.appointments.noShow')}</dt>
                <dd>{health.noShowAppointments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.appointments.pendingPayment')}</dt>
                <dd>{health.pendingPaymentAppointments}</dd>
              </dl>
            </div>

            <div>
              <h2 className="mb-2 text-sm font-medium">{t('platformHealth.payments.title')}</h2>
              <dl className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <dt className="text-muted-foreground">{t('platformHealth.payments.succeeded')}</dt>
                <dd>{health.succeededPayments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.payments.failed')}</dt>
                <dd>{health.failedPayments}</dd>
                <dt className="text-muted-foreground">{t('platformHealth.payments.refunded')}</dt>
                <dd>{health.refundedPayments}</dd>
              </dl>
            </div>

            <p className="text-xs text-muted-foreground">{t('platformHealth.videoQualityNotTracked')}</p>
          </CardContent>
        )}
      </Card>
    </div>
  )
}
