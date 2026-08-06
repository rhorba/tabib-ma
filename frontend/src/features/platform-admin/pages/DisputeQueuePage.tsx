import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { DisputeQueueItem } from '../components/DisputeQueueItem'

export function DisputeQueuePage() {
  const { t } = useTranslation()

  const queueQuery = useQuery({
    queryKey: ['dispute-queue'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/admin/platform/disputes')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('disputeQueue.title')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {queueQuery.data && queueQuery.data.length > 0 ? (
            <ul className="grid gap-4">
              {queueQuery.data.map((dispute) => (
                <DisputeQueueItem key={dispute.id} dispute={dispute} />
              ))}
            </ul>
          ) : (
            !queueQuery.isLoading && (
              <p className="text-sm text-muted-foreground">{t('disputeQueue.empty')}</p>
            )
          )}
        </CardContent>
      </Card>
    </div>
  )
}
