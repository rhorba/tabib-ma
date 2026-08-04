import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'

const STATUS_STYLES: Record<string, string> = {
  active: 'bg-green-100 text-green-800',
  inactive: 'bg-gray-100 text-gray-600',
}

function useClinicResources(clinicId: string) {
  return useQuery({
    queryKey: ['clinic', 'resources', clinicId],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/clinics/{clinicId}/resources', {
        params: { path: { clinicId } },
      })
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

export function ClinicResourceList({ clinicId }: { clinicId: string }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const resourcesQuery = useClinicResources(clinicId)

  const deactivateMutation = useMutation({
    mutationFn: async (resourceId: string) => {
      const { data, error } = await apiClient.PATCH(
        '/api/v1/clinic/clinics/{clinicId}/resources/{resourceId}/deactivate',
        { params: { path: { clinicId, resourceId } } },
      )
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clinic', 'resources', clinicId] })
    },
  })

  if (!resourcesQuery.data || resourcesQuery.data.length === 0) {
    return <p className="text-sm text-muted-foreground">{t('clinicAdmin.resources.empty')}</p>
  }

  return (
    <ul className="grid gap-2 text-sm">
      {resourcesQuery.data.map((resource) => (
        <li
          key={resource.id}
          className="flex items-center justify-between rounded-md border border-border px-3 py-2"
        >
          <div className="flex items-center gap-2">
            <span>{resource.name}</span>
            <span className="text-xs text-muted-foreground">
              {t(`clinicAdmin.resources.types.${resource.type === 'ROOM' ? 'room' : 'equipment'}`)}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span
              className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                resource.active ? STATUS_STYLES.active : STATUS_STYLES.inactive
              }`}
            >
              {t(
                resource.active
                  ? 'clinicAdmin.resources.statusActive'
                  : 'clinicAdmin.resources.statusInactive',
              )}
            </span>
            {resource.active && (
              <Button
                type="button"
                variant="outline"
                size="sm"
                disabled={deactivateMutation.isPending}
                onClick={() => deactivateMutation.mutate(resource.id!)}
              >
                {t('clinicAdmin.resources.deactivate')}
              </Button>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}
