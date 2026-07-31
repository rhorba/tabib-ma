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
import { apiClient } from '@/shared/api/client'
import { getApiErrorCode } from '@/shared/api/errors'
import { createExceptionSchema, type AvailabilityExceptionFormValues } from '../schemas'

export function AvailabilityExceptionForm() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<AvailabilityExceptionFormValues>({
    resolver: zodResolver(createExceptionSchema(t)),
    defaultValues: { exceptionDate: '', reason: '' },
  })

  const mutation = useMutation({
    mutationFn: async (values: AvailabilityExceptionFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/booking/availability/exceptions', {
        body: values,
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['availability-exceptions', 'mine'] })
      form.reset({ exceptionDate: '', reason: '' })
    },
  })

  const errorCode = getApiErrorCode(mutation.error)
  const errorMessage =
    mutation.isError &&
    (errorCode === 'CONFLICT'
      ? t('booking.availability.exceptions.errors.alreadyMarked')
      : t('booking.availability.errors.generic'))

  return (
    <Form {...form}>
      <form
        className="grid gap-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        noValidate
      >
        {errorMessage && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}
        <FormField
          control={form.control}
          name="exceptionDate"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('booking.availability.exceptions.dateLabel')}</FormLabel>
              <FormControl>
                <Input type="date" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="reason"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('booking.availability.exceptions.reasonLabel')}</FormLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending
            ? t('booking.availability.exceptions.submitting')
            : t('booking.availability.exceptions.submit')}
        </Button>
      </form>
    </Form>
  )
}
