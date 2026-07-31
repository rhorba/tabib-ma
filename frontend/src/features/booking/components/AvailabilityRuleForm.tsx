import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
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

export function AvailabilityRuleForm() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<AvailabilityRuleFormValues>({
    resolver: zodResolver(createAvailabilityRuleSchema(t)),
    defaultValues: {
      dayOfWeek: 'MONDAY',
      startTime: '09:00',
      endTime: '17:00',
      slotDurationMinutes: 30,
      locationType: 'IN_PERSON',
    },
  })

  const mutation = useMutation({
    mutationFn: async (values: AvailabilityRuleFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/booking/availability/rules', {
        body: { ...values, startTime: `${values.startTime}:00`, endTime: `${values.endTime}:00` },
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability-rules', 'mine'] })
      form.reset()
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
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending
            ? t('booking.availability.form.submitting')
            : t('booking.availability.form.submit')}
        </Button>
      </form>
    </Form>
  )
}
