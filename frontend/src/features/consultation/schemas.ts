import { z } from 'zod'
import type { TFunction } from 'i18next'

// Mirrors backend/.../consultation/dto/CompleteConsultationRequest.java + PrescriptionItemRequest.
export function createCompleteConsultationSchema(t: TFunction) {
  return z.object({
    items: z
      .array(
        z.object({
          medicationName: z.string().min(1, t('consultation.complete.errors.medicationRequired')),
          dosage: z.string().min(1, t('consultation.complete.errors.dosageRequired')),
          instructions: z.string().optional(),
        }),
      )
      .min(1, t('consultation.complete.errors.itemsRequired')),
  })
}

export type CompleteConsultationFormValues = z.infer<ReturnType<typeof createCompleteConsultationSchema>>
