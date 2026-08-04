import { z } from 'zod'
import type { TFunction } from 'i18next'

// Mirrors backend/.../clinic/dto/CreateClinicRequest.java.
export function createClinicSchema(t: TFunction) {
  return z.object({
    name: z.string().min(1, t('clinicAdmin.validation.nameRequired')),
    city: z.string().min(1, t('clinicAdmin.validation.cityRequired')),
    address: z.string().optional(),
  })
}

// Mirrors backend/.../clinic/dto/InviteDoctorRequest.java.
export function inviteDoctorSchema(t: TFunction) {
  return z.object({
    email: z.string().email(t('clinicAdmin.validation.emailInvalid')),
  })
}

// Mirrors backend/.../clinic/dto/CreateClinicResourceRequest.java.
export function createClinicResourceSchema(t: TFunction) {
  return z.object({
    type: z.enum(['ROOM', 'EQUIPMENT']),
    name: z.string().min(1, t('clinicAdmin.resources.validation.nameRequired')),
  })
}

export type CreateClinicFormValues = z.infer<ReturnType<typeof createClinicSchema>>
export type InviteDoctorFormValues = z.infer<ReturnType<typeof inviteDoctorSchema>>
export type CreateClinicResourceFormValues = z.infer<ReturnType<typeof createClinicResourceSchema>>
