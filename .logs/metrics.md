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
38 tests total (up from 18 at Epic 1 close): AuthServiceTest, AuthControllerIntegrationTest, ArchitectureTest, DoctorOnboardingServiceTest (8 unit), DoctorProfileControllerIntegrationTest (6 integration, incl. cross-doctor IDOR case).
