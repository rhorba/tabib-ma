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
import { getApiErrorCode } from '@/shared/api/errors'
import { createDocumentUploadSchema, type DocumentUploadFormValues } from '../schemas'

export function DocumentUploadForm({ doctorProfileId }: { doctorProfileId: string }) {
  const { t } = useTranslation()
  const queryClient = useQueryClient()

  const form = useForm<DocumentUploadFormValues>({
    resolver: zodResolver(createDocumentUploadSchema(t)),
    defaultValues: { documentType: 'MEDICAL_LICENSE' },
  })

  const mutation = useMutation({
    mutationFn: async (values: DocumentUploadFormValues) => {
      const formData = new FormData()
      formData.append('file', values.file)
      const { data, error } = await apiClient.POST(
        '/api/v1/clinic/doctor-profiles/{doctorProfileId}/documents',
        {
          params: { path: { doctorProfileId }, query: { documentType: values.documentType } },
          // The generated schema types this as a JSON body (springdoc can't
          // describe multipart precisely); openapi-fetch passes FormData
          // through untouched and lets the browser set the boundary header.
          body: formData as unknown as { file: string },
        }
      )
      if (error) {
        throw error
      }
      return data
    },
    onSuccess: () => {
      form.reset({ documentType: form.getValues('documentType') })
      queryClient.invalidateQueries({ queryKey: ['doctor-profile', 'documents', doctorProfileId] })
    },
  })

  const errorCode = getApiErrorCode(mutation.error)
  const errorMessage =
    mutation.isError &&
    (errorCode === 'VALIDATION_FAILED'
      ? t('doctorOnboarding.errors.fileTypeInvalid')
      : t('doctorOnboarding.errors.generic'))

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
          name="documentType"
          render={({ field }) => (
            <FormItem>
              <FormLabel>{t('doctorOnboarding.documents.typeLabel')}</FormLabel>
              <Select value={field.value} onValueChange={field.onChange}>
                <FormControl>
                  <SelectTrigger className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                </FormControl>
                <SelectContent>
                  <SelectItem value="MEDICAL_LICENSE">
                    {t('doctorOnboarding.documents.typeMedicalLicense')}
                  </SelectItem>
                  <SelectItem value="ID_CARD">{t('doctorOnboarding.documents.typeIdCard')}</SelectItem>
                  <SelectItem value="DIPLOMA">{t('doctorOnboarding.documents.typeDiploma')}</SelectItem>
                </SelectContent>
              </Select>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="file"
          render={({ field: { onChange, value: _value, ...field } }) => (
            <FormItem>
              <FormLabel>{t('doctorOnboarding.documents.fileLabel')}</FormLabel>
              <FormControl>
                <Input
                  type="file"
                  accept="application/pdf,image/png,image/jpeg"
                  onChange={(e) => onChange(e.target.files?.[0])}
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" disabled={mutation.isPending} className="mt-2">
          {mutation.isPending
            ? t('doctorOnboarding.documents.uploading')
            : t('doctorOnboarding.documents.upload')}
        </Button>
      </form>
    </Form>
  )
}
