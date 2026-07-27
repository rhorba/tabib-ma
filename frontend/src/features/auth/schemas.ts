import { z } from 'zod'
import type { TFunction } from 'i18next'

// Mirrors backend/.../identity/dto/LoginRequest.java and RegisterRequest.java —
// keep constraints in sync with those @Valid annotations.
export function createLoginSchema(t: TFunction) {
  return z.object({
    email: z.string().email(t('auth.validation.emailInvalid')),
    password: z.string().min(1, t('auth.validation.passwordRequired')),
  })
}

export function createRegisterSchema(t: TFunction) {
  return z.object({
    email: z.string().email(t('auth.validation.emailInvalid')),
    password: z.string().min(8, t('auth.validation.passwordMinLength')),
    firstName: z.string().min(1, t('auth.validation.firstNameRequired')),
    lastName: z.string().min(1, t('auth.validation.lastNameRequired')),
    role: z.enum(['PATIENT', 'DOCTOR'], { message: t('auth.validation.roleRequired') }),
  })
}

export type LoginFormValues = z.infer<ReturnType<typeof createLoginSchema>>
export type RegisterFormValues = z.infer<ReturnType<typeof createRegisterSchema>>
