import { http, HttpResponse } from 'msw'
import { findUserById, getAuthenticatedUser } from './authHandlers'
import { getReviewSummaryFor } from './reviewHandlers'

const RECENT_REVIEWS_LIMIT = 5

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
type FakeDashboard = {
  clinicId: string
  bookingVolume: number
  revenueMad: number
}
type FakeResource = {
  id: string
  clinicId: string
  type: 'ROOM' | 'EQUIPMENT'
  name: string
  active: boolean
}
type FakeMembership = {
  doctorProfileId: string
  clinicId: string
}
type FakeAllocation = {
  resourceId: string
  appointmentId: string
  startsAt: string
  endsAt: string
}

let profiles: FakeDoctorProfile[] = []
let documents: FakeDocument[] = []
let clinics: FakeClinic[] = []
let invitations: FakeInvitation[] = []
let dashboards: FakeDashboard[] = []
let resources: FakeResource[] = []
let memberships: FakeMembership[] = []
let allocations: FakeAllocation[] = []
let nextProfileId = 1
let nextDocumentId = 1
let nextClinicId = 1
let nextInvitationId = 1
let nextResourceId = 1

export function resetClinicState() {
  profiles = []
  documents = []
  clinics = []
  invitations = []
  dashboards = []
  resources = []
  memberships = []
  allocations = []
  nextProfileId = 1
  nextDocumentId = 1
  nextClinicId = 1
  nextInvitationId = 1
  nextResourceId = 1
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

function toSearchResultResponse(profile: FakeDoctorProfile) {
  const owner = findUserById(profile.userId)
  return {
    doctorProfileId: profile.id,
    firstName: owner?.firstName ?? null,
    lastName: owner?.lastName ?? null,
    specialty: profile.specialty,
    city: profile.city,
    consultationFeeMad: profile.consultationFeeMad,
  }
}

function toPublicProfileResponse(profile: FakeDoctorProfile) {
  const owner = findUserById(profile.userId)
  const reviews = getReviewSummaryFor(profile.id, RECENT_REVIEWS_LIMIT)
  return {
    doctorProfileId: profile.id,
    firstName: owner?.firstName ?? null,
    lastName: owner?.lastName ?? null,
    specialty: profile.specialty,
    city: profile.city,
    bio: profile.bio ?? null,
    consultationFeeMad: profile.consultationFeeMad,
    averageRating: reviews.averageRating,
    reviewCount: reviews.reviewCount,
    recentReviews: reviews.recentReviews,
  }
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

function toResourceResponse(resource: FakeResource) {
  const { id, clinicId, type, name, active } = resource
  return { id, clinicId, type, name, active }
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

  http.get(/\/api\/v1\/clinic\/doctor-profiles\/search$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const url = new URL(request.url)
    const specialty = url.searchParams.get('specialty')
    const city = url.searchParams.get('city')
    const maxFeeMad = url.searchParams.get('maxFeeMad')
    const results = profiles
      .filter((p) => p.verificationStatus === 'APPROVED')
      .filter((p) => !specialty || p.specialty.toLowerCase() === specialty.toLowerCase())
      .filter((p) => !city || p.city.toLowerCase() === city.toLowerCase())
      .filter((p) => !maxFeeMad || p.consultationFeeMad <= Number(maxFeeMad))
      .map((p) => toSearchResultResponse(p))
    return HttpResponse.json({ results, page: 0, size: 20, totalElements: results.length, totalPages: 1 })
  }),

  http.get(/\/api\/v1\/clinic\/doctor-profiles\/[^/]+\/public$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const doctorProfileId = pathSegment(request, 2)
    const profile = profiles.find((p) => p.id === doctorProfileId && p.verificationStatus === 'APPROVED')
    if (!profile) {
      return errorResponse(404, 'NOT_FOUND', 'Doctor profile not found.')
    }
    return HttpResponse.json(toPublicProfileResponse(profile))
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

  http.get(/\/api\/v1\/clinic\/clinics\/dashboard$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinic = clinics.find((c) => c.adminUserId === user.id)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', "You don't have a clinic yet.")
    }
    const dashboard = dashboards.find((d) => d.clinicId === clinic.id)
    return HttpResponse.json({
      bookingVolume: dashboard?.bookingVolume ?? 0,
      revenueMad: dashboard?.revenueMad ?? 0,
    })
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

  http.post(/\/api\/v1\/clinic\/clinics\/[^/]+\/resources$/, async ({ request }) => {
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
    const body = (await request.json()) as { type: 'ROOM' | 'EQUIPMENT'; name: string }
    const resource: FakeResource = {
      id: String(nextResourceId++),
      clinicId,
      active: true,
      ...body,
    }
    resources.push(resource)
    return HttpResponse.json(toResourceResponse(resource), { status: 201 })
  }),

  http.get(/\/api\/v1\/clinic\/clinics\/[^/]+\/resources$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinicId = pathSegment(request, 2)
    const clinic = clinics.find((c) => c.id === clinicId)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', 'Clinic not found.')
    }
    if (clinic.adminUserId === user.id) {
      return HttpResponse.json(resources.filter((r) => r.clinicId === clinicId).map(toResourceResponse))
    }
    // Mirrors ClinicResourceService.listResources: a clinic's own doctor-staff can also list
    // resources, but only active ones — the admin still sees everything.
    const profile = profiles.find((p) => p.userId === user.id)
    const isStaffDoctor =
      profile != null && memberships.some((m) => m.doctorProfileId === profile.id && m.clinicId === clinicId)
    if (!isStaffDoctor) {
      return errorResponse(403, 'FORBIDDEN', "You don't have access to this clinic's resources.")
    }
    return HttpResponse.json(
      resources.filter((r) => r.clinicId === clinicId && r.active).map(toResourceResponse)
    )
  }),

  http.get(/\/api\/v1\/clinic\/doctor-profiles\/me\/clinics$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const profile = profiles.find((p) => p.userId === user.id)
    if (!profile) {
      return errorResponse(404, 'NOT_FOUND', "You don't have a doctor profile yet.")
    }
    const clinicIds = memberships.filter((m) => m.doctorProfileId === profile.id).map((m) => m.clinicId)
    return HttpResponse.json(clinics.filter((c) => clinicIds.includes(c.id)).map(toClinicResponse))
  }),

  http.get(/\/api\/v1\/clinic\/clinics\/resources\/utilization$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinic = clinics.find((c) => c.adminUserId === user.id)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', "You don't have a clinic yet.")
    }
    return HttpResponse.json(
      resources
        .filter((r) => r.clinicId === clinic.id)
        .map((r) => ({
          resourceId: r.id,
          resourceName: r.name,
          type: r.type,
          active: r.active,
          allocations: allocations
            .filter((a) => a.resourceId === r.id)
            .map(({ appointmentId, startsAt, endsAt }) => ({ appointmentId, startsAt, endsAt })),
        }))
    )
  }),

  http.patch(/\/api\/v1\/clinic\/clinics\/[^/]+\/resources\/[^/]+\/deactivate$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    const clinicId = pathSegment(request, 4)
    const resourceId = pathSegment(request, 2)
    const clinic = clinics.find((c) => c.id === clinicId)
    if (!clinic) {
      return errorResponse(404, 'NOT_FOUND', 'Clinic not found.')
    }
    if (clinic.adminUserId !== user.id) {
      return errorResponse(403, 'FORBIDDEN', 'You can only manage your own clinic.')
    }
    const resource = resources.find((r) => r.id === resourceId && r.clinicId === clinicId)
    if (!resource) {
      return errorResponse(404, 'NOT_FOUND', 'Resource not found.')
    }
    resource.active = false
    return HttpResponse.json(toResourceResponse(resource))
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
  const acceptingProfile = profiles.find((p) => p.userId === user.id)
  if (status === 'ACCEPTED' && !acceptingProfile) {
    return errorResponse(409, 'CONFLICT', 'You need to create your doctor profile before joining a clinic.')
  }
  if (status === 'ACCEPTED' && acceptingProfile) {
    if (!memberships.some((m) => m.doctorProfileId === acceptingProfile.id && m.clinicId === invitation.clinicId)) {
      memberships.push({ doctorProfileId: acceptingProfile.id, clinicId: invitation.clinicId })
    }
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

// Shared with bookingHandlers.ts so the availability fakes can resolve "my
// doctor profile" the same way the real backend's AvailabilityService does
// (DoctorProfileRepository.findByUserId).
export function findDoctorProfileByUserId(userId: string) {
  return profiles.find((p) => p.userId === userId) ?? null
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

// Test-only helper: lets tests seed the clinic-wide dashboard aggregate
// directly, without seeding the underlying appointments/payments the real
// ClinicDashboardService derives it from.
export function seedClinicDashboard(dashboard: FakeDashboard) {
  dashboards.push(dashboard)
  return dashboard
}

// Test-only helper: lets tests seed a clinic resource directly without going
// through the create-resource UI first (e.g. to test ClinicResourceList's
// deactivate flow in isolation).
export function seedClinicResource(resource: Omit<FakeResource, 'id'>) {
  const fakeResource: FakeResource = { id: String(nextResourceId++), ...resource }
  resources.push(fakeResource)
  return fakeResource
}

// Test-only helper: lets tests seed a doctor's clinic-staff membership directly,
// without going through the invitation-accept flow first (e.g. to test the
// availability-rule form's resource picker in isolation).
export function seedClinicStaffMembership(membership: FakeMembership) {
  memberships.push(membership)
  return membership
}

// Test-only helper: lets tests seed a resource allocation directly, without
// seeding a real appointment/payment (e.g. to test ResourceUtilizationView).
export function seedResourceAllocation(allocation: FakeAllocation) {
  allocations.push(allocation)
  return allocation
}
