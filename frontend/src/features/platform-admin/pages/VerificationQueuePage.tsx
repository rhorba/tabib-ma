import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { VerificationQueueItem } from '../components/VerificationQueueItem'

export function VerificationQueuePage() {
  const { t } = useTranslation()

  const queueQuery = useQuery({
    queryKey: ['verification-queue'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/admin/platform/verification-queue')
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
            {t('verificationQueue.title')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {queueQuery.data && queueQuery.data.length > 0 ? (
            <ul className="grid gap-4">
              {queueQuery.data.map((profile) => (
                <VerificationQueueItem key={profile.id} profile={profile} />
              ))}
            </ul>
          ) : (
            !queueQuery.isLoading && (
              <p className="text-sm text-muted-foreground">{t('verificationQueue.empty')}</p>
            )
          )}
        </CardContent>
      </Card>
    </div>
  )
}
