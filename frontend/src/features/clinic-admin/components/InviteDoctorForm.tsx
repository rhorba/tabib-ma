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
import { inviteDoctorSchema, type InviteDoctorFormValues } from '../schemas'

export function InviteDoctorForm({ clinicId }: { clinicId: string }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<InviteDoctorFormValues>({
    resolver: zodResolver(inviteDoctorSchema(t)),
    defaultValues: { email: '' },
  })

  const mutation = useMutation({
    mutationFn: async (values: InviteDoctorFormValues) => {
      const { data, error } = await apiClient.POST('/api/v1/clinic/clinics/{clinicId}/invitations', {
        params: { path: { clinicId } },
        body: values,
      })
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      form.reset({ email: '' })
      queryClient.invalidateQueries({ queryKey: ['clinic', 'invitations', clinicId] })
    },
  })

  const errorCode = getApiErrorCode(mutation.error)
  const errorMessage =
    mutation.isError &&
    (errorCode === 'CONFLICT'
      ? t('clinicAdmin.errors.alreadyInvited')
      : t('clinicAdmin.errors.generic'))

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
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('clinicAdmin.invitations.emailLabel')}</FormLabel>
              <FormControl>
                <Input type="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending
            ? t('clinicAdmin.invitations.inviting')
            : t('clinicAdmin.invitations.invite')}
        </Button>
      </form>
    </Form>
  )
}
