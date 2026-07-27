import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
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
import { getApiErrorCode } from '@/shared/api/errors'
import { useAuth } from '../AuthContext'
import { createRegisterSchema, type RegisterFormValues } from '../schemas'

export function RegisterForm({ onSuccess }: { onSuccess: () => void }) {
  const { t } = useTranslation()
  const { register } = useAuth()

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(createRegisterSchema(t)),
    defaultValues: { email: '', password: '', firstName: '', lastName: '', role: 'PATIENT' },
  })

  const mutation = useMutation({
    mutationFn: (values: RegisterFormValues) => register(values),
    onSuccess,
  })

  const errorCode = getApiErrorCode(mutation.error)
  const errorMessage =
    mutation.isError &&
    (errorCode === 'CONFLICT' ? t('auth.errors.emailTaken') : t('auth.errors.generic'))

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
        <div className="grid grid-cols-2 gap-4">
          <FormField
            control={form.control}
            name="firstName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('auth.register.firstNameLabel')}</FormLabel>
                <FormControl>
                  <Input autoComplete="given-name" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <FormField
            control={form.control}
            name="lastName"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('auth.register.lastNameLabel')}</FormLabel>
                <FormControl>
                  <Input autoComplete="family-name" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
        </div>
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('auth.register.emailLabel')}</FormLabel>
              <FormControl>
                <Input type="email" autoComplete="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('auth.register.passwordLabel')}</FormLabel>
              <FormControl>
                <Input type="password" autoComplete="new-password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="role"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('auth.register.roleLabel')}</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="PATIENT">{t('auth.register.rolePatient')}</SelectItem>
                  <SelectItem value="DOCTOR">{t('auth.register.roleDoctor')}</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending ? t('auth.register.submitting') : t('auth.register.submit')}
        </Button>
      </form>
    </Form>
  )
}
