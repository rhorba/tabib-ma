import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { apiClient } from '@/shared/api/client'

function useMyAvailabilityExceptions() {
  return useQuery({
    queryKey: ['availability-exceptions', 'mine'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/booking/availability/exceptions')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

export function AvailabilityExceptionsList() {
  const { t } = useTranslation()
  const exceptionsQuery = useMyAvailabilityExceptions()

  if (!exceptionsQuery.data || exceptionsQuery.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('booking.availability.exceptions.empty')}</p>
  }

  return (
    <ul className="grid gap-2 text-sm">
      {exceptionsQuery.data.map((exception) => (
        <li
          key={exception.id}
          className="flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2"
        >
          <span>{exception.exceptionDate}</span>
          {exception.reason && <span className="text-muted-foreground">{exception.reason}</span>}
        </li>
      ))}
    </ul>
  )
}
