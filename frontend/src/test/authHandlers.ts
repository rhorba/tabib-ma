import { http, HttpResponse } from 'msw'

// A tiny in-memory fake of the real backend's auth contract (register / login /
// refresh / me), close enough to exercise the frontend's real request/response
// handling — not a reimplementation of AuthService's business rules.
type FakeUser = {
  id: string
  email: string
  password: string
  role: 'PATIENT' | 'DOCTOR'
  firstName: string
  lastName: string
}

let users: FakeUser[] = []
let nextId = 1
// token -> email
let accessTokens = new Map<string, string>()
let refreshTokens = new Map<string, string>()

export function resetAuthState() {
  users = []
  nextId = 1
  accessTokens = new Map()
  refreshTokens = new Map()
}

function errorResponse(status: number, code: string, message: string) {
  return HttpResponse.json(
    { error: { code, message, details: [], requestId: 'test-request-id' } },
    { status }
  )
}

function issueTokenPair(email: string) {
  const accessToken = `access-${email}-${Math.random()}`
  const refreshToken = `refresh-${email}-${Math.random()}`
  accessTokens.set(accessToken, email)
  refreshTokens.set(refreshToken, email)
  return { accessToken, refreshToken, expiresInMs: 900000 }
}

function toSummary(user: FakeUser) {
  return {
    id: String(user.id),
    email: user.email,
    role: user.role,
    firstName: user.firstName,
    lastName: user.lastName,
  }
}

export const authHandlers = [
  http.post(/\/api\/v1\/auth\/register$/, async ({ request }) => {
    const body = (await request.json()) as {
      email: string
      password: string
      role: 'PATIENT' | 'DOCTOR'
      firstName: string
      lastName: string
    }
    if (users.some((u) => u.email === body.email)) {
      return errorResponse(409, 'CONFLICT', 'An account with this email already exists.')
    }
    const user: FakeUser = { id: String(nextId++), ...body }
    users.push(user)
    return HttpResponse.json(toSummary(user), { status: 201 })
  }),

  http.post(/\/api\/v1\/auth\/login$/, async ({ request }) => {
    const body = (await request.json()) as { email: string; password: string }
    const user = users.find((u) => u.email === body.email && u.password === body.password)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Invalid email or password.')
    }
    return HttpResponse.json(issueTokenPair(user.email))
  }),

  http.post(/\/api\/v1\/auth\/refresh$/, async ({ request }) => {
    const body = (await request.json()) as { refreshToken: string }
    const email = refreshTokens.get(body.refreshToken)
    if (!email) {
      return errorResponse(401, 'UNAUTHORIZED', 'Invalid refresh token.')
    }
    refreshTokens.delete(body.refreshToken)
    return HttpResponse.json(issueTokenPair(email))
  }),

  http.get(/\/api\/v1\/users\/me$/, ({ request }) => {
    const auth = request.headers.get('Authorization')
    const token = auth?.startsWith('Bearer ') ? auth.slice('Bearer '.length) : null
    const email = token ? accessTokens.get(token) : null
    const user = users.find((u) => u.email === email)
    if (!user) {
      return errorResponse(401, 'UNAUTHORIZED', 'Authentication is required.')
    }
    return HttpResponse.json(toSummary(user))
  }),
]
