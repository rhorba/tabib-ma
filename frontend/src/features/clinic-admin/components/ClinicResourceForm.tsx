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
import { createClinicResourceSchema, type CreateClinicResourceFormValues } from '../schemas'

export function ClinicResourceForm({ clinicId }: { clinicId: string }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<CreateClinicResourceFormValues>({
    resolver: zodResolver(createClinicResourceSchema(t)),
    defaultValues: { type: 'ROOM', name: '' },
  })

  const mutation = useMutation({
    mutationFn: async (values: CreateClinicResourceFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/clinic/clinics/{clinicId}/resources', {
        params: { path: { clinicId } },
        body: values,
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      form.reset({ type: 'ROOM', name: '' })
      queryClient.invalidateQueries({ queryKey: ['clinic', 'resources', clinicId] })
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
            <AlertDescription>{t('clinicAdmin.errors.generic')}</AlertDescription>
          </Alert>
        )}
        <FormField
          control={form.control}
          name="type"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('clinicAdmin.resources.typeLabel')}</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="ROOM">{t('clinicAdmin.resources.types.room')}</SelectItem>
                  <SelectItem value="EQUIPMENT">
                    {t('clinicAdmin.resources.types.equipment')}
                  </SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="name"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('clinicAdmin.resources.nameLabel')}</FormLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending
            ? t('clinicAdmin.resources.submitting')
            : t('clinicAdmin.resources.submit')}
        </Button>
      </form>
    </Form>
  )
}
