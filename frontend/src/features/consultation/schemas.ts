import { z } from 'zod'
import type { TFunction } from 'i18next'

// Mirrors backend/.../consultation/dto/CompleteConsultationRequest.java + PrescriptionItemRequest.
// items itself is allowed to be empty (Story 6.3, amended 2026-08-07) — the "skip prescription"
// action in CompleteConsultationForm submits an empty array directly, bypassing this schema
// entirely; this validation only runs for the "complete with prescription" submit path, where the
// UI never lets the array shrink below one item, so per-item validation is what actually matters.
export function createCompleteConsultationSchema(t: TFunction) {
  return z.object({
    items: z.array(
      z.object({
        medicationName: z.string().min(1, t('consultation.complete.errors.medicationRequired')),
        dosage: z.string().min(1, t('consultation.complete.errors.dosageRequired')),
        instructions: z.string().optional(),
      }),
    ),
  })
}

export type CompleteConsultationFormValues = z.infer<ReturnType<typeof createCompleteConsultationSchema>>
