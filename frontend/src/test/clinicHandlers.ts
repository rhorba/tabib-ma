import { http, HttpResponse } from 'msw'
import { getAuthenticatedUser } from './authHandlers'

// A tiny in-memory fake of the clinic module's doctor-profile/verification-queue
// contract — close enough to exercise the frontend's request/response handling,
// not a reimplementation of DoctorOnboardingService/VerificationReviewService's
// business rules (those are covered by the backend's own tests).
type FakeDoctorProfile = {
  id: string
  userId: string
  specialty: string
  bio?: string
  consultationFeeMad: number
  city: string
  verificationStatus: 'PENDING' | 'APPROVED' | 'REJECTED'
}
type FakeDocument = {
  id: string
  doctorProfileId: string
  documentType: string
  createdAt: string
}
type FakeClinic = {
  id: string
  adminUserId: string
  name: string
  city: string
  address?: string
}
type FakeInvitation = {
  id: string
  clinicId: string
  invitedEmail: string
  status: 'PENDING' | 'ACCEPTED' | 'DECLINED'
  createdAt: string
}

let profiles: FakeDoctorProfile[] = []
let documents: FakeDocument[] = []
let clinics: FakeClinic[] = []
let invitations: FakeInvitation[] = []
let nextProfileId = 1
let nextDocumentId = 1
let nextClinicId = 1
let nextInvitationId = 1

export function resetClinicState() {
  profiles = []
  documents = []
  clinics = []
  invitations = []
  nextProfileId = 1
  nextDocumentId = 1
  nextClinicId = 1
  nextInvitationId = 1
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message, details: [], requestId: 'test-request-id' } },
    { status }
  )
}

function toProfileResponse(profile: FakeDoctorProfile) {
  const { id, userId, specialty, bio, consultationFeeMad, verificationStatus, city } = profile
  return { id, userId, specialty, bio, consultationFeeMad, verificationStatus, city }
}

function toDocumentResponse(document: FakeDocument) {
  const { id, doctorProfileId, documentType, createdAt } = document
  return { id, doctorProfileId, documentType, createdAt }
}

function toClinicResponse(clinic: FakeClinic) {
  const { id, adminUserId, name, city, address } = clinic
  return { id, adminUserId, name, city, address }
}

function toInvitationResponse(invitation: FakeInvitation, clinicName?: string) {
  const { id, clinicId, invitedEmail, status, createdAt } = invitation
  return { id, clinicId, clinicName: clinicName ?? null, invitedEmail, status, createdAt }
}

function pathSegment(request: Request, indexFromEnd: number) {
  const segments = new URL(request.url).pathname.split('/').filter(Boolean)
  return segments[segments.length - indexFromEnd]
}

export const clinicHandlers = [
  http.post(/\/api\/v1\/clinic\/doctor-profiles$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    if (profiles.some((p) => p.userId === user.id)) {
      return errorResponse(409, 'CONFLICT', 'A doctor profile already exists for this account.')
    }
    const body = (await request.json()) as {
      specialty: string
      bio?: string
      consultationFeeMad: number
      city: string
    }
    const profile: FakeDoctorProfile = {
      id: String(nextProfileId++),
      userId: user.id,
      verificationStatus: 'PENDING',
      ...body,
    }
    profiles.push(profile)
    return HttpResponse.json(toProfileResponse(profile), { status: 201 })
  }),

  http.get(/\/api\/v1\/clinic\/doctor-profiles\/me$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const profile = profiles.find((p) => p.userId === user.id)
    if (!profile) {
      return errorResponse(404, 'NOT_FOUND', "You don't have a doctor profile yet.")
    }
    return HttpResponse.json(toProfileResponse(profile))
  }),

  http.post(/\/api\/v1\/clinic\/doctor-profiles\/[^/]+\/documents$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const doctorProfileId = pathSegment(request, 2)
    const profile = profiles.find((p) => p.id === doctorProfileId)
    if (!profile) {
      return errorResponse(404, 'NOT_FOUND', 'Doctor profile not found.')
    }
    if (profile.userId !== user.id) {
      return errorResponse(403, 'FORBIDDEN', 'You can only upload documents to your own doctor profile.')
    }
    const documentType = new URL(request.url).searchParams.get('documentType') ?? 'MEDICAL_LICENSE'
    const formData = await request.formData()
    const file = formData.get('file') as File | null
    if (!file || !['application/pdf', 'image/png', 'image/jpeg'].includes(file.type)) {
      return errorResponse(400, 'VALIDATION_FAILED', 'Only PDF, PNG, and JPEG documents are accepted.')
    }
    const document: FakeDocument = {
      id: String(nextDocumentId++),
      doctorProfileId,
      documentType,
      createdAt: new Date().toISOString(),
    }
    documents.push(document)
    return HttpResponse.json(toDocumentResponse(document), { status: 201 })
  }),

  http.get(/\/api\/v1\/clinic\/doctor-profiles\/[^/]+\/documents$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const doctorProfileId = pathSegment(request, 2)
    const profile = profiles.find((p) => p.id === doctorProfileId)
    if (!profile) {
      return errorResponse(404, 'NOT_FOUND', 'Doctor profile not found.')
    }
    if (profile.userId !== user.id) {
      return errorResponse(403, 'FORBIDDEN', 'You can only view documents on your own doctor profile.')
    }
    return HttpResponse.json(documents.filter((d) => d.doctorProfileId === doctorProfileId).map(toDocumentResponse))
  }),

  http.get(/\/api\/v1\/admin\/platform\/verification-queue$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    return HttpResponse.json(
      profiles.filter((p) => p.verificationStatus === 'PENDING').map(toProfileResponse)
    )
  }),

  http.get(/\/api\/v1\/admin\/platform\/verification-queue\/[^/]+\/documents$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const doctorProfileId = pathSegment(request, 2)
    return HttpResponse.json(documents.filter((d) => d.doctorProfileId === doctorProfileId).map(toDocumentResponse))
  }),

  http.post(/\/api\/v1\/admin\/platform\/verification-queue\/[^/]+\/approve$/, ({ request }) =>
    decide(request, 'APPROVED')
  ),
  http.post(/\/api\/v1\/admin\/platform\/verification-queue\/[^/]+\/reject$/, ({ request }) =>
    decide(request, 'REJECTED')
  ),

  http.post(/\/api\/v1\/clinic\/clinics$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    if (user.role !== 'CLINIC_ADMIN') {
      return errorResponse(403, 'FORBIDDEN', 'Only clinic admins can create a clinic.')
    }
    if (clinics.some((c) => c.adminUserId === user.id)) {
      return errorResponse(409, 'CONFLICT', 'A clinic already exists for this account.')
    }
    const body = (await request.json()) as { name: string; city: string; address?: string }
    const clinic: FakeClinic = { id: String(nextClinicId++), adminUserId: user.id, ...body }
    clinics.push(clinic)
    return HttpResponse.json(toClinicResponse(clinic), { status: 201 })
  }),

  http.get(/\/api\/v1\/clinic\/clinics\/me$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinic = clinics.find((c) => c.adminUserId === user.id)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', "You don't have a clinic yet.")
    }
    return HttpResponse.json(toClinicResponse(clinic))
  }),

  http.post(/\/api\/v1\/clinic\/clinics\/[^/]+\/invitations$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinicId = pathSegment(request, 2)
    const clinic = clinics.find((c) => c.id === clinicId)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', 'Clinic not found.')
    }
    if (clinic.adminUserId !== user.id) {
      return errorResponse(403, 'FORBIDDEN', 'You can only manage your own clinic.')
    }
    const body = (await request.json()) as { email: string }
    if (
      invitations.some(
        (i) => i.clinicId === clinicId && i.invitedEmail === body.email && i.status === 'PENDING'
      )
    ) {
      return errorResponse(409, 'CONFLICT', 'This email already has a pending invitation for this clinic.')
    }
    const invitation: FakeInvitation = {
      id: String(nextInvitationId++),
      clinicId,
      invitedEmail: body.email,
      status: 'PENDING',
      createdAt: new Date().toISOString(),
    }
    invitations.push(invitation)
    return HttpResponse.json(toInvitationResponse(invitation, clinic.name), { status: 201 })
  }),

  http.get(/\/api\/v1\/clinic\/clinics\/[^/]+\/invitations$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinicId = pathSegment(request, 2)
    const clinic = clinics.find((c) => c.id === clinicId)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', 'Clinic not found.')
    }
    if (clinic.adminUserId !== user.id) {
      return errorResponse(403, 'FORBIDDEN', 'You can only manage your own clinic.')
    }
    return HttpResponse.json(
      invitations.filter((i) => i.clinicId === clinicId).map((i) => toInvitationResponse(i, clinic.name))
    )
  }),

  http.get(/\/api\/v1\/clinic\/invitations\/me$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    return HttpResponse.json(
      invitations
        .filter((i) => i.invitedEmail === user.email && i.status === 'PENDING')
        .map((i) => toInvitationResponse(i, clinics.find((c) => c.id === i.clinicId)?.name))
    )
  }),

  http.post(/\/api\/v1\/clinic\/invitations\/[^/]+\/accept$/, ({ request }) =>
    decideInvitation(request, 'ACCEPTED')
  ),
  http.post(/\/api\/v1\/clinic\/invitations\/[^/]+\/decline$/, ({ request }) =>
    decideInvitation(request, 'DECLINED')
  ),
]

function decideInvitation(request: Request, status: 'ACCEPTED' | 'DECLINED') {
  const user = getAuthenticatedUser(request)
  if (!user) {
    return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
  }
  const invitationId = pathSegment(request, 2)
  const invitation = invitations.find((i) => i.id === invitationId)
  if (!invitation) {
    return errorResponse(404, 'NOT_FOUND', 'Invitation not found.')
  }
  if (invitation.invitedEmail !== user.email) {
    return errorResponse(403, 'FORBIDDEN', 'This invitation was not sent to you.')
  }
  if (invitation.status !== 'PENDING') {
    return errorResponse(409, 'CONFLICT', 'This invitation has already been decided.')
  }
  if (status === 'ACCEPTED' && !profiles.some((p) => p.userId === user.id)) {
    return errorResponse(409, 'CONFLICT', 'You need to create your doctor profile before joining a clinic.')
  }
  invitation.status = status
  return HttpResponse.json(toInvitationResponse(invitation, clinics.find((c) => c.id === invitation.clinicId)?.name))
}

function decide(request: Request, status: 'APPROVED' | 'REJECTED') {
  const user = getAuthenticatedUser(request)
  if (!user) {
    return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
  }
  const doctorProfileId = pathSegment(request, 2)
  const profile = profiles.find((p) => p.id === doctorProfileId)
  if (!profile) {
    return errorResponse(404, 'NOT_FOUND', 'Doctor profile not found.')
  }
  if (profile.verificationStatus !== 'PENDING') {
    return errorResponse(409, 'CONFLICT', 'This doctor profile has already been reviewed.')
  }
  profile.verificationStatus = status
  return HttpResponse.json(toProfileResponse(profile))
}

// Test-only helper mirroring the seed migration: lets tests create a doctor
// profile directly without going through the create-profile UI first.
export function seedDoctorProfile(profile: Omit<FakeDoctorProfile, 'id'>) {
  const fakeProfile: FakeDoctorProfile = { id: String(nextProfileId++), ...profile }
  profiles.push(fakeProfile)
  return fakeProfile
}

// Test-only helper: lets tests seed a clinic directly without going through
// the create-clinic UI first (e.g. to test InviteDoctorForm/invitation lists
// in isolation).
export function seedClinic(clinic: Omit<FakeClinic, 'id'>) {
  const fakeClinic: FakeClinic = { id: String(nextClinicId++), ...clinic }
  clinics.push(fakeClinic)
  return fakeClinic
}

// Test-only helper: lets tests seed a pending invitation directly, mirroring
// seedDoctorProfile's role for the doctor-onboarding side.
export function seedClinicInvitation(invitation: Omit<FakeInvitation, 'id' | 'createdAt' | 'status'>) {
  const fakeInvitation: FakeInvitation = {
    id: String(nextInvitationId++),
    status: 'PENDING',
    createdAt: new Date().toISOString(),
    ...invitation,
  }
  invitations.push(fakeInvitation)
  return fakeInvitation
}
