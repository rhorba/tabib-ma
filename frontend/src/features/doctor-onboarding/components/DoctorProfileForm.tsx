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
import { createDoctorProfileSchema, type DoctorProfileFormValues } from '../schemas'

export function DoctorProfileForm() {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<DoctorProfileFormValues>({
    resolver: zodResolver(createDoctorProfileSchema(t)),
    defaultValues: { specialty: '', bio: '', consultationFeeMad: 0, city: '' },
  })

  const mutation = useMutation({
    mutationFn: async (values: DoctorProfileFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/clinic/doctor-profiles', {
        body: values,
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['doctor-profile', 'me'] })
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
            <AlertDescription>{t('doctorOnboarding.errors.generic')}</AlertDescription>
          </Alert>
        )}
        <FormField
          control={form.control}
          name="specialty"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('doctorOnboarding.form.specialtyLabel')}</FormLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="city"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('doctorOnboarding.form.cityLabel')}</FormLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="consultationFeeMad"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('doctorOnboarding.form.consultationFeeLabel')}</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  step="0.01"
                  min="0"
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
          name="bio"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('doctorOnboarding.form.bioLabel')}</FormLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending ? t('doctorOnboarding.form.submitting') : t('doctorOnboarding.form.submit')}
        </Button>
      </form>
    </Form>
  )
}
