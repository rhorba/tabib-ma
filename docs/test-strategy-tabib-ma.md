# Test Strategy: Tabib.ma
**References**: docs/prd-tabib-ma.md, docs/architecture-tabib-ma.md, docs/security-tabib-ma.md | **Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Test Architect | **Status**: Draft

## 1. Risk-Based Test Strategy

| Component | Failure Impact | Change Freq | Complexity | Risk | Test Level |
|---|---|---|---|---|---|
| Booking (double-booking prevention) | Critical (5) | Medium (3) | High (5) | 13 | Maximum |
| Payment (CMI webhook) | Critical (5) | Low (2) | High (5) | 12 | High/Maximum |
| Auth (JWT, RBAC, ownership checks) | Critical (5) | Low (2) | Medium (3) | 10 | Maximum |
| Video Consultation | High (4) | Medium (3) | High (5) | 12 | High |
| Prescription (integrity/immutability) | Critical (5) | Low (2) | Medium (3) | 10 | Maximum |
| Doctor Verification workflow | Medium (3) | Low (2) | Low (2) | 7 | Standard |
| Search | Medium (3) | Medium (3) | Medium (3) | 9 | High |
| Notifications (SMS/email) | Low (2) | Low (2) | Low (2) | 6 | Standard |
| Clinic Admin dashboard | Low (2) | Low (2) | Medium (3) | 7 | Standard |
| Reviews | Low (1) | Low (2) | Low (1) | 4 | Minimal |

### Coverage Targets
| Risk Level | Unit | Integration | E2E |
|---|---|---|---|
| Maximum (Booking, Auth, Prescription) | 95%+ | All paths incl. adversarial | Happy + error + race conditions |
| High (Payment, Video, Search) | 85%+ | Happy + error | Happy path + key errors |
| Standard (Verification, Notifications, Clinic Admin) | 70%+ | Happy path | Smoke only |
| Minimal (Reviews) | 60%+ | Smoke | None |

Combined project-wide target stays ≥ 80% per the mandatory coverage gate — the weighting above concentrates effort where it matters rather than spreading it evenly.

## 2. ATDD — Acceptance Scenarios (Gherkin)

```gherkin
Feature: Appointment Booking

  Scenario: Successful booking and payment
    Given a patient is logged in
    And a doctor has an open availability slot
    When the patient selects the slot and completes CMI payment
    Then the appointment status should be CONFIRMED
    And the patient should receive a confirmation SMS and email

  Scenario: Concurrent booking race — only one wins
    Given two patients simultaneously attempt to book the same doctor slot
    When both requests are submitted at the same time
    Then exactly one appointment should be CONFIRMED
    And the other patient should see "This slot was just booked"

  Scenario: Payment failure does not confirm the appointment
    Given a patient has selected a slot and been redirected to CMI
    When the CMI payment fails
    Then the appointment status should remain PENDING_PAYMENT or transition to a failed state
    And the slot should become available again after the expiry window

  Scenario: CMI webhook replay is rejected
    Given a CMI webhook has already been processed for a given transaction reference
    When the same webhook payload is received again
    Then the system should not double-process the payment (idempotency key enforced)

Feature: Authorization & Data Access

  Scenario: Doctor cannot view another doctor's appointments
    Given Doctor A and Doctor B both have appointments
    When Doctor A requests Doctor B's appointment by ID
    Then the response should be 403 Forbidden, not the appointment data

  Scenario: Patient cannot access another patient's prescription
    Given Patient A and Patient B each have a prescription
    When Patient A requests Patient B's prescription URL directly
    Then access should be denied

Feature: Video Consultation

  Scenario: Poor connection triggers audio-only suggestion
    Given a consultation is in progress
    When connection quality drops below the defined threshold
    Then the patient/doctor should be offered an audio-only fallback

  Scenario: Join window enforcement
    Given an appointment is scheduled for 14:00
    When a user attempts to join the video room at 13:30
    Then the join button should be disabled until 13:50 (±10min window per UX doc)

Feature: Prescription Integrity

  Scenario: Prescriptions are never edited in place
    Given a signed prescription exists
    When the doctor issues a correction
    Then a new prescription record is created referencing the original (supersedes_id)
    And the original record remains unchanged
```

## 3. Adversarial Review — Priority Targets

Per the risk table above, adversarial review is mandatory for **Booking, Payment, Auth, Prescription**.

```markdown
## Adversarial Review: Booking + Payment
### Race Conditions
- [ ] Double-submit: 2 concurrent booking requests for the same slot (DB EXCLUDE constraint from ADR-4 must reject one)
- [ ] TOCTOU: slot shown as available in UI, taken by the time payment redirect completes
### Auth & Access Abuse
- [ ] Patient A attempts to view/modify Patient B's appointment by guessing/incrementing IDs (IDOR)
- [ ] Expired JWT access token reused after expiry
- [ ] Booking request with a tampered price/fee value in the payload (server must recompute, never trust client)
### Data Integrity
- [ ] CMI webhook replay (same transaction ref sent twice) — must not double-confirm or double-charge
- [ ] Webhook with invalid/missing signature — must be rejected outright
- [ ] Database connection drop mid-booking-transaction — appointment must not end up in an inconsistent state (no CONFIRMED without a matching CONFIRMED payment — Architecture doc invariant)

## Adversarial Review: Auth
### Auth & Access Abuse
- [ ] Access without authentication on every non-public endpoint
- [ ] Privilege escalation: Patient-role JWT modified/replayed against admin endpoints
- [ ] Account lockout bypass (distributed attempts across IPs)
- [ ] Refresh token reuse after rotation (should be detected and all sessions revoked — replay indicates token theft)

## Adversarial Review: Prescription
### Data Integrity
- [ ] Attempt to modify a signed prescription's PDF/content directly via API
- [ ] Guess/enumerate prescription object-storage URLs
- [ ] Doctor attempts to issue a prescription for a consultation that isn't theirs or isn't COMPLETED

## Adversarial Review: Input Abuse (applies broadly)
- [ ] Unicode edge cases, especially RTL/Arabic text in name/bio/notes fields (right-to-left override characters, zero-width joiners) — verify no rendering or storage corruption
- [ ] SQL injection attempts in every search/filter field (mitigated by JPA parameterized queries, but verify no raw-concatenated queries exist anywhere, especially in any native `@Query` used for search)
- [ ] XSS payloads in doctor bio, review comments, prescription notes (React auto-escapes JSX — verify no `dangerouslySetInnerHTML` usage slips in)
```

## 4. Edge Case Coverage (selected, high-value)

```
TIME:
  - Appointment scheduled across a DST transition (Morocco doesn't observe DST changes mid-year the same way historically — verify current rules, but design timezone handling as UTC-stored, locale-displayed regardless)
  - Slot exactly at midnight boundary
  - Cancellation attempted exactly at the policy window boundary (23h59m vs 24h before appointment)

STATE:
  - Doctor's verification_status changes to REJECTED after already having open availability slots — must be pulled from search immediately
  - Patient account soft-deleted mid-session (active JWT) — must be rejected on next request, not just at login

NETWORK:
  - CMI gateway timeout mid-payment — appointment must not be left ambiguously CONFIRMED
  - SMS/Email provider down — must not block booking confirmation (circuit breaker per System Design)
```

## 5. Test Automation Architecture
```
🟡 Medium tier selected: JUnit 5 + Mockito (backend unit), Testcontainers (Postgres integration tests —
   real DB behavior for the EXCLUDE constraint is untestable with mocks), Playwright (frontend e2e),
   MSW (frontend API mocking for component tests).

tests/
├── unit/            — JUnit 5, per-module (mirrors backend package-by-feature structure)
├── integration/      — Testcontainers-backed, hits real Postgres — REQUIRED for booking module
│                        (the double-booking constraint is a DB-level guarantee; mocking the DB
│                         would give false confidence)
├── e2e/              — Playwright, critical paths only: booking flow, video join, prescription issue
├── adversarial/       — Section 3 scenarios, run as a dedicated suite, gates release
└── fixtures/          — Test data factories (doctor, patient, appointment builders)
```
**Video recording note**: per project rules, Playwright E2E runs with video recording enabled at each project version completion, saved to `.recordings/v[version]-[date].webm`, covering: booking happy path, booking race condition, video join window, prescription issuance, RTL/Arabic booking flow (UX/UI doc requirement).

## 6. Traceability Matrix (seed — expand as stories are written)

| Requirement (PRD ref) | Priority | Unit | Integration | E2E | Status |
|---|---|---|---|---|---|
| FR-1 Search doctors | Must | ❌ | ❌ | ❌ | Not covered (Sprint 2) |
| FR-2 Double-booking prevention | Must | ❌ | ❌ | ❌ | Not covered — highest priority for Sprint 2 |
| FR-3 CMI payment capture | Must | ❌ | ❌ | ❌ | Not covered |
| FR-4 WebRTC video session | Must | ❌ | ❌ | ❌ | Not covered |
| FR-5 E-prescription generation | Must | ❌ | ❌ | ❌ | Not covered |
| FR-7 RBAC (4 roles) | Must | ❌ | ❌ | ❌ | Not covered |
| FR-8 Verification workflow | Should | ❌ | ❌ | ❌ | Not covered |

(Full matrix populated once Scrum Master's stories doc assigns story IDs — see docs/stories-tabib-ma.md.)

## 7. NFR Testing Plan
```
PERFORMANCE:
  Search: p95 < 1.5s (PRD NFR-1) — k6 load test against seeded 10k-doctor dataset
  Video signaling: p99 round-trip < 300ms (PRD NFR-7) — measured against managed WebRTC vendor's SLA once selected (System Design SDR-3)

SCALABILITY:
  Load test target: 20 RPS sustained (System Design capacity estimate's burst ceiling), verify no degradation

RELIABILITY:
  Uptime target 99.9% — verified via staging chaos test (kill DB connection mid-transaction, verify no partial-state appointments) before GA

SECURITY:
  → Handed to Security Engineer (threat model complete, docs/security-tabib-ma.md) and DevSecOps (scanning, docs/devops-tabib-ma.md)

ACCESSIBILITY:
  WCAG 2.1 AA on booking flow (PRD NFR-6) — automated axe-core scan in CI + manual screen-reader pass on booking flow in both French and Arabic (RTL)
```

## 8. Release Gates
```
- [ ] All Maximum-risk component tests pass (Booking, Auth, Prescription)
- [ ] No critical/high adversarial findings open
- [ ] Combined coverage ≥ 80% (mandatory project gate)
- [ ] CMI webhook idempotency verified under replay test
- [ ] Double-booking EXCLUDE constraint verified under concurrency test
- [ ] RTL/Arabic booking flow passes e2e
- [ ] Security scan clean (DevOps doc)
- [ ] CI green on the branch (mandatory project gate)
```

## 9. Handoff Points
- **→ Tester**: Implement the unit/integration/e2e suites above per module, starting with Booking + Auth (Maximum risk).
- **→ Backend/Frontend Dev**: Adversarial findings above are requirements, not suggestions — double-booking, webhook idempotency, and IDOR checks must be built defensively from the start, not patched in after a failed test.
- **→ DevOps**: CI pipeline must run unit+integration on every push, e2e nightly or pre-release, coverage gate enforced as a required check.
- **→ PM**: Release readiness = all gates above green.
