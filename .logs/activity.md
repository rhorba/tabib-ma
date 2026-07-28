# ACTIVITY — Tabib.ma



## 2026-07-21 — PLAN: Sprint 1 foundation docs
Batches:
  1. PRD (PM)
  2. System Design + Architecture (System Designer, Software Architect)
  3. Security + Database (Security Engineer, DBA)
  4. UX + UI (UX Designer, UI Designer)
  5. Test Strategy + DevOps (Test Architect, DevOps/DevSecOps)
  6. Stories (Scrum Master)
  7. Commit + push docs/
Depth: Comprehensive (all roles, edge cases, compliance, scalability).
Stack: Java Spring Boot 3 + React 18 + PostgreSQL 16 + Docker.

## 2026-07-21 — Batch 1 complete: PRD
Created docs/prd-tabib-ma.md (comprehensive depth). Covers 4 roles, 20 user stories, 10 functional + 8 non-functional requirements, risk matrix, timeline.

## 2026-07-21 — Batch 2 complete: System Design + Architecture
Created docs/system-design-tabib-ma.md (NFRs, topology, capacity estimation, 4 SDRs: modular monolith, Postgres-first search, managed WebRTC vendor, in-process async notifications).
Created docs/architecture-tabib-ma.md (layered/package-by-feature modular monolith, module structure for 8 backend modules, domain model sketch, 4 ADRs incl. double-booking prevention via DB exclusion constraint).

## 2026-07-21 — Batch 3 complete: Security + Database
Created docs/security-tabib-ma.md (threat model, JWT auth design, RBAC + ownership checks, STRIDE for booking/video/prescription flows, CNDP compliance mapping flagged as open legal item, incident response plan).
Created docs/database-tabib-ma.md (full DDL for 12 tables, double-booking EXCLUDE constraint per ADR-4, indexing rationale, connection pooling, backup strategy).

## 2026-07-21 — Batch 4 complete: UX + UI
Created docs/ux-tabib-ma.md (4 personas, IA/sitemap, 4 core user flows with error/edge cases, screen-state checklist, heuristic review, RTL/accessibility constraints).
Created docs/ui-tabib-ma.md (Tailwind + shadcn/ui foundation, color/type/spacing tokens, product-specific component patterns, RTL implementation rules for Arabic).

## 2026-07-21 — Batch 5 complete: Test Strategy + DevOps
Created docs/test-strategy-tabib-ma.md (risk-based test matrix, ATDD Gherkin scenarios, adversarial review targeting booking/payment/auth/prescription, traceability matrix seed, NFR testing plan, release gates).
Created docs/devops-tabib-ma.md (GitHub Actions CI/CD with 80% coverage gate enforced in-pipeline, Dockerfiles for Spring Boot + React/Nginx, docker-compose dev setup, infra topology, security scanning, CI monitoring protocol).

## 2026-07-22 — Batch 6 complete: Stories
Created docs/stories-tabib-ma.md (10 epics, 27 stories, ATDD acceptance criteria sourced from Test Strategy Gherkin scenarios, full FR-1..FR-10 traceability, Sprint 2-6 allocation with MVP at Sprint 6, Sprint 7+ for post-MVP admin/clinic-resource stories). WebRTC vendor spike flagged as blocking dependency for Story 6.1.

## 2026-07-22 — Batch 7: git init + docs commit + PUSH
Repo was not yet a git repository — ran `git init`, `git branch -M main`, `git remote add origin https://github.com/rhorba/tabib-ma`.
Staged and committed docs/ only (10 files) per rule 13: commit c9a94b7 "docs: foundation documents for tabib-ma".
PUSH: `git push -u origin main` succeeded — new branch `main` created on origin, tracking set up.
Sprint 1 foundation is now complete and pushed. Untracked but not yet committed: .claude/, .env.example, .logs/, CLAUDE.md, README.md, bash.exe.stackdump (latter looks like a stray crash-dump artifact, not project content — flagged to user, not added).

## 2026-07-22 — Sprint 2, Batch 1: Epic 1 (Identity & Access) backend
Bootstrapped backend/ as a Gradle 9.6.1 Spring Boot 3.5.3 project (Groovy DSL — Gradle 8.10's Kotlin DSL couldn't parse JDK 25's version string; had to download Gradle 9.6.1 to get a working wrapper on this machine's JDK 25).
Implemented: AuthController/AuthService (register/login/refresh), JwtTokenProvider (RS256, 15-min access tokens per Security doc), argon2id password hashing, refresh token rotation + replay detection (RefreshTokenSecurityService, its own REQUIRES_NEW transaction), RBAC path-prefix enforcement (SecurityConfig), GlobalExceptionHandler + JSON auth entry point/access-denied handler, CorrelationIdFilter, ArchUnit fitness function test, Flyway V1 migration (users + refresh_tokens).
Bugs caught and fixed before commit: (1) identity<->shared package cycle (SecurityConfig depended on concrete JwtAuthenticationFilter — fixed via Filter interface + @Qualifier), (2) transaction-rollback bug where the "revoke all sessions on replay" side effect was being undone by the same method's own exception throw (fixed via isolated REQUIRES_NEW transaction).
Verify: 18 tests (Testcontainers Postgres 16 + Mockito) all passing; jacocoTestCoverageVerification gate (80% min) passing at 81.9% line coverage; Semgrep (owasp-top-ten + security-audit) 0 findings.
Committed: 5c2a4fc (backend code) + d51668a (Story 1.5 doc addition). Not yet pushed — pending user confirmation.
Deferred to later batches: React frontend for Story 1.1 login/register UI, CI pipeline (.github/workflows/ci.yml, needs adapting from the devops doc's Maven example to Gradle), Docker/docker-compose wiring, Story 1.4's full clinic/platform data-scoping (blocked on Epics 2/8/10).
PUSH: `git push origin main` succeeded (c9a94b7..d51668a). No CI pipeline exists yet, so this push has nothing to monitor — building the CI workflow is the next batch.

## 2026-07-22 — Sprint 2, Batch 2: CI pipeline (Gradle)
Added .github/workflows/ci.yml (backend-build-test + security jobs), adapted from docs/devops-tabib-ma.md's Maven draft to the Gradle decision.
Pre-push validation caught two real issues before they became CI failures: (1) Semgrep's own github-actions-mutable-action-tag rule blocked on every tag-pinned action reference (citing the real trivy-action supply-chain compromise) — fixed by pinning all 6 actions to full commit SHA; (2) Trivy's fs scanner doesn't resolve Gradle deps without a lockfile (verified locally, 0 manifests found) — documented as a known gap + fast-follow rather than silently shipped as if it were covered.
CI MONITORING (rule 11): first push (ec161bc) went RED — `./gradlew: Permission denied` (Windows doesn't track the Unix executable bit, so gradlew lost +x on commit). Fixed via `git update-index --chmod=+x backend/gradlew`, committed (90d0447), pushed, re-ran.
Second run (29921958312): GREEN — security job 34s, backend-build-test 1m26s. Both jobs passing.
Committed: ec161bc (CI pipeline), 90d0447 (gradlew executable-bit fix). Pushed and CI verified green.

## 2026-07-22 — Sprint 2, Batch 3: Docker Compose local dev stack
Added backend/Dockerfile (multi-stage, non-root, healthcheck) and docker-compose.yml (backend + Postgres 16 + Redis), adapted from docs/devops-tabib-ma.md to Gradle. Host ports made overridable (BACKEND_HOST_PORT/DB_HOST_PORT) after discovering this machine already had other projects bound to 8080/5432.
Smoke-tested end-to-end against the real containerized stack: register -> login -> unauthenticated 401 -> authenticated 200. Caught a real bug the test suite missed: CorrelationIdFilter had no explicit order, ran after Spring Security's chain, so error responses showed requestId as literal "null" — fixed via @Order(HIGHEST_PRECEDENCE), re-verified with a real UUID in the response.
Committed da9c457, PUSH: `git push origin main` succeeded (90d0447..da9c457). CI run 29926295674: GREEN (backend-build-test 1m8s, security 39s).

## 2026-07-23 — Sprint 2, Story 1.1 Frontend, Batch 1: Scaffold & Foundation
- Scaffolded frontend/ with Vite + React 18 + TypeScript (npm create vite@latest, react-ts template)
- Tailwind CSS v4 (via @tailwindcss/vite plugin) + shadcn/ui (new-york style) initialized; design tokens ported from docs/ui-tabib-ma.md §2 into src/index.css (@theme inline mapping, light tokens only — dark class scaffolded per doc, not activated for v1)
- Added shadcn/ui components: button, input, label, form, select, card, alert (+ manually fixed missing deps: clsx, tailwind-merge, class-variance-authority, lucide-react, tw-animate-css, and src/shared/lib/utils.ts — shadcn CLI didn't install them automatically)
- Fixed a shadcn CLI path-alias bug: root tsconfig.json lacked "paths", causing components to be generated into a literal "@" folder on Windows; added paths to both tsconfig.json and tsconfig.app.json (baseUrl omitted — deprecated under TS 6, unneeded with moduleResolution "bundler")
- Feature-folder skeleton created per Architecture doc §6 (features/{auth,search,booking,consultation,prescription,clinic-admin,platform-admin}, shared/{api,components,hooks,context,lib}, app/)
- App shell: app/queryClient.ts (React Query), app/RootLayout.tsx, app/HomePage.tsx, app/routes.tsx (React Router, placeholder /login /register routes for Batch 4), App.tsx wired to providers
- Removed unused Vite scaffold boilerplate (App.css, react.svg, an unrelated hero.png asset), fixed index.html title
- Verified: tsc -b clean, vite build succeeds (98KB gzipped JS, well under the 500KB budget), oxlint clean (2 expected shadcn-generated fast-refresh warnings, upstream pattern), manually verified in Chrome — homepage and /login placeholder both render correctly with tokens/branding, no console errors
- Env: added VITE_API_BASE_URL to root .env.example and new frontend/.env.example; corrected FRONTEND_URL/CORS_ALLOWED_ORIGINS in root .env.example from a guessed localhost:3000 to Vite's real default localhost:5173 (frontend didn't exist yet when that was first written)
- Not committed yet — batch checkpoint, awaiting user go-ahead for Batch 2

## 2026-07-26 — EXECUTE: Story 1.1 frontend, Batch 1 committed
Resumed session. Verified frontend still builds clean (tsc+vite build) after 3 idle days.
Committed Batch 1/6 (scaffold) per user choice: commit now rather than fold into a later commit.
Commit 860b541: frontend/ (Vite+React+TS+Tailwind+shadcn scaffold) + root .env.example port/VITE_API_BASE_URL fixes.
Not pushed yet (mid-sprint commit, not a SHIP-phase boundary). Continuing to Batch 2 (OpenAPI codegen).

## 2026-07-27 — EXECUTE: Story 1.1 frontend, Batch 2 complete (OpenAPI codegen)
Started Docker Desktop (was stopped) + docker compose stack to serve a live OpenAPI spec.
Found + fixed real bug: springdoc-openapi 2.6.0 incompatible with Spring Boot 3.5.3 (see .logs/decisions.md). Bumped to 2.8.9.
Fixed stale CORS default in application.yml (:3000→:5173) and a VITE_API_BASE_URL double-prefix bug (/api/v1 was in both the base URL and the generated paths).
Added openapi-typescript + openapi-fetch, generated src/shared/api/schema.d.ts + client.ts.
Verified end-to-end: live browser call through the generated client hit the real backend (localhost:8090), correct URL, no CORS error, expected 401. tsc/build/lint clean.
Hit a local-only blocker: Gradle test runs failed with Windows file-lock errors on backend/build/ (OneDrive-synced project folder). Asked user how to proceed; user chose "pause OneDrive, retry". Paused it, retried — still failed until a stray build/ directory was force-deleted via PowerShell, after which gradle test ran clean. Restarted OneDrive after. Backend suite: all tests pass, coverage 80% (gate: 80%, still passing after springdoc bump).
Commit 3a35096. Not pushed yet (mid-sprint, not a SHIP boundary). Docker stack stopped/removed after verification.
Remaining: Batches 3-6 (i18n/RTL, actual Login/Register forms, tests, verify+ship).

## 2026-07-27 — EXECUTE: Story 1.1 frontend, Batch 3 complete (i18n/RTL)
Added react-i18next + i18next-browser-languagedetector, fr/ar locale files (src/shared/i18n/locales), useSyncHtmlDir hook (keeps <html lang>/<html dir> in sync so Tailwind logical properties mirror correctly), LanguageSwitcher component wired into RootLayout header. Self-hosted Inter + Noto Sans Arabic via @fontsource (no external font CDN). Added the ui-doc-mandated 1.6 line-height override for lang=ar.
Verified live in browser: French renders by default (browser-language fallback), switching to Arabic flips the whole layout to RTL (header mirrors, text right-aligns, language switcher reorders), choice persists across a full page reload via localStorage, no console errors either direction. tsc/build/lint clean.
Not committed yet — committing after this log entry.
Remaining: Batches 4-6 (actual Login/Register forms, tests, verify+ship).

## 2026-07-27 — EXECUTE: Story 1.1 frontend, Batch 4 complete (Login/Register forms)
Built the real auth feature (frontend/src/features/auth/): AuthContext (session state per architecture doc's React Context guidance), tokenStore (access token in memory, refresh token in localStorage — see decisions.md), zod schemas mirroring backend @Valid constraints, LoginForm/RegisterForm (react-hook-form + shadcn), LoginPage/RegisterPage, wired into routes.tsx and an auth-aware header in RootLayout. Register auto-chains into login since the backend's /auth/register doesn't issue tokens. Role selector restricted to Patient/Doctor per AuthService.SELF_REGISTERABLE_ROLES.
Fixed a pre-existing RTL bug in the Batch-1 shadcn select.tsx (pl-/pr-/right- → ps-/pe-/end-) while wiring the role Select — but that alone wasn't enough: found and fixed a deeper RTL bug where Radix's Select portal ignores document dir entirely, requiring an app-wide Direction.Provider (see decisions.md).
Found and fixed a real bug: StrictMode double-invoking the session-bootstrap effect burned the one-time-use refresh token twice per page load (see decisions.md) — added a ref guard, verified via network trace.
Verified live end-to-end against the real docker-compose backend: register→auto-login→session works, session persists across a full reload (silent refresh, confirmed single network call), logout clears it, wrong-password shows the generic no-enumeration error the story's acceptance criteria requires, duplicate-email shows the correct conflict error, and the entire register form (including the role Select's checkmark/chevron) mirrors correctly in Arabic/RTL. tsc/build/lint clean.
Not committed yet — committing after this log entry.
Remaining: Batches 5-6 (tests, verify+ship).

## 2026-07-27 — EXECUTE: Story 1.1 frontend, Batch 5 complete (automated tests)
Added Vitest + React Testing Library + MSW (matches test-strategy-tabib-ma.md Section 5's frontend tooling choice). vitest.config lives inside vite.config.ts (shared alias resolution) rather than a separate file.
Found and fixed a real bug while wiring MSW: openapi-fetch captures globalThis.fetch once at client-creation time via a parameter default, so MSW's test-time fetch patch (applied in beforeAll, after the client module already imported) never took effect — requests silently hit the real network instead of mocks. Fixed with a thin dynamic-lookup wrapper in client.ts (see .logs/decisions.md).
26 tests added across schemas, LoginForm, RegisterForm, AuthContext (bootstrap/login/logout/silent-refresh), useSyncHtmlDir, and the api error-parsing helpers. Coverage on the tested scope (features/** + shared/**, excluding vendor UI primitives and generated schema.d.ts): 85.51% statements / 85.31% lines — clears the 80% gate (logged to .logs/metrics.md).
tsc/build/lint clean with test files included in the typecheck.
Not committed yet — committing after this log entry.
Remaining: Batch 6 (verify+ship — Playwright e2e smoke + video recording per rule 9, final combined coverage check, push).
