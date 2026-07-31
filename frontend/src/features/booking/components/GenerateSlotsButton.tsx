import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon, CheckCircle2Icon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'

// No date-range picker: the backend defaults to [today, today+30 days) when the body is empty,
// which is enough for Story 4.1 — a custom range wasn't asked for by any doc or AC.
export function GenerateSlotsButton() {
  const { t } = useTranslation()

  const mutation = useMutation({
    mutationFn: async () => {
      const { data, error } = await apiClient.POST('/api/v1/booking/availability/generate', {
        body: {},
      })
      if (error) {
        throw error
      }
      return data ?? []
    },
  })

  return (
    <div className="grid gap-3">
      <Button
        type="button"
        variant="outline"
        disabled={mutation.isPending}
        onClick={() => mutation.mutate()}
      >
        {mutation.isPending
          ? t('booking.availability.generate.generating')
          : t('booking.availability.generate.submit')}
      </Button>
      {mutation.isError && (
        <Alert variant="destructive">
          <AlertCircleIcon />
          <AlertDescription>{t('booking.availability.errors.generic')}</AlertDescription>
        </Alert>
      )}
      {mutation.isSuccess && (
        <Alert>
          <CheckCircle2Icon />
          <AlertDescription>
            {t('booking.availability.generate.success', { count: mutation.data?.length ?? 0 })}
          </AlertDescription>
        </Alert>
      )}
    </div>
  )
}
