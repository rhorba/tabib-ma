import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { apiClient } from '@/shared/api/client'

function useMyAvailabilityRules() {
  return useQuery({
    queryKey: ['availability-rules', 'mine'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/booking/availability/rules')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

export function AvailabilityRulesList() {
  const { t } = useTranslation()
  const rulesQuery = useMyAvailabilityRules()

  if (!rulesQuery.data || rulesQuery.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('booking.availability.rules.empty')}</p>
  }

  return (
    <ul className="grid gap-2 text-sm">
      {rulesQuery.data.map((rule) => (
        <li
          key={rule.id}
          className="flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2"
        >
          <span>
            {t(`booking.availability.days.${rule.dayOfWeek}`)} · {rule.startTime?.slice(0, 5)}–
            {rule.endTime?.slice(0, 5)} · {rule.slotDurationMinutes}
            {t('booking.availability.rules.minutesAbbrev')}
          </span>
          <span className="text-muted-foreground">
            {t(
              rule.locationType === 'VIDEO'
                ? 'booking.availability.locationTypes.video'
                : 'booking.availability.locationTypes.inPerson'
            )}
          </span>
        </li>
      ))}
    </ul>
  )
}
