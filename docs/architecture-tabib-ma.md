# Architecture: Tabib.ma
**PRD Reference**: docs/prd-tabib-ma.md | **System Design Reference**: docs/system-design-tabib-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Software Architect | **Status**: Draft

## 1. Architecture Style Decision

**Chosen: Layered architecture, package-by-feature, modular monolith.**

The domain has real complexity (scheduling conflicts, multi-role permissions, payment state machines, consultation lifecycle) that justifies clear module boundaries — but not full Clean/Hexagonal Architecture or DDD tactical patterns (aggregates, domain events across bounded contexts, CQRS). Package-by-feature gives most of DDD's boundary benefits (each module owns its data, cross-module access via service interfaces) without the ceremony. Upgrade to stricter DDD only if a specific module's business rules become genuinely tangled — booking/scheduling is the most likely candidate to watch.

```
┌─────────────────────────────────────────────┐
│  Presentation   — REST controllers (Spring Web) │
├─────────────────────────────────────────────┤
│  Application    — services (use-case orchestration) │
├─────────────────────────────────────────────┤
│  Domain         — entities, business rules      │
├─────────────────────────────────────────────┤
│  Infrastructure — JPA repositories, CMI client, │
│                   TURN client, email/SMS clients │
└─────────────────────────────────────────────┘
```
Dependency rule: infrastructure implements interfaces defined by the application/domain layer (e.g., `PaymentGateway` interface implemented by `CmiPaymentGatewayAdapter`) — domain code never imports Spring Data or an HTTP client directly.

## 2. Backend Module Structure (Spring Boot, package-by-feature)

```
com.tabibma
├── identity/              (Identity/Auth bounded context)
│   ├── User, Role, ClinicStaffMembership (entities)
│   ├── AuthController, UserService
│   ├── JwtTokenProvider, PasswordEncoder config
│   └── UserRepository (interface) + JpaUserRepository (impl)
│
├── booking/                (Booking + Scheduling — highest-complexity module)
│   ├── Appointment, AvailabilitySlot, CancellationPolicy (entities/value objects)
│   ├── BookingController, BookingService, AvailabilityService
│   ├── AppointmentRepository, AvailabilitySlotRepository
│   └── DoubleBookingGuard (domain service — enforces slot-lock invariant)
│
├── payment/                 (Payment)
│   ├── Payment, PaymentStatus (entity/enum)
│   ├── PaymentController (webhook endpoint), PaymentService
│   ├── PaymentGateway (interface) ← CmiPaymentGatewayAdapter (infra impl)
│   └── PaymentWebhookSignatureVerifier
│
├── consultation/            (Video consultation)
│   ├── Consultation, ConsultationStatus
│   ├── ConsultationController, ConsultationService
│   ├── SignalingTokenIssuer, TurnCredentialProvider (interface) ← vendor adapter
│
├── prescription/            (E-prescription)
│   ├── Prescription, PrescriptionItem
│   ├── PrescriptionController, PrescriptionService
│   ├── PrescriptionPdfGenerator, ObjectStorageClient (interface) ← S3 adapter
│
├── clinic/                  (Clinic + doctor onboarding/verification)
│   ├── Clinic, DoctorProfile, VerificationDocument, VerificationStatus
│   ├── ClinicController, DoctorOnboardingService, VerificationReviewService
│
├── admin/                   (Platform admin — disputes, refunds, health dashboard)
│   ├── DisputeController, DisputeService
│   ├── AdminDashboardController (read-only aggregation across modules)
│
├── review/                  (Ratings & reviews)
│   ├── Review, ReviewController, ReviewService
│
├── notification/            (Cross-cutting — consumed via events by other modules)
│   ├── BookingConfirmedEvent, BookingCancelledEvent, ReminderDueEvent (domain events)
│   ├── NotificationListener (@Async @TransactionalEventListener)
│   ├── SmsSender (interface), EmailSender (interface) ← provider adapters
│
└── shared/
    ├── config/              (Spring config, CORS, security filter chain)
    ├── exception/            (ApiException hierarchy → global @ControllerAdvice)
    ├── audit/                (immutable admin-action audit log — see Security doc NFR-8)
    └── web/                  (pagination, common DTOs, API versioning conventions)
```

**Cross-module rule**: `booking` never queries `payment`'s tables directly — it calls `PaymentService` (or reacts to a `PaymentConfirmedEvent`). Same rule applies to every module pair. Enforced via ArchUnit tests in CI (see Test Strategy doc).

## 3. Key Domain Model Sketch

```
Appointment (aggregate root within booking module)
  - id, patientId, doctorId, clinicId (nullable — direct doctor booking allowed)
  - slot: AvailabilitySlot (value object: start, end, location type [IN_PERSON|VIDEO])
  - status: PENDING_PAYMENT | CONFIRMED | CANCELLED | COMPLETED | NO_SHOW
  - cancellationPolicy: CancellationPolicy (value object — window hours, refund %)
  Invariant: cannot transition PENDING_PAYMENT → CONFIRMED without a matching CONFIRMED Payment record.
  Invariant: no two CONFIRMED appointments for the same doctor with overlapping slot windows (enforced via DB constraint + row lock, not just application logic — see DBA doc).

Payment
  - id, appointmentId, amount, currency (MAD), status, cmiTransactionRef, idempotencyKey

Consultation
  - id, appointmentId (1:1), status, startedAt, endedAt
  - Created only when appointment.status == CONFIRMED and slot.locationType == VIDEO

Prescription
  - id, consultationId, doctorId, patientId, items: List<PrescriptionItem>, pdfUrl, signedAt
  - Immutable once signed (no edits — a correction creates a new prescription referencing the old one)
```

## 4. Design Patterns Applied

| Pattern | Where | Why |
|---|---|---|
| Repository | Every module's data access | Standard decoupling of persistence from business logic |
| Strategy | `PaymentGateway`, `TurnCredentialProvider`, `SmsSender`/`EmailSender` interfaces | Swap CMI/vendor implementations without touching business logic — driven by real multi-vendor need (SDR-3 in System Design), not speculation |
| Domain Events | `BookingConfirmedEvent`, `PaymentConfirmedEvent` | Decouple booking/payment from notification — booking module doesn't know how confirmation is communicated |
| Specification-lite | `DoubleBookingGuard`, `CancellationPolicy.isWithinWindow()` | Composable, testable business rules kept out of controllers/services |

Explicitly **not** applying: CQRS (read/write models don't diverge enough), Event Sourcing (no temporal-query or audit-replay requirement beyond the admin audit log, which is a simple append-only table), full DDD aggregates-everywhere (only `Appointment` has real aggregate-root complexity worth naming).

## 5. Cross-Cutting Concerns

| Concern | Approach |
|---|---|
| Logging | SLF4J + Logback, structured JSON, correlation ID per request (MDC) |
| Error handling | `ApiException` hierarchy (`ValidationException`, `NotFoundException`, `ConflictException` for double-booking) → `@ControllerAdvice` maps to consistent JSON error shape + HTTP status |
| Validation | Bean Validation (`@Valid`) at controller boundary; business invariants (double-booking, cancellation window) enforced in domain/application layer, not annotations |
| Config | `application.yml` + env var overrides (Spring's standard relaxed binding) — matches `.env.example` |
| Auth context | `Authentication` principal → resolved to `UserContext` injected into service methods via Spring Security context, never passed as raw IDs from the client |
| Transactions | `@Transactional` at application-service method boundary (use-case level), not spread across repository calls |

## 6. Frontend Architecture (React)

```
src/
├── features/
│   ├── search/         (doctor search, filters)
│   ├── booking/         (booking flow, calendar picker, payment redirect)
│   ├── consultation/    (video call UI, WebRTC client wiring)
│   ├── prescription/    (view/download prescription)
│   ├── clinic-admin/     (clinic dashboard, onboarding)
│   ├── platform-admin/  (verification queue, disputes, health dashboard)
│   └── auth/            (login, registration, role-based routing)
├── shared/
│   ├── api/             (typed API client, generated or hand-written from OpenAPI)
│   ├── components/      (design-system components — see UI doc)
│   └── hooks/
└── app/                 (routing, providers, root layout)
```
- **State management**: React Query (server state/caching) + React Context for auth/session — no Redux. Booking flow's local wizard state is component-local (`useReducer`), not global. Upgrade to a global store only if cross-feature state sharing becomes a real pain point.
- **API contract**: OpenAPI spec generated from Spring Boot (springdoc-openapi) is the source of truth; frontend generates typed client from it to avoid drift.

## 7. Architectural Fitness Functions (enforced in CI)

```
Coverage:    ≥ 80% (unit + integration combined) — see Test Strategy doc
Cycles:      0 circular dependencies between modules — enforced via ArchUnit rule
Coupling:    No module imports another module's repository/entity directly — ArchUnit rule
Build time:  < 5 minutes for backend, < 3 minutes for frontend — re-evaluate if exceeded
Bundle size: < 500KB gzipped initial JS bundle (frontend) — route-based code splitting per feature folder
```

## 8. Architecture Decision Records

### ADR-1: Modular Monolith, Package-by-Feature
**Status**: Accepted
**Context**: Marketplace with 4 roles and several cross-cutting workflows (booking→payment→consultation→prescription); team is small for Sprint 1-6.
**Decision**: Single Spring Boot deployable, packages organized by feature/bounded context, enforced boundaries via ArchUnit.
**Consequences**: + Simple deployment, single transaction boundary for most flows, low operational overhead. − Cannot scale/deploy modules independently (acceptable per System Design SDR-1).
**Re-evaluate when**: System Design SDR-1 trigger conditions are met.

### ADR-2: JPA/Hibernate over jOOQ/raw SQL
**Status**: Accepted
**Context**: Standard CRUD-heavy domain with some complex queries (search, availability lookups).
**Decision**: Spring Data JPA for standard CRUD; native queries (via `@Query`) for the availability-search hot path if JPQL proves insufficient for performance.
**Consequences**: + Fast to build, well-understood by Java teams. − JPA N+1 pitfalls must be actively guarded against (see DBA doc for indexing/fetch strategy).
**Re-evaluate when**: Query performance profiling shows JPA-generated SQL is a bottleneck the DBA can't fix via `@EntityGraph`/fetch joins.

### ADR-3: JWT-Based Stateless Auth
**Status**: Accepted
**Context**: Multiple client roles, need for mobile-web friendliness, no server-side session storage desired for horizontal scale readiness.
**Decision**: Spring Security + JWT access tokens (short-lived, 1hr) + refresh tokens (stored hashed in DB, rotatable, revocable).
**Consequences**: + Stateless, horizontally scalable. − Revocation requires the refresh-token DB check (access tokens can't be revoked mid-life; kept short-lived to bound this risk).
**Re-evaluate when**: Never, unless a session-based requirement emerges (unlikely for this product shape).

### ADR-4: Double-Booking Prevention via DB Constraint + Row Lock
**Status**: Accepted
**Context**: This is the single highest-risk correctness bug class for the product (PRD risk matrix, System Design data flow #1).
**Decision**: `SELECT ... FOR UPDATE` on the availability slot during booking transaction, plus a DB-level exclusion constraint (Postgres `EXCLUDE USING gist` on doctor_id + time range) as a second line of defense against application-layer bugs.
**Consequences**: + Correctness guaranteed even under app bugs or concurrent requests. − Slightly more complex migration (see DBA doc for the exclusion constraint definition).
**Re-evaluate when**: Never — this is a permanent invariant, not a scaling trade-off.

## 9. Handoff Points
- **→ Backend Dev**: Module structure, interfaces (`PaymentGateway`, `TurnCredentialProvider`, `SmsSender`/`EmailSender`), repository contracts above.
- **→ Frontend Dev**: Feature-folder structure, React Query + Context state strategy, OpenAPI-generated client contract.
- **→ DBA**: `Appointment` aggregate + exclusion-constraint requirement (ADR-4), entity list per module for schema design.
- **→ Test Architect**: ArchUnit fitness functions above; `DoubleBookingGuard` and payment webhook idempotency are the top two areas needing adversarial test scenarios.
