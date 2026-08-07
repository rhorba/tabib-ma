// Story 3.1 (docs/stories-tabib-ma.md) — k6 load test for PRD NFR-1: doctor search p95 < 1.5s,
// at the System Design capacity estimate's ~20 RPS burst ceiling (docs/system-design-tabib-ma.md
// §Capacity, docs/test-strategy-tabib-ma.md §7 SCALABILITY/PERFORMANCE).
//
// Prerequisites:
//   1. Backend running and reachable at BASE_URL (defaults to the dev docker-compose port).
//   2. A large doctor dataset seeded — run seed-doctors.sql against the dev DB first:
//        docker compose exec -T db psql -U tabibma -d tabibma < load-tests/seed-doctors.sql
//
// Run: docker run --rm -i --network host -e BASE_URL=http://localhost:8090 grafana/k6 run - < load-tests/search.js
// (--network host only needed on Linux; on Windows/Mac Docker Desktop, host.docker.internal
// resolves the host automatically, so BASE_URL=http://host.docker.internal:8090 works without it.)

import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8090'

const SPECIALTIES = [
  'Cardiologie', 'Dermatologie', 'Pediatrie', 'Gynecologie', 'Neurologie',
  'Psychiatrie', 'Rhumatologie', 'Endocrinologie', 'Gastroenterologie', 'Ophtalmologie',
]
const CITIES = [
  'Rabat', 'Casablanca', 'Marrakech', 'Fes', 'Tanger',
  'Agadir', 'Meknes', 'Oujda', 'Kenitra', 'Tetouan',
]

export const options = {
  scenarios: {
    // "20 RPS sustained" per the Test Strategy doc's SCALABILITY target — constant-arrival-rate
    // decouples request rate from response latency, unlike a plain VU-count executor where a
    // slow endpoint would silently reduce the actual RPS instead of surfacing degradation.
    sustained_search_load: {
      executor: 'constant-arrival-rate',
      rate: 20,
      timeUnit: '1s',
      duration: '1m',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<1500'], // PRD NFR-1
    http_req_failed: ['rate<0.01'],
  },
}

export function setup() {
  const email = `k6-loadtest-patient-${Date.now()}@example.com`
  const password = 'correcthorsebattery'
  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({ email, password, role: 'PATIENT', firstName: 'K6', lastName: 'Load' }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  if (registerRes.status !== 201) {
    throw new Error(`setup: registration failed (${registerRes.status}): ${registerRes.body}`)
  }
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  if (loginRes.status !== 200) {
    throw new Error(`setup: login failed (${loginRes.status}): ${loginRes.body}`)
  }
  return { token: loginRes.json('accessToken') }
}

export default function (data) {
  const specialty = SPECIALTIES[Math.floor(Math.random() * SPECIALTIES.length)]
  const city = CITIES[Math.floor(Math.random() * CITIES.length)]
  const res = http.get(
    `${BASE_URL}/api/v1/clinic/doctor-profiles/search?specialty=${specialty}&city=${city}&page=0&size=20`,
    { headers: { Authorization: `Bearer ${data.token}` } },
  )
  check(res, {
    'status is 200': (r) => r.status === 200,
  })
}
