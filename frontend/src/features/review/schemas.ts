import { z } from 'zod'
import type { TFunction } from 'i18next'

// Mirrors backend/.../review/dto/SubmitReviewRequest.java (@Min(1) @Max(5)).
export function createSubmitReviewSchema(t: TFunction) {
  return z.object({
    rating: z.number().min(1, t('review.form.errors.ratingRequired')).max(5),
    comment: z.string().optional(),
  })
}

export type SubmitReviewFormValues = z.infer<ReturnType<typeof createSubmitReviewSchema>>
