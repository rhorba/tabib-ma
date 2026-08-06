import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/shared/components/ui/form'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/components/ui/select'
import { Textarea } from '@/shared/components/ui/textarea'
import { apiClient } from '@/shared/api/client'
import { createReportDisputeSchema, type ReportDisputeFormValues } from '../schemas'

/** Story 10.1: a patient or doctor reporting a problem on their own appointment. Unlike
 * ReviewForm, there is no "already reported" list to check against — DisputeService.report()
 * has no uniqueness constraint, so this only tracks local submitted-state for this render. */
export function DisputeReportForm({ appointmentId, onSubmitted }: { appointmentId: string; onSubmitted: () => void }) {
  const { t } = useTranslation()

  const form = useForm<ReportDisputeFormValues>({
    resolver: zodResolver(createReportDisputeSchema(t)),
    defaultValues: { type: 'COMPLAINT', reason: '' },
  })

  const mutation = useMutation({
    mutationFn: async (values: ReportDisputeFormValues) => {
      const { error } = await apiClient.POST('/api/v1/disputes', {
        body: { appointmentId, type: values.type, reason: values.reason },
      })
      if (error) {
        throw error
      }
    },
    onSuccess: onSubmitted,
  })

  return (
    <Form {...form}>
      <form
        className="grid gap-3 rounded-md border border-border p-3"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
        noValidate
      >
        {mutation.isError && (
          <Alert variant="destructive">
            <AlertCircleIcon />
            <AlertDescription>{t('dispute.report.errors.generic')}</AlertDescription>
          </Alert>
        )}

        <FormField
          control={form.control}
          name="type"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('dispute.report.typeLabel')}</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="COMPLAINT">{t('disputeQueue.types.complaint')}</SelectItem>
                  <SelectItem value="PAYMENT_ISSUE">{t('disputeQueue.types.paymentIssue')}</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="reason"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('dispute.report.reasonLabel')}</FormLabel>
              <FormControl>
                <Textarea {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" disabled={mutation.isPending} className="justify-self-start">
          {mutation.isPending ? t('dispute.report.submitting') : t('dispute.report.submit')}
        </Button>
      </form>
    </Form>
  )
}
