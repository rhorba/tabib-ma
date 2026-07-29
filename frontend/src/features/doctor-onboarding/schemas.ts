import { z } from 'zod'
import type { TFunction } from 'i18next'

const ALLOWED_DOCUMENT_TYPES = ['application/pdf', 'image/png', 'image/jpeg']
const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024

// Mirrors backend/.../clinic/dto/CreateDoctorProfileRequest.java.
export function createDoctorProfileSchema(t: TFunction) {
  return z.object({
    specialty: z.string().min(1, t('doctorOnboarding.validation.specialtyRequired')),
    bio: z.string().optional(),
    consultationFeeMad: z
      .number({ message: t('doctorOnboarding.validation.feeRequired') })
      .positive(t('doctorOnboarding.validation.feePositive')),
    city: z.string().min(1, t('doctorOnboarding.validation.cityRequired')),
  })
}

// Mirrors DoctorOnboardingService.ALLOWED_CONTENT_TYPES / MAX_DOCUMENT_BYTES.
export function createDocumentUploadSchema(t: TFunction) {
  return z.object({
    documentType: z.enum(['MEDICAL_LICENSE', 'ID_CARD', 'DIPLOMA'], {
      message: t('doctorOnboarding.validation.documentTypeRequired'),
    }),
    file: z
      .instanceof(File, { message: t('doctorOnboarding.validation.fileRequired') })
      .refine((file) => file.size > 0, t('doctorOnboarding.validation.fileRequired'))
      .refine((file) => file.size <= MAX_DOCUMENT_BYTES, t('doctorOnboarding.validation.fileTooLarge'))
      .refine(
        (file) => ALLOWED_DOCUMENT_TYPES.includes(file.type),
        t('doctorOnboarding.validation.fileTypeInvalid')
      ),
  })
}

export type DoctorProfileFormValues = z.infer<ReturnType<typeof createDoctorProfileSchema>>
export type DocumentUploadFormValues = z.infer<ReturnType<typeof createDocumentUploadSchema>>
