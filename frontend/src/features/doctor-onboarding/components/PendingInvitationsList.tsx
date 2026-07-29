import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { getApiErrorCode } from '@/shared/api/errors'
import { AlertCircleIcon } from 'lucide-react'

function useMyPendingInvitations() {
  return useQuery({
    queryKey: ['clinic-invitations', 'me'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/invitations/me')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

export function PendingInvitationsList() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const invitationsQuery = useMyPendingInvitations()

  const decisionMutation = useMutation({
    mutationFn: async ({ id, decision }: { id: string; decision: 'accept' | 'decline' }) => {
      const path =
        decision === 'accept'
          ? '/api/v1/clinic/invitations/{invitationId}/accept'
          : '/api/v1/clinic/invitations/{invitationId}/decline'
      const { data, error } = await apiClient.POST(path, { params: { path: { invitationId: id } } })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clinic-invitations', 'me'] })
      queryClient.invalidateQueries({ queryKey: ['doctor-profile'] })
    },
  })

  const errorCode = getApiErrorCode(decisionMutation.error)
  const errorMessage =
    decisionMutation.isError &&
    (errorCode === 'CONFLICT'
      ? t('doctorOnboarding.invitations.errors.needsProfile')
      : t('doctorOnboarding.invitations.errors.generic'))

  if (!invitationsQuery.data || invitationsQuery.data.length === 0) {
    return null
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg" role="heading" aria-level={2}>
          {t('doctorOnboarding.invitations.title')}
        </CardTitle>
      </CardHeader>
      <CardContent className="grid gap-3">
        {errorMessage && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}
        <ul className="grid gap-2 text-sm">
          {invitationsQuery.data.map((invitation) => (
            <li
              key={invitation.id}
              className="flex items-center justify-between gap-3 rounded-md border border-border px-3 py-2"
            >
              <span>{invitation.clinicName ?? invitation.clinicId}</span>
              <div className="flex gap-2">
                <Button
                  type="button"
                  size="sm"
                  disabled={decisionMutation.isPending}
                  onClick={() => decisionMutation.mutate({ id: invitation.id!, decision: 'accept' })}
                >
                  {t('doctorOnboarding.invitations.accept')}
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={decisionMutation.isPending}
                  onClick={() => decisionMutation.mutate({ id: invitation.id!, decision: 'decline' })}
                >
                  {t('doctorOnboarding.invitations.decline')}
                </Button>
              </div>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}
