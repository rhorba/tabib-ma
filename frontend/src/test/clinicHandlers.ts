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

let profiles: FakeDoctorProfile[] = []
let documents: FakeDocument[] = []
let nextProfileId = 1
let nextDocumentId = 1

export function resetClinicState() {
  profiles = []
  documents = []
  nextProfileId = 1
  nextDocumentId = 1
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
]

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
