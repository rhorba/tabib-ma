# METRICS — Tabib.ma

## 2026-07-22 — Sprint 2, Epic 1 (Identity & Access) coverage
Line coverage: 235/287 = 81.9% | Instruction coverage: 1028/1273 = 80.8%
Gate: jacocoTestCoverageVerification (minimum 0.80) — PASSED
18 tests: AuthServiceTest (unit, Mockito), AuthControllerIntegrationTest (Testcontainers Postgres 16), ArchitectureTest (ArchUnit — zero cross-module cycles)

## 2026-07-27 — Sprint 2, Story 1.1 frontend Batch 2 (OpenAPI codegen) — backend coverage re-check
Instruction coverage: 1028/1273 = 80% (unchanged test set; re-run after springdoc-openapi 2.6.0→2.8.9 bump + CORS default fix)
Gate: jacocoTestCoverageVerification (minimum 0.80) — PASSED
No new backend tests added this batch (springdoc/CORS changes are config-only, covered by existing suite).

## 2026-07-27 — Story 1.1 frontend Batch 5 (auth feature) coverage
Statements: 85.51% (124/145) | Branches: 77.19% | Functions: 83.67% | Lines: 85.31% (122/143)
Scope: src/features/** and src/shared/** (excludes vendor shadcn UI primitives and the generated OpenAPI schema.d.ts — same treatment as backend coverage excluding generated/vendor code).
Gate: ≥ 80% combined — PASSED
26 tests (Vitest + React Testing Library + MSW): schemas validation (login/register), LoginForm/RegisterForm (validation, generic no-enumeration error, conflict error, successful register→auto-login), AuthContext (bootstrap unauthenticated/authenticated, login, logout, silent-refresh session persistence), useSyncHtmlDir (RTL flip), api error-parsing helpers.
Not covered: LoginPage/RegisterPage (thin routing wrappers around the tested forms), LanguageSwitcher (thin wrapper, manually verified live in Batch 3/4) — left uncovered deliberately, same reasoning as excluding shadcn primitives.

## 2026-07-28 — Story 1.1 frontend Batch 6 (e2e) — final combined coverage check
Frontend (Vitest, src/features/**+src/shared/**): Statements 85.51%, Branches 77.19%, Functions 83.67%, Lines 85.31% (122/143) — unchanged from Batch 5, LoginPage/RegisterPage JSX edits added no new branches.
Backend (Gradle jacoco): Instruction 1028/1273 = 80.75%, Line 235/287 = 81.88% — re-verified via `jacocoTestCoverageVerification`, unchanged test set.
Gate: ≥ 80% on both — PASSED. e2e suite (Playwright, 5 tests, real backend) is a smoke layer on top, not counted toward the coverage %.

## 2026-07-29 — Epic 2 Batch 1 (backend Story 2.1) coverage, post Testcontainers-lifecycle fix
Instruction coverage: 1522/1864 = 81.7% (342 missed) | Branch coverage: 37/54 = 68.5% (17 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED

## 2026-07-29 — Epic 2 Batch 2 (backend Story 2.2) coverage
Instruction coverage: 1716/2064 = 83.1% (348 missed) | Branch coverage: 39/56 = 69.6% (17 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
41 tests total: adds VerificationReviewServiceTest (5 unit) + VerificationReviewControllerIntegrationTest (5 integration) to the 31 from Batch 1/Epic 1.

## 2026-07-29 — Epic 2 Batch 5 (frontend automated tests) coverage
Statements: 88.84% (223/251) | Branches: 73.54% (114/155) | Functions: 89.88% (80/89) | Lines: 88.7% (220/248)
Scope: src/features/**+src/shared/** (excludes vendor shadcn UI primitives and generated schema.d.ts — same treatment as Story 1.1). LoginPage/RegisterPage/LanguageSwitcher remain deliberately uncovered per the 2026-07-27 precedent (thin wrappers, manually verified live).
Gate: ≥ 80% combined — PASSED
25 new tests added (51 total, up from 26 at Epic 1 close): doctor-onboarding schemas (9), DoctorProfileForm (2), DocumentUploadForm (2), DoctorOnboardingPage (3), platform-admin VerificationQueueItem (2) + VerificationQueuePage (4), RequireRole (3). Added a clinicHandlers.ts MSW fake (mirrors authHandlers.ts's style) covering the doctor-profiles/verification-queue contract.

## 2026-07-29 — Story 2.3 Batch 1 (backend: clinic creation + doctor invitation) coverage
Instruction coverage: 2108/2531 = 83.3% (423 missed) | Branch coverage: 49/66 = 74.2% (17 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
62 tests total: adds ClinicOnboardingServiceTest (9 unit) + ClinicControllerIntegrationTest (4 integration) to the 48 from Epic 2.

## 2026-07-29 — Story 2.3 Batch 2 (backend: doctor accept/decline) coverage
Instruction coverage: 2284/2677 = 85.3% (393 missed) | Branch coverage: 55/72 = 76.4% (17 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
74 tests total: adds 7 unit tests (DoctorOnboardingServiceTest extended) + 4 integration tests (ClinicInvitationControllerIntegrationTest) to the 62 from Batch 1. Includes the JwtTokenProvider email-claim fix (real bug, not test-only).
38 tests total (up from 18 at Epic 1 close): AuthServiceTest, AuthControllerIntegrationTest, ArchitectureTest, DoctorOnboardingServiceTest (8 unit), DoctorProfileControllerIntegrationTest (6 integration, incl. cross-doctor IDOR case).

## 2026-07-29 — Story 2.3 Batch 5 (frontend automated tests: clinic-admin + pending invitations) coverage
Statements: 90.18% (294/326) | Branches: 74.19% (161/217) | Functions: 92.3% (108/117) | Lines: 90.09% (291/323)
Scope: src/features/**+src/shared/** (same exclusions as prior batches). Up from 88.84%/88.7% at Epic 2 close.
Gate: ≥ 80% combined — PASSED
63 tests total (up from 51 at Epic 2 close): 12 new tests — CreateClinicForm (2), InviteDoctorForm (3), ClinicAdminPage (3), PendingInvitationsList (4 incl. accept/decline/needs-profile-conflict cases). Extended clinicHandlers.ts with clinic/invitation MSW fakes + seedClinic/seedClinicInvitation test helpers; FakeUser role type gained CLINIC_ADMIN.

## 2026-07-30 — Story 2.3 Batch 6 (e2e + final verify) — final combined coverage check
Backend (Gradle jacoco): Instruction 2284/2677 = 85% — re-verified via `jacocoTestCoverageVerification` (task UP-TO-DATE, no backend changes this batch), unchanged test set (74 tests).
Frontend (Vitest, src/features/**+src/shared/**): Statements 90.18% (294/326), Branches 74.19% (161/217), Functions 92.3% (108/117), Lines 90.09% (291/323) — unchanged from Batch 5, no new frontend source landed (only e2e specs, which aren't counted toward this %).
Gate: ≥ 80% on both — PASSED. e2e suite (Playwright, 9 tests total — 7 from Epics 1-2 + 2 new Story 2.3 tests, real backend) is a smoke layer on top, not counted toward the coverage %.

## 2026-07-30 — Epic 3 Batch 1 (backend: doctor search, Story 3.1) coverage
Instruction coverage: 2474/2867 = 86.29% (393 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
82 tests total: adds DoctorSearchServiceTest (4 unit) + DoctorSearchControllerIntegrationTest (4 integration, incl. unauthenticated-rejected, non-APPROVED-excluded, case-insensitive match, no-filter) to the 74 from Story 2.3.

## 2026-07-30 — Epic 3 Batch 2 (backend: public doctor profile, Story 3.2) coverage
Instruction coverage: 2567/2962 = 86.66% (395 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
89 tests total: adds 3 unit tests (DoctorSearchServiceTest extended) + 4 integration tests (DoctorSearchControllerIntegrationTest extended) to the 82 from Batch 1.

## 2026-07-30 — Epic 3 Batch 5 (frontend automated tests: search + public profile) coverage
Statements: 90.71% (332/366) | Branches: 74.59% (185/248) | Functions: 92.96% (119/128) | Lines: 90.63% (329/363)
Scope: src/features/**+src/shared/** (same exclusions as prior batches). Up from 90.18%/90.09% at Story 2.3 close.
Gate: ≥ 80% combined — PASSED
69 tests total (up from 63 at Story 2.3 close): 6 new tests — SearchPage (3) + DoctorPublicProfilePage (3). Extended clinicHandlers.ts with search/public-profile MSW fakes + a findUserById export from authHandlers.ts.

## 2026-07-31 — Epic 4 (Sprint 3→4 continuation) SPRINT_SNAPSHOT
Backend: 151 tests, 89.16% instruction coverage.
Frontend: 98 tests (unit/component), 91.88%/91.84% statement/line coverage, 13 e2e tests (11 pre-existing + 2 new Epic 4 flows).
Both clear the 80% combined coverage gate.

## 2026-08-01 — Epic 6+7 Batch 1 (backend: consultation module, Story 6.1) coverage
Instruction coverage: 5032/5626 = 89% (594 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
197 tests total (up from 151 at Epic 4/5.1 close, plus the auth fast-follow's additions): adds Consultation/ConsultationStatus/ConsultationRepository, JoinWindowPolicy, TurnCredentialProvider+MockTurnCredentialProvider, SignalingTokenIssuer+JwtSignalingTokenIssuer, ConsultationBookingListener, ConsultationService/Controller, and the /ws/consultations WebSocket signaling relay (ConsultationSignalingHandler, SignalingHandshakeInterceptor). Unit + integration test coverage for all of the above.

## 2026-08-01 — Epic 6+7 Batch 2 (backend: prescription module + Story 6.3 completion, coverage)
Instruction coverage: 5786/6406 = 90% (620 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
210 tests total (up from 197 at Batch 1): adds Prescription/PrescriptionItem (immutable, no setters), PrescriptionRepository, PrescriptionPdfGenerator (Apache PDFBox), PrescriptionService (issue/correct/getById/loadPdf), PrescriptionController, and ConsultationService.complete() (Story 6.3, links consultation->prescription). Unit tests for all new classes + PrescriptionControllerIntegrationTest (real DB, full complete->prescribe->correct flow) + PrescriptionPdfGeneratorTest (parses the generated PDF back with PDFTextStripper to assert real content, not just non-empty bytes).

## 2026-08-01 — Epic 6+7 Batch 3 (backend: Story 7.2 access control, coverage) — SPRINT_SNAPSHOT
Instruction coverage: 5801/6421 = 90% (620 missed)
Gate: jacocoTestCoverageVerification (minimum 0.80 instruction) — PASSED
216 tests total (up from 210 at Batch 2): adds GET /api/v1/prescriptions/mine (PrescriptionService.getMine) + PrescriptionAccessControlIntegrationTest, the adversarial Story 7.2 suite against the real backend/DB — Patient A cannot GET, download, or (as a doctor) correct Patient B's prescription; "mine" listing is scoped per-patient; unauthenticated request rejected.
Backend total across Epic 6+7 so far: 216 tests, 90% instruction coverage — clears the 80% gate. Backend portion of Epic 6 (6.1, 6.3) + Epic 7 (7.1, 7.2) is now feature-complete; frontend (Batches 4-6) and e2e (Batch 7) remain.

## 2026-08-04 — Epic 8 Batch 3 (frontend automated tests: clinic dashboard card) coverage
Statements: 82.6% (717/868) | Branches: 72.1% (442/613) | Functions: 86.52% (244/282) | Lines: 83.05% (706/850)
Scope: src/features/**+src/shared/** (same exclusions as prior batches).
Gate: ≥ 80% combined — PASSED
141 tests total: adds 2 new ClinicAdminPage tests (dashboard zero-state, dashboard with bookings) + a seedClinicDashboard test helper/GET .../dashboard MSW fake in clinicHandlers.ts. tsc -b and oxlint both clean.
