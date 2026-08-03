import { http, HttpResponse } from 'msw'
import { getAuthenticatedUser, findUserById } from './authHandlers'

// A tiny in-memory fake of the review module's contract (submit + mine) — close enough
// to exercise the frontend's request/response handling, not a reimplementation of
// ReviewService's ownership/completed-status checks (those are covered by the backend's
// own ReviewControllerIntegrationTest).
type FakeReview = {
  id: string
  appointmentId: string
  patientId: string
  doctorProfileId: string
  rating: number
  comment?: string
  createdAt: string
}

let reviews: FakeReview[] = []
let nextId = 1

export function resetReviewState() {
  reviews = []
  nextId = 1
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message, details: [], requestId: 'test-request-id' } },
    { status }
  )
}

function toReviewResponse(review: FakeReview) {
  const { id, appointmentId, doctorProfileId, rating, comment, createdAt } = review
  return { id, appointmentId, doctorProfileId, rating, comment: comment ?? null, createdAt }
}

export const reviewHandlers = [
  http.post(/\/api\/v1\/reviews$/, async ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    if (user.role !== 'PATIENT') return errorResponse(403, 'FORBIDDEN', 'Only patients can submit reviews.')
    const body = (await request.json()) as { appointmentId: string; doctorProfileId?: string; rating: number; comment?: string }
    if (reviews.some((r) => r.appointmentId === body.appointmentId)) {
      return errorResponse(409, 'CONFLICT', 'This appointment has already been reviewed.')
    }
    const review: FakeReview = {
      id: String(nextId++),
      appointmentId: body.appointmentId,
      patientId: user.id,
      doctorProfileId: body.doctorProfileId ?? 'doc-1',
      rating: body.rating,
      comment: body.comment,
      createdAt: new Date().toISOString(),
    }
    reviews.push(review)
    return HttpResponse.json(toReviewResponse(review), { status: 201 })
  }),

  http.get(/\/api\/v1\/reviews\/mine$/, ({ request }) => {
    const user = getAuthenticatedUser(request)
    if (!user) return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    return HttpResponse.json(reviews.filter((r) => r.patientId === user.id).map(toReviewResponse))
  }),
]

// Test-only helper: lets tests seed a review directly (e.g. to test the "already
// reviewed" state on MyAppointmentsPage without going through submission first).
export function seedReview(review: Omit<FakeReview, 'id' | 'createdAt'> & { createdAt?: string }) {
  const fake: FakeReview = { id: String(nextId++), createdAt: new Date().toISOString(), ...review }
  reviews.push(fake)
  return fake
}

// Consumed by clinicHandlers.ts's /public endpoint fake, mirroring the real backend's
// review.PublicDoctorProfileController composing clinic + review data.
export function getReviewSummaryFor(doctorProfileId: string, recentLimit: number) {
  const forDoctor = reviews
    .filter((r) => r.doctorProfileId === doctorProfileId)
    .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
  if (forDoctor.length === 0) {
    return { averageRating: null as number | null, reviewCount: 0, recentReviews: [] as ReturnType<typeof toEntry>[] }
  }
  const averageRating = forDoctor.reduce((sum, r) => sum + r.rating, 0) / forDoctor.length
  const recentReviews = forDoctor.slice(0, recentLimit).map(toEntry)
  return { averageRating, reviewCount: forDoctor.length, recentReviews }
}

function toEntry(review: FakeReview) {
  const patient = findUserById(review.patientId)
  return {
    patientFirstName: patient?.firstName ?? null,
    rating: review.rating,
    comment: review.comment ?? null,
    createdAt: review.createdAt,
  }
}
