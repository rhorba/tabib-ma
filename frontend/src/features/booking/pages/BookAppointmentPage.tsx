import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { Link, useParams } from 'react-router'
import { AlertCircleIcon, CheckCircle2Icon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/shared/components/ui/card'
import { apiClient } from '@/shared/api/client'
import { getApiErrorCode } from '@/shared/api/errors'
import { SlotPicker } from '../components/SlotPicker'

// Shares the ['doctor-public-profile', id] query key with DoctorPublicProfilePage's own hook.
function useDoctorPublicProfile(doctorProfileId: string | undefined) {
  return useQuery({
    queryKey: ['doctor-public-profile', doctorProfileId],
    queryFn: async () => {
      const { data, error, response } = await apiClient.GET(
        '/api/v1/clinic/doctor-profiles/{doctorProfileId}/public',
        { params: { path: { doctorProfileId: doctorProfileId! } } }
      )
      if (response.status === 404) {
        return null
      }
      if (error) {
        throw error
      }
      return data ?? null
    },
    enabled: doctorProfileId !== undefined,
  })
}

export function BookAppointmentPage() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const { doctorProfileId } = useParams<{ doctorProfileId: string }>()
  const profileQuery = useDoctorPublicProfile(doctorProfileId)
  const [selectedSlotId, setSelectedSlotId] = useState<string | null>(null)

  const bookMutation = useMutation({
    mutationFn: async (availabilitySlotId: string) => {
      const { data, error } = await apiClient.POST('/api/v1/booking/appointments', {
        body: { availabilitySlotId },
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability-slots', doctorProfileId] })
      queryClient.invalidateQueries({ queryKey: ['appointments', 'mine'] })
    },
  })

  if (profileQuery.isLoading) {
    return null
  }

  if (!profileQuery.data) {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-4 px-4 py-12">
        <p className="text-sm text-muted-foreground">{t('doctorPublicProfile.notFound')}</p>
        <Link to="/search" className="text-sm font-medium text-primary hover:underline">
          {t('doctorPublicProfile.backToSearch')}
        </Link>
      </div>
    )
  }

  const profile = profileQuery.data
  const errorCode = getApiErrorCode(bookMutation.error)
  const errorMessage =
    bookMutation.isError &&
    (errorCode === 'CONFLICT' ? t('booking.book.errors.slotTaken') : t('booking.book.errors.generic'))

  if (bookMutation.isSuccess && bookMutation.data) {
    const confirmed = bookMutation.data.status === 'CONFIRMED'
    return (
      <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-4 px-4 py-12">
        <Alert variant={confirmed ? 'default' : 'destructive'}>
          {confirmed ? <CheckCircle2Icon /> : <AlertCircleIcon />}
          <AlertDescription>
            {confirmed ? t('booking.book.result.confirmed') : t('booking.book.result.paymentFailed')}
          </AlertDescription>
        </Alert>
        <Link to="/appointments" className="text-sm font-medium text-primary hover:underline">
          {t('booking.book.result.viewAppointments')}
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 px-4 py-12">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl" role="heading" aria-level={1}>
            {t('booking.book.title', { name: `${profile.firstName} ${profile.lastName}` })}
          </CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 text-sm">
          <p className="text-muted-foreground">
            {t('booking.book.feeLabel')}: {profile.consultationFeeMad} MAD
          </p>
          <SlotPicker
            doctorProfileId={doctorProfileId!}
            selectedSlotId={selectedSlotId}
            onSelect={setSelectedSlotId}
          />
        </CardContent>
      </Card>

      {errorMessage && (
        <Alert variant="destructive">
          <AlertCircleIcon />
          <AlertDescription>{errorMessage}</AlertDescription>
        </Alert>
      )}

      <Button
        type="button"
        disabled={!selectedSlotId || bookMutation.isPending}
        onClick={() => selectedSlotId && bookMutation.mutate(selectedSlotId)}
      >
        {bookMutation.isPending ? t('booking.book.confirming') : t('booking.book.confirmAndPay')}
      </Button>
    </div>
  )
}
