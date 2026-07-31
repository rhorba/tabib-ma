import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router'
import { apiClient } from '@/shared/api/client'

const STATUS_STYLES: Record<string, string> = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800',
  CONFIRMED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-slate-200 text-slate-700',
  COMPLETED: 'bg-blue-100 text-blue-800',
  NO_SHOW: 'bg-red-100 text-red-800',
}

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
  const appointmentsQuery = useMyAppointments()

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 px-4 py-12">
      <h1 className="text-xl font-semibold text-foreground" role="heading" aria-level={1}>
        {t('booking.myAppointments.title')}
      </h1>

      {appointmentsQuery.data && appointmentsQuery.data.length > 0 ? (
        <ul className="grid gap-3">
          {appointmentsQuery.data.map((appointment) => (
            <li
              key={appointment.id}
              className="flex items-center justify-between gap-3 rounded-md border border-border px-4 py-3 text-sm"
            >
              <div className="grid gap-1">
                <span>{appointment.startsAt && new Date(appointment.startsAt).toLocaleString()}</span>
                <Link
                  to={`/doctors/${appointment.doctorProfileId}`}
                  className="text-primary hover:underline"
                >
                  {t('booking.myAppointments.viewDoctor')}
                </Link>
              </div>
              <span
                className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_STYLES[appointment.status ?? 'PENDING_PAYMENT']}`}
              >
                {t(`booking.myAppointments.status.${(appointment.status ?? 'PENDING_PAYMENT').toLowerCase()}`)}
              </span>
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
