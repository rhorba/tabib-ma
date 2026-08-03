# DECISIONS — Tabib.ma



## 2026-07-21 — Stack pivot
Decision: Java Spring Boot + React + PostgreSQL + Docker (replaces Next.js/TS/Drizzle stack in original README).
Reason: User directive for Sprint 1.
Owner: User + Tech Lead

## 2026-07-21 — Doc depth
Decision: Comprehensive depth for all Sprint 1 foundation docs (all roles, edge cases, compliance detail, scalability planning).
Owner: User

## 2026-07-21 — Key architecture decisions
- Modular monolith (not microservices) for v1.
- Managed WebRTC vendor (not self-hosted TURN) — final vendor TBD by DevOps/Tech Lead.
- Double-booking prevented via Postgres EXCLUDE constraint + row lock (ADR-4) — DBA must implement this in schema.
- React Query + Context, no Redux, for frontend state.

## 2026-07-21 — Test/DevOps decisions
- Testcontainers required for booking module integration tests (DB EXCLUDE constraint can't be validated with mocks).
- Coverage gate (80%) enforced via jacoco:check inside CI, not just manually checked.
- Managed WebRTC vendor selection (Twilio Video or Daily.co) deferred to a Sprint 2 spike — not blocking Sprint 1 docs.

## 2026-07-22 — Sprint 2 kickoff decisions
Decision: Gradle (not Maven) as the backend build tool.
Reason: User directive.
Owner: Tech Lead + User

Decision: `TurnCredentialProvider` gets a mock/stub implementation for Sprint 2 (no real Twilio/Daily.co integration yet).
Reason: User directive — unblocks Epic 1 (Identity) and later Epic 6 (Video) development without waiting on the vendor spike/contract. Interface (ADR/Architecture §4 Strategy pattern) already isolates this — swapping the mock for a real adapter later is a contained change.
Owner: Tech Lead + User
Re-evaluate when: Before Sprint 5 (Story 6.1 video join) — real vendor must be selected and integrated by then, mock is not shippable for production video.

## 2026-07-22 — Spec conflict found during Epic 1 implementation: JWT algorithm/expiry
Found: Architecture doc ADR-3 said "JWT access tokens (short-lived, 1hr)" with no algorithm specified. Security doc Section 3 (already approved) specifies RS256, 15-min access token expiry, and argon2id password hashing — a stricter, more specific spec.
Decision: Implement per the Security doc (RS256, 15 min, argon2id) — it's the later, more specific, already-approved authority on security-sensitive parameters, not a new scope change.
Owner: Tech Lead (resolved without re-litigating with user — implementation detail, not new scope)

## 2026-07-22 — CI SCA scan gap: Trivy doesn't resolve Gradle deps without a lockfile
Found: verified locally that `trivy fs .` finds 0 dependency manifests for the Gradle backend (only picks up files inside the gitignored build/ directory) — it needs a gradle.lockfile or SBOM to check Gradle dependency CVEs, unlike Maven's pom.xml which it parses natively.
Decision: Ship the CI security job anyway (still covers secrets via Gitleaks and SAST via Semgrep) but documented the gap in .github/workflows/ci.yml as a comment rather than silently shipping a scan that looks like dependency coverage but isn't.
Owner: DevOps — fast-follow: add Gradle dependency locking or a CycloneDX SBOM step so Trivy actually covers backend dependency CVEs.

## 2026-07-22 — Platform Admin MFA scope gap found during Epic 1 implementation
Found: Security doc mandates TOTP MFA for Platform Admin; no Sprint 2-6 story covered it.
Decision: Added Story 1.5 (TOTP MFA for Platform Admin) to docs/stories-tabib-ma.md as a pre-launch gate, not a Sprint 2 blocker. Sprint 2 ships Epic 1 without MFA.
Owner: User (asked directly — chose "defer to new story" over "build now")

## 2026-07-23 — Story 1.1 frontend scope
Decision: COMPREHENSIVE approach chosen over Balanced — full OpenAPI codegen pipeline (springdoc-openapi backend + openapi-typescript client) AND full i18n (react-i18next, ar/fr, live RTL switching) built now alongside the login/register UI, not deferred.
Rationale: User's explicit choice after Frontend Dev flagged it as broader than YAGNI-strictly-required for two forms; accepted the larger scope intentionally.
Stack (unchanged from Architecture/UI docs): Vite + React 18, feature-folder structure, React Query + Context (no Redux), Tailwind + shadcn/ui, React Hook Form.

## 2026-07-27 — springdoc-openapi 2.6.0 incompatible with Spring Boot 3.5.3
Found: /v3/api-docs threw NoSuchMethodError (ControllerAdviceBean.<init>(Object)) at runtime. Spring Framework 6.2 (pulled in by Spring Boot 3.5.3) removed a constructor springdoc-openapi 2.6.0 depends on — known upstream issue (springdoc/springdoc-openapi #3041), fixed in springdoc 2.8.0+.
Decision: Bumped springdoc-openapi-starter-webmvc-ui to 2.8.9 in backend/build.gradle. Verified live: /v3/api-docs returns valid spec, full backend test suite + 80% coverage gate still pass.
Owner: Backend Dev (found and fixed while wiring frontend OpenAPI codegen, Batch 2 of Story 1.1)

## 2026-07-27 — API base URL convention: no /api/v1 in VITE_API_BASE_URL
Found: VITE_API_BASE_URL was set to .../api/v1, but the generated OpenAPI client paths (from controller @RequestMapping) already include /api/v1 — combining both would double the prefix.
Decision: VITE_API_BASE_URL is the bare server root (e.g. http://localhost:8080); /api/v1 lives only in the generated schema paths. Updated root .env.example and frontend/.env.example accordingly.
Owner: Frontend Dev

## 2026-07-27 — i18n implementation: react-i18next, French primary
Decision: French is fallbackLng/default (per ux-tabib-ma.md line 185 "French primary, Arabic secondary"); language choice persisted to localStorage (key tabibma-language) via i18next-browser-languagedetector so it survives reloads without needing a backend user-preference field yet.
Fonts self-hosted via @fontsource/inter + @fontsource/noto-sans-arabic (no external Google Fonts network call) — matches ui-tabib-ma.md Section 2 font stack.
Html dir/lang synced to i18next language via a small useSyncHtmlDir hook so Tailwind logical properties (ps-/pe-/start-/end-) flip automatically — no separate RTL stylesheet, per ui-tabib-ma.md Section 5.
Owner: Frontend Dev (Story 1.1 Batch 3)

## 2026-07-27 — Token storage: access token in memory, refresh token in localStorage
Found: backend AuthResponse returns both accessToken and refreshToken in the JSON body (no httpOnly cookie mechanism exists) — an existing Epic 1 backend decision, not revisited here.
Decision: Access token lives in memory only (module-level variable, never persisted) since it is short-lived (15 min) anyway. Only the refresh token is persisted to localStorage (key tabibma-refresh-token) — it is already rotated on every use and revocable server-side, so persisting it is the lower-risk half of the pair. On app load, a stored refresh token silently re-establishes the session via POST /auth/refresh.
Owner: Frontend Dev (Story 1.1 Batch 4)
Re-evaluate when: if XSS surface grows (e.g. third-party scripts get added), reconsider a BFF/httpOnly-cookie pattern instead — flagged, not needed for current scope.

## 2026-07-27 — Self-registration scope: Patient/Doctor only, matches backend
Confirmed via AuthService.SELF_REGISTERABLE_ROLES and stories-tabib-ma.md Story 1.1 that self-registration is intentionally limited to Patient/Doctor (Clinic Admin/Platform Admin are provisioned out-of-band). Register form role selector only offers these two, matching the backend contract rather than exposing all 4 Role enum values.
Owner: Frontend Dev

## 2026-07-27 — RTL bug: Radix Select ignores <html dir>, needs explicit DirectionProvider
Found: fixing select.tsx to use Tailwind logical properties (ps-/pe-/end-) was NOT sufficient for RTL — measured the live DOM and found Radix's SelectContent portal sets its own dir="ltr" internally, overriding the inherited document direction for everything rendered inside it (checkmark indicator, positioning math). This is a known Radix behavior: primitives default to ltr unless explicitly told the direction.
Decision: Wrapped the app in radix-ui's `Direction.Provider` (frontend/src/App.tsx), fed by the same i18next language state as useSyncHtmlDir, so every current and future Radix primitive (Select, DropdownMenu, Dialog, etc.) gets the correct direction, not just a one-off per-component fix.
Verified: measured getComputedStyle before/after — indicator moved from right:8px (wrong) to left:8px (correct) once the provider was wired.
Owner: Frontend Dev

## 2026-07-27 — Bug: StrictMode double-fires the session-bootstrap refresh call
Found: React 19 StrictMode's dev-only double effect invocation caused AuthProvider's bootstrap effect to call POST /auth/refresh twice with the same one-time-use refresh token on every page load — confirmed via network trace (2x refresh, 2x /users/me). Refresh tokens are rotated server-side on use with replay-detection that revokes ALL sessions on reuse, so this is a real hazard even though it happened not to trigger the revoke path in manual testing (likely a DB-commit race, not a guarantee).
Decision: Added a useRef guard so the bootstrap logic runs exactly once regardless of StrictMode's double-invoke. Verified via network trace: exactly 1 refresh + 1 /users/me call after the fix.
Owner: Frontend Dev

## 2026-07-27 — Bug: openapi-fetch captures globalThis.fetch at client-creation time
Found while wiring MSW for component tests: apiClient (frontend/src/shared/api/client.ts) requests kept hitting the real backend instead of MSW mocks, even with correct handler URL patterns. Root cause: openapi-fetch's createClient() resolves `fetch: baseFetch = globalThis.fetch` as a parameter default — evaluated once, at client-creation/module-import time. Since MSW patches globalThis.fetch later (inside a test's beforeAll), the already-captured reference is the original, unpatched fetch.
Decision: Pass `fetch: (...args) => globalThis.fetch(...args)` explicitly to createClient() — a thin wrapper that looks up globalThis.fetch at call time instead of capture time. No production behavior change (globalThis.fetch is stable outside tests); makes the client properly mockable.
Owner: Frontend Dev (Story 1.1 Batch 5)

## 2026-07-28 — Epic 2 scope & approach (BRAINSTORM gate)
Scope: Stories 2.1 (doctor profile + credential upload) + 2.2 (Platform Admin verification review queue) this pass. Story 2.3 (clinic onboarding + doctor invitation, Should priority) deferred — its formal dependency on Story 1.4 is real for the invitation UX but 1.4 itself only needs a minimal Clinic/ClinicStaffMembership row, which 2.1/2.2 don't require.
Storage: local-filesystem ObjectStorageClient implementation behind the interface already speced in docs/architecture-tabib-ma.md — same mock-adapter pattern as Twilio/TurnCredentialProvider. Swap for real S3 later without touching callers.
Admin bootstrap: one PLATFORM_ADMIN user seeded via Flyway migration (dev/test credentials, documented in .env.example) rather than building an admin-creation endpoint nothing else needs yet.

## 2026-07-28 — Epic 2 PLAN confirmed
User confirmed the 6-batch plan for Stories 2.1+2.2 (backend 2.1 -> backend 2.2 -> frontend 2.1 -> frontend 2.2 -> tests/coverage -> verify+ship). Proceeding to EXECUTE, Batch 1.

## 2026-07-29 — Root cause found for the "second integration test class always red" blocker carried from 2026-07-28
Found: `AbstractIntegrationTest.POSTGRES` is a `static` field on the shared base class, so every subclass (AuthControllerIntegrationTest, DoctorProfileControllerIntegrationTest) refers to the *same* single container instance via inheritance. It was annotated `@Container` under class-level `@Testcontainers`, which makes JUnit's Testcontainers extension start the container before and stop it after each test class independently. Whichever class ran second therefore tried to reuse a container the first class had already stopped — every DB call blocked for exactly Hikari's 30s connection-timeout before failing (confirmed via per-testcase `time="30.0xx"` in the JUnit XML report and testsuite timestamps showing strict first-class/second-class ordering). This was a real code bug, not environment/Docker resource contention as suspected on 2026-07-28 (that theory is now superseded).
Decision: Removed `@Testcontainers`/`@Container` from `AbstractIntegrationTest` and start `POSTGRES` once in a static initializer block instead — the standard Testcontainers "singleton container" pattern for a container shared across multiple test classes. Ryuk still reaps it at JVM/session end.
Verified: full `./gradlew test` now passes in ~1m (down from ~5m20s with 7 failures), reran clean; `jacocoTestCoverageVerification` passes (81% instruction coverage).
Owner: Backend Dev / Tech Lead

## 2026-07-29 — Gap found starting frontend Story 2.2: PLATFORM_ADMIN seeding decision from 2026-07-28 BRAINSTORM was never implemented
Found: the 2026-07-28 BRAINSTORM decided to seed a PLATFORM_ADMIN via Flyway ("dev/test credentials, documented in .env.example") to unblock Story 2.2 without an admin-creation endpoint. Batches 1-2 built the verification-queue backend but nobody actually added the seed migration — there was no way to reach the PLATFORM_ADMIN-only endpoints outside of a test's direct repository insert.
Decision: Added V3__seed_platform_admin.sql (email tabib-admin@tabibma.dev, password changeme-admin-dev-only, argon2id hash generated via the real Argon2PasswordEncoder bean through a throwaway test — not hand-computed, to guarantee it actually verifies). Documented in .env.example with an explicit rotate-or-remove-before-production note.
Owner: Backend Dev (completing a previously-agreed but dropped scope item, not a new decision)

## 2026-07-29 — Gap found starting frontend Story 2.1: no way for a doctor to fetch their own profile/status
Found: backend Story 2.1 only exposed `POST /api/v1/clinic/doctor-profiles` (create) and `POST .../{id}/documents` (upload) — nothing to read the caller's own profile or their uploaded documents back. Without it, the frontend can't render onboarding status on a page reload/return visit except by provoking a 409 from a duplicate create attempt, which is a poor UX pattern, not a real read path.
Decision: Added `GET /api/v1/clinic/doctor-profiles/me` (own profile, 404 if none) and `GET /api/v1/clinic/doctor-profiles/{id}/documents` (own documents, 403 if not owner) to `DoctorOnboardingService`/`DoctorProfileController`. Treated as an implementation-detail completion of the already-agreed Story 2.1 scope, not a new feature — same precedent as the 2026-07-22 JWT spec-conflict resolution (resolved without re-litigating with the user).
Owner: Backend Dev / Frontend Dev

## 2026-07-29 — Story 2.3 (clinic onboarding + doctor invitation) BRAINSTORM
Research found this story genuinely under-specified: docs have only one UX sitemap bullet, no email-sending infrastructure exists (no notification module; SMTP_* are untouched placeholders), clinic_staff_memberships has no status/token columns (not built for a pending->accepted flow), and — same gap pattern as PLATFORM_ADMIN before it was fixed this epic — there's no way to provision a CLINIC_ADMIN user or a Clinic record.

Decision 1 (invitation flow): In-app accept, no email infrastructure. Admin invites by entering a doctor's email -> creates a PENDING clinic_invitations row. The doctor sees their own pending invitations (matched by their authenticated email, not a mailed token/link) on their existing onboarding page and accepts/declines. Accept creates the ClinicStaffMembership row. Rejected alternatives: a full mocked-EmailSender+token flow (more work, stands up the notification module a sprint early for a Should-priority story) and a no-accept-step direct-add (drops the AC's core "doctor accepts" semantics).
Owner: User (BRAINSTORM gate)

Decision 2 (clinic + admin provisioning): CLINIC_ADMIN self-creates their own Clinic via POST, mirroring the DOCTOR self-service profile pattern from Story 2.1 (`clinics` gets a new `admin_user_id` column — it had no owner column before). One dev/test CLINIC_ADMIN seeded via Flyway, same pattern as the PLATFORM_ADMIN seed added earlier this epic. Rejected alternative: seeding a demo Clinic row directly (skips building the create-clinic endpoint, but clinics had no owner FK to seed against anyway, and self-service is more consistent with Story 2.1's precedent).
Owner: User (BRAINSTORM gate)

## 2026-07-29 — Story 2.3 PLAN confirmed
User confirmed the 6-batch plan (backend clinic+invite -> backend doctor accept/decline -> frontend clinic-admin -> frontend doctor invitations UI -> tests/coverage -> verify+ship), mirroring Epic 2's rhythm exactly. Proceeding to EXECUTE, Batch 1.

## 2026-07-29 — Real bug found in Batch 2: JwtTokenProvider never encoded the email claim, so UserContext.email() was always null from a real token
Found: writing the doctor-side accept/decline integration tests (which match an invitation to the caller by `principal.email()`) surfaced that every integration test failed with 403 "not addressed to you" / an empty pending-invitations list, even right after successfully inviting that exact email. `JwtTokenProvider.generateAccessToken` only ever set the `role` claim and the subject (userId) — never an email claim — and `validateAndExtract` hardcoded `new UserContext(userId, null, role)`. This bug existed since Epic 1 (Story 1.1) but was invisible until now: every prior consumer of `UserContext` only ever compared `userId`, never `email`; my unit tests for the new methods also missed it because they construct `UserContext` directly (bypassing the real JWT round-trip) rather than through a real token like the integration tests do.
Decision: Added an `email` claim to the JWT (encode from `User.getEmail()`, decode into `UserContext.email()`). No frontend impact — the frontend never read this claim; `GET /users/me`'s email always came from the DB via `UserController`, not the token.
Owner: Backend Dev

## 2026-07-30 — Epic 3 (Doctor Search & Discovery) BRAINSTORM
Scope: Stories 3.1 (search by specialty/city) + 3.2 (doctor public profile view), completing Sprint 3 alongside the just-closed Stories 2.1-2.3.
Decision 1 (availability filter): Story 3.1's AC says "search... by specialty, city, and availability" and "only... doctors with open slots are returned", but availability (Story 4.1, Sprint 4) doesn't exist yet. Scoping 3.1 down to specialty+city+APPROVED-only for this pass; the open-slots filter is a natural extension once Story 4.1 lands, not a redesign.
Decision 2 (k6 load test): Story 3.1 is flagged Test Strategy risk score 9, with an AC requiring 1.5s p95 against a seeded 10k-doctor catalog. Deferred to a fast-follow — YAGNI at current scale (a handful of seeded test doctors); build with correct indexes now (specialty, city, verification_status), revisit load testing before the catalog approaches production scale.
Decision 3 (caching): Add the Redis cache layer (specialty+city key, 60s TTL) now, per the system-design doc's already-approved search data flow, rather than starting Postgres-only.
Owner: User (BRAINSTORM gate)

## 2026-07-30 — Real bug found in Epic 3 Batch 6: Epic 2's e2e test assumed a globally-empty verification queue
Found running the full e2e suite together (not just the new Epic 3 spec in isolation): `clinic-onboarding.spec.ts`'s "platform admin approves it" test asserts `Aucune soumission en attente.` (queue globally empty) after approving the one profile it created. This has been silently fragile since it was written — the dev Postgres `pgdata` volume is persistent across every session, and `story-2.3-clinic-onboarding.spec.ts`'s doctor (fixed specialty `Dermatologie`, never approved or rejected by that test, since Story 2.3 doesn't touch the verification queue) leaves a permanent stray PENDING row in the queue on every run. Two prior runs of that suite (this session and an earlier one) had accumulated enough stray PENDING profiles that the "globally empty" assertion started failing — not a regression in Epic 3's own code.
Decision: Fixed the assertion in both `clinic-onboarding.spec.ts` and the new Epic 3 spec to check the specific queue item is gone (`expect(queueItem).not.toBeVisible()`) instead of asserting the whole queue is empty — correct regardless of what other tests/runs have left lying around, not just a workaround for today's count. Manually rejected the 3 stray PENDING profiles already accumulated in the dev DB via curl to unblock this run. Did not touch `story-2.3-clinic-onboarding.spec.ts` itself (still uses a fixed, unapproved `Dermatologie` doctor) — that's a smaller, lower-priority fast-follow now that the downstream assertion no longer breaks because of it.
Owner: Frontend Dev / Test Architect (found and fixed without re-litigating with the user — same "resolve without escalating" precedent as the 2026-07-22 JWT spec conflict)

## 2026-07-30 — Epic 3 PLAN confirmed
User confirmed the 6-batch plan: backend search (3.1) -> backend public profile (3.2) -> frontend search page -> frontend profile page -> tests/coverage -> verify+ship (e2e/video v0.4.0/push), mirroring Epic 2's rhythm. Search endpoint lives in the existing `clinic` module (no new backend module — architecture doc only calls out a frontend `features/search`, backend search is a query concern on the existing DoctorProfile aggregate). Proceeding to EXECUTE, Batch 1.

## 2026-07-31 — Epic 4 BRAINSTORM: comprehensive scope
User picked the 🔴 COMPREHENSIVE option: all of Epic 4 (Stories 4.1 availability, 4.2 book+pay, 4.3 double-booking, 4.4 reschedule/cancel, 4.5 reminders) plus Epic 5 Story 5.1 (CMI payment capture) in this sprint. CMI has no real merchant credentials (.env.example still has changeme placeholders) so payment will use a MockCmiPaymentGatewayAdapter behind the existing PaymentGateway Strategy interface (architecture doc §2/§7) — same mock-external-vendor precedent as Epic 1's TURN provider. Owner: Backend Dev (primary), Frontend Dev, Test Architect for the double-booking adversarial suite (Story 4.3 is the highest-risk story in the whole backlog per Test Strategy).

## 2026-07-31 — Batch 5 scope calls (Stories 4.4 + 4.5)
Reschedule (Story 4.4's title, not its Gherkin AC): no dedicated "reschedule" endpoint. The AC only specifies cancel+refund; a reschedule is exactly cancel-existing (policy-checked) + book-new (existing POST /booking/appointments) composed by the frontend in Batch 6/7, not a new backend orchestration. Avoids a redundant, AC-less endpoint.
Cancellation policy: cancellation itself is ALWAYS allowed regardless of the window (Appointment.cancel()'s only guard is "not already CANCELLED/COMPLETED") — only refund eligibility depends on CancellationPolicy.isWithinWindow(). The boundary (exactly at windowHours) resolves as refund-eligible (inclusive `>=`), tested explicitly.
No cancellation-confirmation notification (PRD FR-6 mentions "cancelled" as a notification event, but neither Story 4.4 nor 4.5's Gherkin AC asks for it) — flagged as a fast-follow, not built now.
No Resilience4j/circuit-breaker library for the "provider down doesn't block booking" NFR — satisfied structurally by @Async @TransactionalEventListener(AFTER_COMMIT) (notification runs after the booking transaction already committed) plus a try/catch around each send call in the listener. Introducing a full circuit-breaker framework for a mocked SMS/email vendor would be premature.
Reminder lead time: no value specified anywhere in docs; defaulting to 24h (config: app.notifications.reminder-lead-time-hours), matching the existing 24h cancellation-window default.

## 2026-07-31 — Batch 6 fast-follows found during live verification
Two pre-existing issues found while manually verifying the new booking UI in Chrome, neither introduced by Epic 4, both deferred:
1. **No auto-retry-on-401 in apiClient.ts.** AuthContext's refresh-token bootstrap is async; any query that fires before it resolves (every hard navigation/reload on a protected page — SearchPage, DoctorPublicProfilePage, DoctorOnboardingPage, ClinicAdminPage, VerificationQueuePage, and now the new booking pages) gets exactly one 401 with nothing retrying it, which most pages' `!data` fallback renders as "not found" instead of a real loading/error state. Root cause: queries aren't gated on `useAuth().status !== 'loading'`. Real user impact (anyone who refreshes a protected page mid-session), not just a test artifact — worth a dedicated fast-follow across all protected pages, not an Epic 4 fix.
2. **Redis has no host port mapping in docker-compose.yml.** Running the backend outside Docker (`gradle bootRun`) against `docker compose up db redis` 500s on any `@Cacheable` endpoint (doctor search) since Redis is only reachable inside the compose network. Not a real gap for normal usage (docker-compose runs backend+db+redis together), but worth a one-line `ports: ["6379:6379"]` addition if host-side bootRun becomes a common dev workflow.

## 2026-08-01 — Epic 6 + Epic 7 BRAINSTORM/PLAN (Video Consultation + E-Prescription)
Decision: bundled Epic 6 (Stories 6.1-6.3) + Epic 7 (Stories 7.1-7.2) into one sprint, same as the Epic 4+5.1 precedent from 2026-07-31 — resolves docs/stories-tabib-ma.md's circular 6.3<->7.1 dependency by shipping both together instead of stubbing prescriptions twice. User picked the comprehensive option at the BRAINSTORM gate.
Vendor: `TurnCredentialProvider` gets a MockTurnCredentialProvider (STUN-only, no real TURN relay) — same mock-external-vendor pattern as Twilio SMS/CMI payment, since no real Twilio Video/Daily.co credentials exist (.env.example WEBRTC_TURN_* are still `changeme` placeholders). Consultation module reuses the existing local-filesystem ObjectStorageClient (built in Epic 4/5 for a different purpose per decisions.md line 99) for prescription PDF storage.
Planned 8 batches (mirrors Epic4+5.1 structure): (1) consultation module backend — entity/service/controller, join-window enforcement, event-listener creating Consultation on BookingConfirmedEvent when slot=VIDEO; (2) Story 6.3 — start/complete consult + prescription module (entity, PrescriptionService, PdfGenerator, immutable supersedes_id correction chain per 7.1); (3) Story 7.2 — ownership-only access control + backend tests; (4) frontend video room UI (join-window gate, 10s connect timeout, poor-connection audio-only banner per UX Flow 3); (5) frontend doctor consult+prescription view, patient consult+prescription view; (6) frontend Vitest suite + MSW fakes; (7) Playwright e2e (video join happy path, audio-fallback trigger, consult+prescription-in-session, adversarial prescription-access-denied), video recording v0.6.0; (8) final coverage re-check, commit, push, CI monitor (rule 11).

## 2026-08-01 — Epic 6+7 Batch 3: docs/stories vs docs/ux discrepancy on skip-prescription
Found while scoping Story 7.2: docs/ux-tabib-ma.md's Flow 3 shows the doctor being prompted "issue prescription? Yes / No -> Mark consultation complete without prescription", but docs/stories-tabib-ma.md Story 6.3's Gherkin AC (sourced "from Test Strategy §2") ties COMPLETED status directly to prescription generation "in the same flow", with no skip branch. Treating the Stories/Test-Strategy AC as binding (same precedent as V8__payments.sql's "DB doc is the binding schema" note when docs disagreed) — ConsultationService.complete() (built in Batch 2) always requires prescribing. The UX doc's "No" skip branch is not implemented; flagging as a fast-follow if product actually wants a skip option, not silently dropped.

## 2026-08-03 — Epic 9 scoping
Found during UNDERSTAND: `AppointmentStatus.COMPLETED` is never actually set anywhere in the backend — `ConsultationService.complete()` marks the `Consultation` COMPLETED but never touches the underlying `Appointment`. Story 9.1's AC ("Given an appointment has status COMPLETED...") is unreachable without fixing this first, so it's in-scope for this epic, not deferred as a separate fast-follow.
Review display depth (user's BRAINSTORM pick): aggregate rating + a list of recent review comments on the doctor's public profile, not aggregate-only.
No dedicated e2e/video batch planned — Test Strategy doc's Minimal tier for Reviews doesn't require e2e, and reaching a real COMPLETED appointment requires the full video-consultation flow Epic 6+7's e2e suite already exercises; backend integration tests will seed COMPLETED directly via repository instead of re-running that flow.
User confirmed the 6-batch plan as proposed (2026-08-03).
Migration V12__reviews.sql deviates from database-tabib-ma.md's documented `rating SMALLINT` — used INTEGER instead to match Hibernate's default mapping for a Java `int` field (Hibernate's ddl-auto:validate rejected the SMALLINT/int2 vs INTEGER/int4 mismatch at context startup). Functionally identical for a 1-5 rating; flagged as a doc discrepancy, not a silent divergence.

## 2026-08-03 — Epic 9 architecture: avoided a clinic<->review module cycle
ArchitectureTest's featureModulesShouldHaveNoCyclicDependencies caught two real violations while wiring the public-profile rating: (1) clinic -> review -> booking -> clinic (review needs Appointment from booking for submission checks; booking already depends on clinic for DoctorProfile), and (2) clinic.dto.DoctorPublicProfileResponse directly referencing review.dto.DoctorReviewSummary.Entry in a field type — ArchUnit counts that as a clinic->review edge on its own, independent of which class populates the field.
Resolution: clinic stays a fully dependency-free "leaf" module (DoctorSearchService.getPublicProfile() always returns stub review fields; DoctorPublicProfileResponse gained its own nested ReviewEntry record instead of reusing review's type). The enriched `GET /api/v1/clinic/doctor-profiles/{id}/public` endpoint moved into a new review.PublicDoctorProfileController (same URL — Spring routes by @RequestMapping value, not package), which composes clinic's base response with review's real aggregate/recent-comments data. review -> clinic and review -> booking is a diamond, not a cycle, since nothing depends back into review.

## 2026-08-03 — Epic 8 scoping
User picked Epic 8 over Epic 10. Scope: Story 8.1 only (booking volume + revenue dashboard) — Story 8.2 (shared clinic resource/room conflicts) is Could-priority and explicitly "not blocking MVP" per the stories doc, deferred as its own future epic if wanted.
Metric definition (BRAINSTORM): booking volume counts appointments with status IN (CONFIRMED, COMPLETED); revenue sums Payment.amountMad where Payment.status = SUCCEEDED. Not strictly COMPLETED-only — Epic 9 found IN_PERSON appointments never reach COMPLETED at all (only VIDEO consults do via ConsultationService.complete()), so a literal reading of the AC would near-zero the dashboard for any clinic with in-person doctors. This matches the invariant "no CONFIRMED appointment without a matching SUCCEEDED Payment" and the story's own "manage staffing and cash flow" justification.
Architecture: the aggregation service/controller will live in the `booking` module, not `clinic` — `booking` already depends on both `clinic` (DoctorProfile) and `payment` (PaymentService) in the safe direction; putting cross-module aggregation in `clinic` instead would recreate the exact clinic<->booking cycle ArchitectureTest caught during Epic 9.
Test Strategy doc scores this "Standard" tier (unlike Reviews' "Minimal") — includes one e2e smoke test + video recording, not skipped this time.
User confirmed the 4-batch plan.
