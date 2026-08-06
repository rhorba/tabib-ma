import { z } from 'zod'
import type { TFunction } from 'i18next'

// Mirrors backend/.../admin/dto/CreateDisputeRequest.java. NO_SHOW is deliberately not offered
// here — DisputeService.report() rejects it (system-generated only, Story 10.1 Batch 2).
export function createReportDisputeSchema(t: TFunction) {
  return z.object({
    type: z.enum(['PAYMENT_ISSUE', 'COMPLAINT'], { message: t('dispute.report.validation.typeRequired') }),
    reason: z.string().min(1, t('dispute.report.validation.reasonRequired')),
  })
}

export type ReportDisputeFormValues = z.infer<ReturnType<typeof createReportDisputeSchema>>
