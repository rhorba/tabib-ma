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
