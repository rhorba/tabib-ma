import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router'
import { AlertCircleIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { apiClient } from '@/shared/api/client'
import { getApiErrorCode } from '@/shared/api/errors'
import { useAuth } from '@/features/auth/AuthContext'

const STATUS_STYLES: Record<string, string> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800',
  CONFIRMED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-slate-200 text-slate-700',
  COMPLETED: 'bg-blue-100 text-blue-800',
  NO_SHOW: 'bg-red-100 text-red-800',
}

// Only these statuses make sense to cancel or reschedule — mirrors
// Appointment.cancel()'s own guard (CANCELLED/COMPLETED can't be re-cancelled).
const CANCELLABLE_STATUSES = new Set(['PENDING_PAYMENT', 'CONFIRMED'])

function useMyAppointments() {
  return useQuery({
    queryKey: ['appointments', 'mine'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/booking/appointments')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

export function MyAppointmentsPage() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const appointmentsQuery = useMyAppointments()
  // CancellationService only ever authorizes the patient who booked the
  // appointment (backend 403s a doctor's cancel attempt) — a doctor's view
  // of their own appointments is read-only, plus the Join Video link below.
  const canManage = user?.role === 'PATIENT'

  const cancelAppointment = async (appointmentId: string) => {
    const { error } = await apiClient.POST('/api/v1/booking/appointments/{appointmentId}/cancel', {
      params: { path: { appointmentId } },
    })
    if (error) {
      throw error
    }
  }

  const cancelMutation = useMutation({
    mutationFn: cancelAppointment,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['appointments', 'mine'] })
    },
  })

  // Story 4.4's "reschedule" is cancel-existing (policy-checked, same endpoint
  // as a plain cancel) + book-new, composed here rather than as a dedicated
  // backend endpoint — see .logs/decisions.md 2026-07-31.
  const rescheduleMutation = useMutation({
    mutationFn: async (appointment: { id: string; doctorProfileId: string }) => {
      await cancelAppointment(appointment.id)
      return appointment
    },
    onSuccess: (appointment) => {
      queryClient.invalidateQueries({ queryKey: ['appointments', 'mine'] })
      navigate(`/doctors/${appointment.doctorProfileId}/book`)
    },
  })

  const errorCode = getApiErrorCode(cancelMutation.error ?? rescheduleMutation.error)
  const errorMessage =
    (cancelMutation.isError || rescheduleMutation.isError) &&
    (errorCode === 'CONFLICT'
      ? t('booking.myAppointments.errors.alreadyDecided')
      : t('booking.myAppointments.errors.generic'))

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 py-12">
      <h1 className="text-xl font-semibold text-foreground" role="heading" aria-level={1}>
        {t('booking.myAppointments.title')}
      </h1>

      {errorMessage && (
        <Alert variant="destructive">
          <AlertCircleIcon />
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      {appointmentsQuery.data && appointmentsQuery.data.length > 0 ? (
        <ul className="grid gap-3">
          {appointmentsQuery.data.map((appointment) => (
            <li
              key={appointment.id}
              className="flex items-center justify-between gap-3 rounded-md border border-border px-4 py-3 text-sm"
            >
              <div className="grid gap-1">
                <span>{appointment.startsAt && new Date(appointment.startsAt).toLocaleString()}</span>
                {canManage && (
                  <Link
                    to={`/doctors/${appointment.doctorProfileId}`}
                    className="text-primary hover:underline"
                  >
                    {t('booking.myAppointments.viewDoctor')}
                  </Link>
                )}
              </div>
              <div className="flex items-center gap-3">
                <span
                  className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[appointment.status ?? 'PENDING_PAYMENT']}`}
                >
                  {t(`booking.myAppointments.status.${(appointment.status ?? 'PENDING_PAYMENT').toLowerCase()}`)}
                </span>
                {appointment.locationType === 'VIDEO' && appointment.status === 'CONFIRMED' && (
                  <Link
                    to={`/appointments/${appointment.id}/consultation`}
                    className="text-sm font-medium text-primary hover:underline"
                  >
                    {t('booking.myAppointments.joinVideo')}
                  </Link>
                )}
                {canManage && CANCELLABLE_STATUSES.has(appointment.status ?? '') && (
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={cancelMutation.isPending || rescheduleMutation.isPending}
                      onClick={() => cancelMutation.mutate(appointment.id!)}
                    >
                      {t('booking.myAppointments.cancel')}
                    </Button>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      disabled={cancelMutation.isPending || rescheduleMutation.isPending}
                      onClick={() =>
                        rescheduleMutation.mutate({
                          id: appointment.id!,
                          doctorProfileId: appointment.doctorProfileId!,
                        })
                      }
                    >
                      {t('booking.myAppointments.reschedule')}
                    </Button>
                  </div>
                )}
              </div>
            </li>
          ))}
        </ul>
      ) : (
        appointmentsQuery.isSuccess && (
          <p className="text-sm text-muted-foreground">{t('booking.myAppointments.empty')}</p>
        )
      )}
    </div>
  )
}
