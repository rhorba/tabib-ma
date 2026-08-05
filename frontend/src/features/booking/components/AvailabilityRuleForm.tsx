import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Checkbox } from '@/shared/components/ui/checkbox'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select'
import { apiClient } from '@/shared/api/client'
import { createAvailabilityRuleSchema, type AvailabilityRuleFormValues } from '../schemas'

const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
] as const

const NO_CLINIC = '__none__'

function useMyClinics() {
  return useQuery({
    queryKey: ['doctor-profile', 'me', 'clinics'],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/doctor-profiles/me/clinics')
      if (error) {
        throw error
      }
      return data ?? []
    },
  })
}

function useClinicResources(clinicId: string | undefined) {
  return useQuery({
    queryKey: ['clinic', 'resources', clinicId],
    queryFn: async () => {
      const { data, error } = await apiClient.GET('/api/v1/clinic/clinics/{clinicId}/resources', {
        params: { path: { clinicId: clinicId! } },
      })
      if (error) {
        throw error
      }
      return data ?? []
    },
    enabled: clinicId !== undefined,
  })
}

export function AvailabilityRuleForm() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()
  const clinicsQuery = useMyClinics()

  const form = useForm<AvailabilityRuleFormValues>({
    resolver: zodResolver(createAvailabilityRuleSchema(t)),
    defaultValues: {
      dayOfWeek: 'MONDAY',
      startTime: '09:00',
      endTime: '17:00',
      slotDurationMinutes: 30,
      locationType: 'IN_PERSON',
      clinicId: undefined,
      resourceIds: [],
    },
  })

  const locationType = form.watch('locationType')
  const clinicId = form.watch('clinicId')
  const resourcesQuery = useClinicResources(
    locationType === 'IN_PERSON' ? clinicId : undefined,
  )

  const mutation = useMutation({
    mutationFn: async (values: AvailabilityRuleFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/booking/availability/rules', {
        body: {
          ...values,
          startTime: `${values.startTime}:00`,
          endTime: `${values.endTime}:00`,
          clinicId: values.locationType === 'IN_PERSON' ? values.clinicId : undefined,
          resourceIds: values.locationType === 'IN_PERSON' ? values.resourceIds : undefined,
        },
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability-rules', 'mine'] })
      form.reset({
        dayOfWeek: 'MONDAY',
        startTime: '09:00',
        endTime: '17:00',
        slotDurationMinutes: 30,
        locationType: 'IN_PERSON',
        clinicId: undefined,
        resourceIds: [],
      })
    },
  })

  return (
    <Form {...form}>
      <form
        className="grid gap-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        noValidate
      >
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertDescription>{t('booking.availability.errors.generic')}</AlertDescription>
          </Alert>
        )}
        <FormField
          control={form.control}
          name="dayOfWeek"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('booking.availability.form.dayOfWeekLabel')}</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  {DAYS_OF_WEEK.map((day) => (
                    <SelectItem key={day} value={day}>
                      {t(`booking.availability.days.${day}`)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />
        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="startTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('booking.availability.form.startTimeLabel')}</FormLabel>
                <FormControl>
                  <Input type="time" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="endTime"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('booking.availability.form.endTimeLabel')}</FormLabel>
                <FormControl>
                  <Input type="time" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
        <FormField
          control={form.control}
          name="slotDurationMinutes"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('booking.availability.form.durationLabel')}</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  min="5"
                  step="5"
                  {...field}
                  onChange={(e) => field.onChange(e.target.valueAsNumber)}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="locationType"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('booking.availability.form.locationTypeLabel')}</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="IN_PERSON">
                    {t('booking.availability.locationTypes.inPerson')}
                  </SelectItem>
                  <SelectItem value="VIDEO">{t('booking.availability.locationTypes.video')}</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />
        {locationType === 'IN_PERSON' && clinicsQuery.data && clinicsQuery.data.length > 0 && (
          <FormField
            control={form.control}
            name="clinicId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('booking.availability.form.clinicLabel')}</FormLabel>
                <Select
                  value={field.value ?? NO_CLINIC}
                  onValueChange={(value) => {
                    field.onChange(value === NO_CLINIC ? undefined : value)
                    form.setValue('resourceIds', [])
                  }}
                >
                  <FormControl>
                    <SelectTrigger className="w-full">
                      <SelectValue />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    <SelectItem value={NO_CLINIC}>
                      {t('booking.availability.form.noClinic')}
                    </SelectItem>
                    {clinicsQuery.data.map((clinic) => (
                      <SelectItem key={clinic.id} value={clinic.id!}>
                        {clinic.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <FormMessage />
              </FormItem>
            )}
          />
        )}
        {locationType === 'IN_PERSON' && clinicId && (
          <FormField
            control={form.control}
            name="resourceIds"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('booking.availability.form.resourcesLabel')}</FormLabel>
                {resourcesQuery.data && resourcesQuery.data.length > 0 ? (
                  <div className="grid gap-2">
                    {resourcesQuery.data.map((resource) => {
                      // Each checkbox needs its own id — FormControl would stamp the same
                      // formItemId from the enclosing FormItem onto every one, producing
                      // invalid duplicate DOM ids (this is a list of controls, not a single one).
                      const checkboxId = `availability-rule-resource-${resource.id}`
                      return (
                        <label
                          key={resource.id}
                          htmlFor={checkboxId}
                          className="flex items-center gap-2 text-sm font-normal"
                        >
                          <Checkbox
                            id={checkboxId}
                            checked={field.value?.includes(resource.id!) ?? false}
                            onCheckedChange={(checked) => {
                              const current = field.value ?? []
                              field.onChange(
                                checked
                                  ? [...current, resource.id!]
                                  : current.filter((id) => id !== resource.id),
                              )
                            }}
                          />
                          {resource.name}
                        </label>
                      )
                    })}
                  </div>
                ) : (
                  <p className="text-sm text-muted-foreground">
                    {t('booking.availability.form.resourcesEmpty')}
                  </p>
                )}
                <FormMessage />
              </FormItem>
            )}
          />
        )}
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending
            ? t('booking.availability.form.submitting')
            : t('booking.availability.form.submit')}
        </Button>
      </form>
    </Form>
  )
}
