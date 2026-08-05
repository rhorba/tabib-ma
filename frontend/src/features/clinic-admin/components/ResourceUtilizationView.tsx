import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { apiClient } from '@/shared/api/client'

function useResourceUtilization(enabled: boolean) {
  return useQuery({
    queryKey: ['clinic', 'resources', 'utilization'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/clinics/resources/utilization')
      if (error) {
        throw error
      }
      return data ?? []
    },
    enabled,
  })
}

export function ResourceUtilizationView({ enabled }: { enabled: boolean }) {
  const { t, i18n } = useTranslation()
  const utilizationQuery = useResourceUtilization(enabled)

  const formatWindow = (startsAt: string, endsAt: string) => {
    const formatter = new Intl.DateTimeFormat(i18n.language, {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
    return `${formatter.format(new Date(startsAt))} → ${formatter.format(new Date(endsAt))}`
  }

  if (!utilizationQuery.data || utilizationQuery.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('clinicAdmin.utilization.empty')}</p>
  }

  return (
    <ul className="grid gap-3 text-sm">
      {utilizationQuery.data.map((resource) => (
        <li key={resource.resourceId} className="rounded-md border border-border px-3 py-2">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="font-medium">{resource.resourceName}</span>
              <span className="text-xs text-muted-foreground">
                {t(`clinicAdmin.resources.types.${resource.type === 'ROOM' ? 'room' : 'equipment'}`)}
              </span>
            </div>
            {!resource.active && (
              <span className="inline-flex rounded-full bg-gray-100 px-2 py-0.5 text-xs font-medium text-gray-600">
                {t('clinicAdmin.resources.statusInactive')}
              </span>
            )}
          </div>
          {resource.allocations && resource.allocations.length > 0 ? (
            <ul className="mt-2 grid gap-1 text-xs text-muted-foreground">
              {resource.allocations.map((allocation) => (
                <li key={allocation.appointmentId}>
                  {formatWindow(allocation.startsAt!, allocation.endsAt!)}
                </li>
              ))}
            </ul>
          ) : (
            <p className="mt-2 text-xs text-muted-foreground">
              {t('clinicAdmin.utilization.noAllocations')}
            </p>
          )}
        </li>
      ))}
    </ul>
  )
}
