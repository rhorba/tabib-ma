import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useFieldArray, useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon, PlusIcon, XIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/shared/components/ui/form'
import { Input } from '@/shared/components/ui/input'
import { apiClient } from '@/shared/api/client'
import { createCompleteConsultationSchema, type CompleteConsultationFormValues } from '../schemas'

/** Story 6.3 (amended 2026-08-07): the doctor fills this in the same session as the call,
 * completing the consultation and issuing the prescription in one submit — or completes without
 * one via the "skip prescription" action, for consults where none is medically warranted. */
export function CompleteConsultationForm({
  consultationId,
  onCompleted,
}: {
  consultationId: string
  onCompleted: (prescribed: boolean) => void
}) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<CompleteConsultationFormValues>({
    resolver: zodResolver(createCompleteConsultationSchema(t)),
    defaultValues: { items: [{ medicationName: '', dosage: '', instructions: '' }] },
  })
  const fieldArray = useFieldArray({ control: form.control, name: 'items' })

  const mutation = useMutation({
    mutationFn: async (values: CompleteConsultationFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/consultations/{consultationId}/complete', {
        params: { path: { consultationId } },
        body: values,
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: (_data, values) => {
      queryClient.invalidateQueries({ queryKey: ['consultation', consultationId] })
      // Completing marks the underlying Appointment COMPLETED too (Story 9.1) — without this,
      // MyAppointmentsPage keeps showing CONFIRMED until an unrelated refetch happens to occur.
      queryClient.invalidateQueries({ queryKey: ['appointments', 'mine'] })
      onCompleted(values.items.length > 0)
    },
  })

  return (
    <Form {...form}>
      <form
        className="grid gap-4 rounded-md border border-border p-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        noValidate
      >
        <h2 className="text-sm font-semibold text-foreground">{t('consultation.complete.title')}</h2>
        <p className="text-sm text-muted-foreground">{t('consultation.complete.prompt')}</p>

        {mutation.isError && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertDescription>{t('consultation.complete.errors.generic')}</AlertDescription>
          </Alert>
        )}

        {fieldArray.fields.map((field, index) => (
          <div key={field.id} className="grid gap-3 rounded-md border border-border p-3">
            <div className="grid grid-cols-2 gap-3">
              <FormField
                control={form.control}
                name={`items.${index}.medicationName`}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('consultation.complete.medicationLabel')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name={`items.${index}.dosage`}
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('consultation.complete.dosageLabel')}</FormLabel>
                    <FormControl>
                      <Input {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>
            <FormField
              control={form.control}
              name={`items.${index}.instructions`}
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('consultation.complete.instructionsLabel')}</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            {fieldArray.fields.length > 1 && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={() => fieldArray.remove(index)}
                className="justify-self-start"
              >
                <XIcon /> {t('consultation.complete.removeItem')}
              </Button>
            )}
          </div>
        ))}

        <Button
          type="button"
          variant="outline"
          size="sm"
          className="justify-self-start"
          onClick={() => fieldArray.append({ medicationName: '', dosage: '', instructions: '' })}
        >
          <PlusIcon /> {t('consultation.complete.addItem')}
        </Button>

        <div className="mt-2 flex flex-wrap items-center gap-3">
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? t('consultation.complete.submitting') : t('consultation.complete.submit')}
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={mutation.isPending}
            onClick={() => mutation.mutate({ items: [] })}
          >
            {t('consultation.complete.skipPrescription')}
          </Button>
        </div>
      </form>
    </Form>
  )
}
