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
import { getApiErrorCode } from '@/shared/api/errors'
import { useAuth } from '../AuthContext'
import { createLoginSchema, type LoginFormValues } from '../schemas'

export function LoginForm({ onSuccess }: { onSuccess: () => void }) {
  const { t } = useTranslation()
  const { login } = useAuth()

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(createLoginSchema(t)),
    defaultValues: { email: '', password: '' },
  })

  const mutation = useMutation({
    mutationFn: (values: LoginFormValues) => login(values.email, values.password),
    onSuccess,
  })

  const errorCode = getApiErrorCode(mutation.error)
  const errorMessage =
    mutation.isError &&
    (errorCode === 'UNAUTHORIZED'
      ? t('auth.errors.invalidCredentials')
      : t('auth.errors.generic'))

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
              <FormLabel>{t('auth.login.emailLabel')}</FormLabel>
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
              <FormLabel>{t('auth.login.passwordLabel')}</FormLabel>
              <FormControl>
                <Input type="password" autoComplete="current-password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending ? t('auth.login.submitting') : t('auth.login.submit')}
        </Button>
      </form>
    </Form>
  )
}
