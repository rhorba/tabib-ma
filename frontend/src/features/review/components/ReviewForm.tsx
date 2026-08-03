import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import { AlertCircleIcon, StarIcon } from 'lucide-react'
import { Alert, AlertDescription } from '@/shared/components/ui/alert'
import { Button } from '@/shared/components/ui/button'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/shared/components/ui/form'
import { Textarea } from '@/shared/components/ui/textarea'
import { apiClient } from '@/shared/api/client'
import { createSubmitReviewSchema, type SubmitReviewFormValues } from '../schemas'

const STARS = [1, 2, 3, 4, 5]

/** Story 9.1: a patient reviews a COMPLETED appointment exactly once — the parent
 * (MyAppointmentsPage) is responsible for only rendering this for appointments not
 * already in GET /reviews/mine. */
export function ReviewForm({ appointmentId, onSubmitted }: { appointmentId: string; onSubmitted: () => void }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<SubmitReviewFormValues>({
    resolver: zodResolver(createSubmitReviewSchema(t)),
    defaultValues: { rating: 0, comment: '' },
  })

  const mutation = useMutation({
    mutationFn: async (values: SubmitReviewFormValues) => {
      const { error } = await apiClient.POST('/api/v1/reviews', {
        body: { appointmentId, rating: values.rating, comment: values.comment || undefined },
      })
      if (error) {
        throw error
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews', 'mine'] })
      onSubmitted()
    },
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
            <AlertDescription>{t('review.form.errors.generic')}</AlertDescription>
          </Alert>
        )}

        <FormField
          control={form.control}
          name="rating"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('review.form.ratingLabel')}</FormLabel>
              <FormControl>
                <div role="radiogroup" aria-label={t('review.form.ratingLabel')} className="flex gap-1">
                  {STARS.map((value) => (
                    <button
                      key={value}
                      type="button"
                      role="radio"
                      aria-checked={field.value === value}
                      aria-label={t('review.form.starLabel', { value })}
                      onClick={() => field.onChange(value)}
                      className={value <= field.value ? 'text-primary' : 'text-muted-foreground'}
                    >
                      <StarIcon fill={value <= field.value ? 'currentColor' : 'none'} />
                    </button>
                  ))}
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="comment"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('review.form.commentLabel')}</FormLabel>
              <FormControl>
                <Textarea {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" disabled={mutation.isPending} className="justify-self-start">
          {mutation.isPending ? t('review.form.submitting') : t('review.form.submit')}
        </Button>
      </form>
    </Form>
  )
}
