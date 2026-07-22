# Stories: Tabib.ma — Telemedicine & Doctor Booking Platform
**PRD**: docs/prd-tabib-ma.md
**Architecture**: docs/architecture-tabib-ma.md
**Test Strategy**: docs/test-strategy-tabib-ma.md
**Version**: 1.0 | **Date**: 2026-07-22 | **Author**: Scrum Master (+ Test Architect ATDD) | **Status**: Draft

---

## Epic 1: Identity & Access (Auth/RBAC)
Registration, login, and role-based access for Patient, Doctor, Clinic Admin, Platform Admin — the foundation every other epic depends on.

### Story 1.1: Patient/Doctor registration and login
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient or doctor, I want to register and log in with email/password, so I can access the platform under my role.

**Acceptance Criteria**:
```gherkin
Given a new user submits valid registration details
When they submit the registration form
Then an account is created with the correct role (Patient/Doctor)
And a JWT access token + refresh token pair is issued on subsequent login

Given a user provides an incorrect password
When they attempt login
Then access is denied with a generic error (no user-enumeration hint)
```

**Technical Notes**: `identity` module — `User`, `Role`; `JwtTokenProvider` issues 1hr access token + rotatable refresh token (ADR-3). Password hashing per Security doc.

**Dependencies**: None (foundation story).

---

### Story 1.2: Role-based access control enforcement
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev

**Description**: As a platform operator, I want every endpoint to enforce role + ownership checks, so users can only access data they're entitled to.

**Acceptance Criteria**:
```gherkin
Given Doctor A and Doctor B both have appointments
When Doctor A requests Doctor B's appointment by ID
Then the response is 403 Forbidden, not the appointment data

Given an unauthenticated request to any non-public endpoint
When the request is made without a valid JWT
Then the response is 401 Unauthorized
```

**Technical Notes**: Spring Security filter chain resolves `Authentication` → `UserContext` (Architecture §5). Ownership checks live in application-service layer, not annotations alone. Maximum-risk area per Test Strategy — adversarial IDOR suite required (see 1.2-adversarial below).

**Dependencies**: 1.1.

---

### Story 1.3: Refresh token rotation & revocation
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev

**Description**: As a security-conscious platform, I want refresh tokens rotated on use and revocable, so stolen tokens can't be replayed indefinitely.

**Acceptance Criteria**:
```gherkin
Given a refresh token has already been used once
When the same (now-rotated) refresh token is replayed
Then the system rejects it and revokes all sessions for that user
```

**Technical Notes**: Refresh tokens stored hashed in DB per ADR-3. Ties to adversarial checklist "Refresh token reuse after rotation" in Test Strategy §3.

**Dependencies**: 1.1.

---

### Story 1.4: Clinic Admin & Platform Admin roles
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev

**Description**: As a clinic admin or platform admin, I want a distinct role with the correct permission set, so I only see clinic-wide or platform-wide data respectively.

**Acceptance Criteria**:
```gherkin
Given a user has the Clinic Admin role
When they request clinic-wide booking data for their own clinic
Then the data is returned
And a request for another clinic's data returns 403
```

**Technical Notes**: Extends `identity` module roles; `ClinicStaffMembership` entity links admin to clinic.

**Dependencies**: 1.1, 1.2.

---

### Story 1.5: TOTP MFA for Platform Admin
**Priority**: Must (pre-launch gate, not Sprint 2) | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a platform admin, I want mandatory TOTP MFA on login, so a compromised password alone can't grant admin access.

**Acceptance Criteria**:
```gherkin
Given a Platform Admin has enrolled a TOTP authenticator
When they log in with a correct password but no/incorrect TOTP code
Then access is denied until a valid TOTP code is provided

Given a Platform Admin has not yet enrolled MFA
When they attempt to log in
Then they are forced through MFA enrollment before reaching any admin endpoint
```

**Technical Notes**: `identity` module. Traces to Security doc Section 3 ("MFA... mandatory for Platform Admin role") and Section 4 STRIDE Elevation-of-Privilege mitigation ("admin routes also require MFA-verified session flag"). Deferred out of the Sprint 2 Epic 1 batch (2026-07-22) — not a Sprint 2 blocker, but must land before Epic 10 (Platform Admin dispute/health tooling) ships to production, since that's the role this protects.

**Dependencies**: 1.1, 1.4.

---

## Epic 2: Doctor & Clinic Onboarding / Verification
Doctors and clinics join the platform through a credential-verification workflow before appearing in search.

### Story 2.1: Doctor profile creation + credential upload
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a doctor, I want to create my profile and upload license/credential documents, so I can be reviewed for verification.

**Acceptance Criteria**:
```gherkin
Given a doctor has registered
When they submit profile details and upload a credential document
Then the profile is created with VerificationStatus = PENDING
```

**Technical Notes**: `clinic` module — `DoctorProfile`, `VerificationDocument`, `VerificationStatus`. Document storage via `ObjectStorageClient` interface (S3 adapter, shared with `prescription` module).

**Dependencies**: 1.1.

---

### Story 2.2: Platform Admin verification review queue
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a platform admin, I want to review pending doctor/clinic submissions and approve or reject them, so only verified providers appear to patients.

**Acceptance Criteria**:
```gherkin
Given a doctor profile is PENDING verification
When the platform admin approves it
Then VerificationStatus becomes APPROVED and the doctor becomes searchable

Given a doctor's VerificationStatus changes to REJECTED after they already had open availability slots
When a patient searches
Then the doctor is excluded from search results immediately
```

**Technical Notes**: `VerificationReviewService`; admin action logged immutably (audit module, PRD NFR-8). Edge case from Test Strategy §4 (STATE) explicitly covered above.

**Dependencies**: 2.1, 1.4.

---

### Story 2.3: Clinic onboarding + doctor invitation
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a clinic admin, I want to invite doctors to join my clinic account, so they inherit clinic branding and shared scheduling.

**Acceptance Criteria**:
```gherkin
Given a clinic admin invites a doctor by email
When the doctor accepts the invitation
Then a ClinicStaffMembership is created linking the doctor to the clinic
```

**Technical Notes**: `Clinic` entity, `ClinicController`, `DoctorOnboardingService`.

**Dependencies**: 2.1, 1.4.

---

## Epic 3: Doctor Search & Discovery
Patients find doctors by specialty, city, availability, and price.

### Story 3.1: Search doctors by specialty/city/availability
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient, I want to search doctors by specialty, city, and availability, so I can find someone who can see me soon.

**Acceptance Criteria**:
```gherkin
Given doctors exist with varying specialties, cities, and open slots
When a patient searches with a specialty + city filter
Then only matching, verified (APPROVED) doctors with open slots are returned
And results return within 1.5s p95 for a 10,000-doctor catalog (PRD NFR-1)
```

**Technical Notes**: Search hot path may need native `@Query` if JPQL underperforms (ADR-2). High-risk per Test Strategy (risk score 9) — needs k6 load test against seeded 10k dataset.

**Dependencies**: 2.2 (only APPROVED doctors surface).

---

### Story 3.2: Doctor public profile view
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient, I want to see a doctor's profile (credentials, reviews, consultation fee), so I can decide if they're a good fit.

**Acceptance Criteria**:
```gherkin
Given a doctor is APPROVED and has reviews
When a patient views the doctor's profile
Then credentials, average rating, review count, and fee are displayed
```

**Technical Notes**: Reads from `clinic` (profile) + `review` module (aggregate rating).

**Dependencies**: 2.2, 6.1 (reviews — degrade gracefully if none exist yet).

---

## Epic 4: Appointment Booking & Scheduling
The highest-risk epic — double-booking prevention is a permanent invariant (ADR-4).

### Story 4.1: Doctor sets recurring availability + exceptions
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a doctor, I want to set recurring weekly availability and block exceptions, so my calendar stays accurate without daily manual updates.

**Acceptance Criteria**:
```gherkin
Given a doctor defines a recurring weekly schedule
When a specific date is marked as an exception (holiday/blocked)
Then no AvailabilitySlot is generated for that date
```

**Technical Notes**: `booking` module — `AvailabilitySlot`. Applies across all clinics the doctor is affiliated with (PRD: single calendar, no double-booking across locations).

**Dependencies**: 1.1, 2.3 (multi-clinic affiliation).

---

### Story 4.2: Book and pay for an appointment in one flow
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient, I want to book and pay for an appointment in one flow, so I don't have to call the clinic.

**Acceptance Criteria** (from Test Strategy §2):
```gherkin
Scenario: Successful booking and payment
  Given a patient is logged in and a doctor has an open availability slot
  When the patient selects the slot and completes CMI payment
  Then the appointment status becomes CONFIRMED
  And the patient receives a confirmation SMS and email

Scenario: Payment failure does not confirm the appointment
  Given a patient has selected a slot and been redirected to CMI
  When the CMI payment fails
  Then the appointment remains PENDING_PAYMENT or transitions to a failed state
  And the slot becomes available again after the expiry window
```

**Technical Notes**: `BookingService` + `PaymentGateway` interface (Strategy pattern, ADR/Architecture §4). Invariant: no CONFIRMED without a matching CONFIRMED Payment (Architecture §3). Depends on Epic 5 (Payment) for the CMI leg.

**Dependencies**: 3.1, 4.1, 5.1.

---

### Story 4.3: Double-booking prevention (Maximum risk)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev

**Description**: As the platform, I need it to be structurally impossible for a doctor to be double-booked, so patients and doctors never hit scheduling conflicts.

**Acceptance Criteria** (from Test Strategy §2 + adversarial §3):
```gherkin
Scenario: Concurrent booking race — only one wins
  Given two patients simultaneously attempt to book the same doctor slot
  When both requests are submitted at the same time
  Then exactly one appointment is CONFIRMED
  And the other patient sees "This slot was just booked"
```
Adversarial (mandatory, per Test Strategy §3): double-submit race, TOCTOU between slot display and payment redirect, tampered price/fee payload (server must recompute).

**Technical Notes**: `DoubleBookingGuard` domain service + Postgres `EXCLUDE USING gist` constraint on doctor_id + time range (ADR-4, DBA doc). Requires Testcontainers-backed integration test — mocking the DB gives false confidence (Test Strategy §5). This is the single highest-risk story in the backlog; do not defer its adversarial suite.

**Dependencies**: 4.1.

---

### Story 4.4: Reschedule / cancel within policy window
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient, I want to reschedule or cancel within a policy window, so I'm not penalized for reasonable changes.

**Acceptance Criteria**:
```gherkin
Given an appointment is more than the cancellation-window hours away
When the patient cancels
Then the cancellation is accepted and any applicable refund is triggered

Given a cancellation is attempted exactly at the policy window boundary (23h59m vs 24h before)
When the patient submits the cancellation
Then the boundary condition is resolved consistently per CancellationPolicy.isWithinWindow()
```

**Technical Notes**: `CancellationPolicy` value object (Specification-lite pattern, Architecture §4). Boundary case explicitly called out in Test Strategy §4 (TIME).

**Dependencies**: 4.2.

---

### Story 4.5: Appointment reminders (SMS/email)
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev

**Description**: As a patient, I want appointment reminders via SMS/email, so I don't forget or no-show.

**Acceptance Criteria**:
```gherkin
Given a CONFIRMED appointment exists
When the reminder lead time is reached
Then a ReminderDueEvent triggers an SMS and email to the patient

Given the SMS/email provider is down
When a reminder or confirmation is due
Then booking confirmation is not blocked (circuit breaker)
```

**Technical Notes**: `notification` module, `@Async @TransactionalEventListener`. Provider-down resilience is an explicit NFR-adjacent requirement (Test Strategy §4 NETWORK).

**Dependencies**: 4.2.

---

## Epic 5: Payment (CMI)
### Story 5.1: CMI payment capture at booking
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev

**Description**: As a patient, I want to pay via CMI at booking time, so my appointment is confirmed immediately.

**Acceptance Criteria**:
```gherkin
Given a patient has selected an available slot
When they complete CMI payment
Then a Payment record is created with status CONFIRMED and a unique idempotencyKey
And the appointment transitions to CONFIRMED only after this Payment is CONFIRMED
```

**Technical Notes**: `PaymentGateway` interface ← `CmiPaymentGatewayAdapter`. Deposit vs. full-payment policy configurable (PRD FR-3).

**Dependencies**: 4.1.

---

### Story 5.2: CMI webhook handling with idempotency + signature verification
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev

**Description**: As the platform, I need CMI webhooks processed exactly once and only when authentically signed, so payments can't be replayed or spoofed.

**Acceptance Criteria** (from Test Strategy §2 + adversarial §3):
```gherkin
Scenario: CMI webhook replay is rejected
  Given a CMI webhook has already been processed for a transaction reference
  When the same webhook payload is received again
  Then the system does not double-process the payment (idempotency key enforced)
```
Adversarial (mandatory): webhook with invalid/missing signature must be rejected outright; CMI gateway timeout mid-payment must not leave an appointment ambiguously CONFIRMED.

**Technical Notes**: `PaymentWebhookSignatureVerifier`. Maximum/High risk per Test Strategy table (score 12).

**Dependencies**: 5.1.

---

## Epic 6: Video Consultation
### Story 6.1: Browser-based WebRTC video join (no app install)
**Priority**: Must | **Size**: L | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient, I want to join a video consultation from my browser with no app install, so the experience is frictionless.

**Acceptance Criteria** (from Test Strategy §2):
```gherkin
Scenario: Join window enforcement
  Given an appointment is scheduled for 14:00
  When a user attempts to join the video room at 13:30
  Then the join button is disabled until 13:50 (±10min window per UX doc)
```

**Technical Notes**: `consultation` module — `SignalingTokenIssuer`, `TurnCredentialProvider` interface (vendor adapter — Twilio Video vs Daily.co decision still open, tracked in `.logs/risks.md`; this story is blocked on that Sprint 2 spike resolving). TURN/STUN fallback for NAT traversal (PRD FR-4).

**Dependencies**: 4.2 (Consultation created only when appointment CONFIRMED + VIDEO slot type, per Architecture §3), WebRTC vendor spike.

---

### Story 6.2: Poor-connection audio-only fallback
**Priority**: Must | **Size**: M | **Specialist**: Frontend Dev

**Description**: As a patient or doctor on a poor connection, I want an audio-only fallback suggestion, so my consult isn't dropped entirely.

**Acceptance Criteria**:
```gherkin
Scenario: Poor connection triggers audio-only suggestion
  Given a consultation is in progress
  When connection quality drops below the defined threshold
  Then the patient/doctor is offered an audio-only fallback
```

**Technical Notes**: Constraint per PRD: audio-first fallback is mandatory, not optional, given variable Moroccan mobile/broadband quality.

**Dependencies**: 6.1.

---

### Story 6.3: Doctor conducts consult + issues prescription in one session
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a doctor, I want to conduct a video consultation and issue an e-prescription in the same session, so my workflow isn't fragmented.

**Acceptance Criteria**:
```gherkin
Given a video consultation is in progress
When the doctor completes the consult and fills the prescription form
Then the consultation status becomes COMPLETED and a Prescription is generated in the same flow
```

**Technical Notes**: Links `consultation` and `prescription` modules; see Epic 7.

**Dependencies**: 6.1, 7.1.

---

## Epic 7: E-Prescription
### Story 7.1: Generate signed, immutable e-prescription PDF
**Priority**: Must | **Size**: M | **Specialist**: Backend Dev

**Description**: As a patient, I want to receive my e-prescription after a consult, so I can fill it at any pharmacy.

**Acceptance Criteria** (from Test Strategy §2):
```gherkin
Scenario: Prescriptions are never edited in place
  Given a signed prescription exists
  When the doctor issues a correction
  Then a new prescription record is created referencing the original (supersedes_id)
  And the original record remains unchanged
```

**Technical Notes**: `PrescriptionPdfGenerator`, `ObjectStorageClient`. Immutability is a hard invariant (Architecture §3) — Maximum risk per Test Strategy.

**Dependencies**: 6.3.

---

### Story 7.2: Prescription access control (ownership only)
**Priority**: Must | **Size**: S | **Specialist**: Backend Dev

**Description**: As a patient, I want only I (and my doctor) to be able to access my prescription, so my health data stays private.

**Acceptance Criteria** (from Test Strategy §2 + adversarial §3):
```gherkin
Scenario: Patient cannot access another patient's prescription
  Given Patient A and Patient B each have a prescription
  When Patient A requests Patient B's prescription URL directly
  Then access is denied
```
Adversarial (mandatory): guess/enumerate prescription object-storage URLs must fail; doctor attempting to issue a prescription for a consultation that isn't theirs or isn't COMPLETED must be rejected.

**Technical Notes**: Ties to Story 1.2 (RBAC/ownership enforcement pattern) applied to `prescription` module specifically.

**Dependencies**: 7.1, 1.2.

---

## Epic 8: Clinic Admin Dashboard
### Story 8.1: Clinic-wide booking & revenue view
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a clinic admin, I want to see clinic-wide booking volume and revenue, so I can manage staffing and cash flow.

**Acceptance Criteria**:
```gherkin
Given a clinic has multiple doctors with completed appointments
When the clinic admin views the dashboard
Then aggregate booking volume and revenue for that clinic only are shown
```

**Technical Notes**: Read-only aggregation across `booking` + `payment`, scoped by clinic — same cross-module rule as Architecture §2 (no direct table joins across modules; aggregate via service calls or read-model query with explicit clinic filter).

**Dependencies**: 2.3, 4.2, 5.1.

---

### Story 8.2: Shared clinic resource management (rooms/equipment)
**Priority**: Could | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a clinic admin, I want to manage shared resources alongside doctor calendars, so in-person bookings don't conflict.

**Acceptance Criteria**:
```gherkin
Given a physical room is booked for one appointment
When another in-person appointment is scheduled for an overlapping time in the same room
Then the system flags or prevents the conflict
```

**Technical Notes**: Extends the availability/slot model in `booking` with a resource dimension. Lower priority — Could, not blocking MVP.

**Dependencies**: 4.1, 8.1.

---

## Epic 9: Ratings & Reviews
### Story 9.1: Post-consultation review
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a patient, I want to leave a review after a completed consult, so future patients can trust the marketplace.

**Acceptance Criteria**:
```gherkin
Given an appointment has status COMPLETED
When the patient submits a review
Then the review is recorded and becomes visible on the doctor's profile

Given an appointment has not reached COMPLETED status
When a review submission is attempted
Then it is rejected (prevents fake/pre-consult reviews)
```

**Technical Notes**: `review` module. Low risk per Test Strategy (score 4) — Minimal test tier.

**Dependencies**: 4.2 (appointment lifecycle), 6.3/completion path.

---

## Epic 10: Platform Admin — Disputes & Health
### Story 10.1: Dispute queue (no-shows, payment issues, complaints)
**Priority**: Should | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a platform admin, I want flagged disputes in one queue, so I can resolve them quickly.

**Acceptance Criteria**:
```gherkin
Given a no-show, payment issue, or complaint is flagged
When the platform admin opens the dispute queue
Then all open disputes are listed with enough context to act (appointment, patient, doctor, reason)
```

**Technical Notes**: `admin` module — `DisputeController`, `DisputeService`.

**Dependencies**: 4.2, 5.1, 1.4.

---

### Story 10.2: Refund / booking-state override
**Priority**: Should | **Size**: S | **Specialist**: Backend Dev

**Description**: As a platform admin, I want to issue refunds or override booking states in exceptional cases, so support can resolve edge cases without engineering involvement.

**Acceptance Criteria**:
```gherkin
Given a dispute requires a refund
When the platform admin issues the refund
Then the Payment status updates accordingly and the action is logged immutably
```

**Technical Notes**: All admin actions logged immutably per PRD NFR-8 (audit module).

**Dependencies**: 10.1.

---

### Story 10.3: Platform health dashboard
**Priority**: Could | **Size**: M | **Specialist**: Backend Dev + Frontend Dev

**Description**: As a platform admin, I want platform-wide health dashboards (bookings, payment failures, video call quality), so I can catch systemic issues early.

**Acceptance Criteria**:
```gherkin
Given bookings, payment failures, and video quality metrics are being recorded
When the platform admin opens the health dashboard
Then current values for each metric are displayed
```

**Technical Notes**: `AdminDashboardController` — read-only aggregation across modules (Architecture §2). Lower priority than the dispute queue — Could for MVP.

**Dependencies**: 4.2, 5.1, 6.1, 10.1.

---

## Sprint Allocation (estimate — refine at each Sprint Planning)

| Sprint | Stories | Theme |
|---|---|---|
| Sprint 2 | 1.1, 1.2, 1.3, 1.4 | Identity & RBAC foundation |
| Sprint 3 | 2.1, 2.2, 2.3, 3.1, 3.2 | Onboarding/verification + search |
| Sprint 4 | 4.1, 4.2, 4.3, 5.1, 5.2 | Booking + double-booking guard + CMI payment (highest risk — most time) |
| Sprint 5 | 4.4, 4.5, 6.1, 6.2, 6.3 | Reschedule/cancel/reminders + video consultation |
| Sprint 6 | 7.1, 7.2, 9.1, 8.1 | Prescription + reviews + clinic dashboard → **MVP Ready** (per PRD Timeline) |
| Pre-launch gate | 1.5 | TOTP MFA for Platform Admin — must land before Epic 10 (Platform Admin tooling) goes to production, not a Sprint 2 blocker |
| Sprint 7+ (post-MVP) | 8.2, 10.1, 10.2, 10.3 | Clinic resource management + platform admin disputes/health |

**MVP definition** (Sprint 6 target, per PRD §9): search → book → pay (CMI) → video consult → e-prescription, with RBAC and doctor verification, is the critical path. Clinic resource management and platform admin tooling beyond the dispute queue are explicitly sequenced after MVP — consistent with PRD YAGNI scope guidance.

**Blocking external dependency**: Story 6.1 cannot start until the WebRTC vendor spike (Twilio Video vs Daily.co, tracked in `.logs/risks.md`) resolves. If unresolved by Sprint 5, treat as a blocker per the Blocker Protocol, not a silent slip.

---

## Traceability (PRD Requirement → Story)

| PRD Requirement | Story | Priority |
|---|---|---|
| FR-1 Search doctors | 3.1 | Must |
| FR-2 Double-booking prevention | 4.3 | Must |
| FR-3 CMI payment capture | 5.1, 5.2 | Must |
| FR-4 WebRTC video session | 6.1, 6.2 | Must |
| FR-5 E-prescription generation | 7.1 | Must |
| FR-6 SMS/email notifications | 4.5 | Should |
| FR-7 RBAC (4 roles) | 1.2, 1.4 | Must |
| FR-8 Verification workflow | 2.1, 2.2 | Must |
| FR-9 Post-consultation reviews | 9.1 | Should |
| FR-10 Cancellation/rescheduling policy | 4.4 | Must |

## Story Validation Checklist
- [x] Every PRD requirement (FR-1 through FR-10) maps to at least one story
- [x] Every story has testable acceptance criteria (sourced from Test Strategy ATDD scenarios where available)
- [x] Dependencies identified and ordered (Identity → Onboarding/Search → Booking/Payment → Video/Prescription → Admin/Reviews)
- [x] Sizes realistic — nothing larger than L; Stories 4.2, 4.3, 5.1, 6.1 are L and are the ones flagged as highest-risk/most-time in Sprint 4-5
- [x] Architecture decisions referenced in technical notes (ADR-1 through ADR-4, module structure)
- [x] Security requirements reflected (ownership checks, IDOR, webhook signature/idempotency, refresh token rotation)

## Handoff Points
- **→ Tech Lead**: Technical breakdown of Sprint 2 stories (1.1–1.4) for implementation start.
- **→ Test Architect**: Acceptance criteria above are the seed for the traceability matrix in `docs/test-strategy-tabib-ma.md` §6 — update Status column as stories complete.
- **→ Backend/Frontend Dev**: Stories are sequenced; do not start a story whose Dependencies aren't yet done.
- **← From all specialists**: Blockers, scope changes, and re-estimation feed back here each Sprint Planning.
