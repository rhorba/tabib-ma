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

## 2026-07-28 — chore: tracked framework config, skills, and process logs (f97d8ad)
Resolved the "how to handle .logs/ commits" question that had carried unresolved since 2026-07-22: user chose to commit .claude/, CLAUDE.md, README.md, and .logs/ now, as their own commit, ahead of Batch 6.

## 2026-07-28 — EXECUTE: Story 1.1 frontend, Batch 6 complete (verify+ship) — Sprint 2 Epic 1 CLOSED
Installed @playwright/test in frontend/ (needed --legacy-peer-deps: pre-existing typescript ~6.0.2 vs openapi-typescript's ^5.x peer conflict, unrelated to this change, not fixed here — flagged as a carry-forward). Ran `npm audit fix` for two non-breaking high-severity transitive advisories (brace-expansion, js-yaml); left the react-router advisory alone since its fix is a breaking downgrade (react-router-dom 7.11.0) — flagged for a user decision, not fixed silently.
Added frontend/e2e/auth.spec.ts (5 tests, real backend not MSW): register+auto-login, logout+login round-trip, wrong-password rejection, Arabic language switch/RTL, full Arabic register flow. Added tsconfig.e2e.json (excluded from the app/vitest build graph) and excluded e2e/ from vitest's own test discovery (vite.config.ts) after it tried to import Playwright's test.describe.
First run: 5/5 failed. Two real bugs found: (1) my own getByLabel('Nom') substring-matched 'Prénom' too (Playwright getByLabel is substring-matching by default) — fixed with exact:true; (2) a real accessibility gap — shadcn's CardTitle renders a bare `<div>` with no heading role, so LoginPage/RegisterPage had zero semantic headings anywhere (HomePage does have a real <h1>, these two didn't) — fixed by adding role="heading" aria-level={1} at the two call sites. Re-run: 5/5 passing.
Video recording (rule 9): Playwright writes one .webm per test; added frontend/scripts/collect-e2e-video.mjs (ffmpeg concat, confirmed ffmpeg already on this machine) to stitch them into .recordings/v0.1.0-2026-07-28.webm (30.6s, all 5 flows). "0.1.0" chosen as the version label since the project has no formal versioning scheme yet — first shippable increment (full-stack auth).
Final combined coverage check: frontend 85.51%/85.31% (unchanged — JSX-only page edits added no branches), backend re-verified via jacocoTestCoverageVerification at 80.75% instruction / 81.88% line. Both clear the 80% gate (logged to .logs/metrics.md).
Committed a0747ee. PUSH: `git push origin main` (de7b50f) — RED. First CI run in weeks brought the Trivy SCA scan current, and it caught the exact react-router advisory flagged (but deferred) as a fast-follow earlier this batch: GHSA-qwww-vcr4-c8h2, HIGH, fixed in 8.3.0. Per rule 11, stopped all other work to fix it.
react-router-dom stops at 7.18.1 on npm — v8 merged DOM bindings into the base `react-router` package, so this was a package swap (6 import sites) not a version bump. Also added npm overrides for js-yaml/brace-expansion (two more high-severity transitives via @redocly/openapi-core that plain `npm audit fix` couldn't resolve) — 0 vulnerabilities now. Re-verified tsc/lint/26 vitest/5 Playwright e2e/vite build all still green after the swap. Bundle grew 98KB→189KB gzipped (v8 is heavier than the old split package) — logged as a fast-follow, not blocking.
Committed 5ef4dc5, pushed. CI run 30358131540: GREEN (backend-build-test 1m6s, security 34s — both passing). Sprint 2 Epic 1 (Identity & Access, full-stack) is now CLOSED: backend + frontend + CI green + e2e recorded + coverage gates cleared + pushed.

## 2026-07-29 — EXECUTE: Epic 2 Batch 1 verified + committed (3f2cb81), Batch 2 complete (backend Story 2.2)
Resumed the 2026-07-28 blocker: reran `./gradlew test`, reproduced the same "second integration test class always red" failure, but this time traced it to a real code bug rather than environment flakiness (root cause + fix in .logs/decisions.md — AbstractIntegrationTest's shared static Postgres container was being stopped/restarted per test class). Fixed, verified green, committed Batch 1 (doctor profile + document upload, Story 2.1) as 3f2cb81.
Batch 2 (backend Story 2.2 — platform admin verification review queue): added `AuditLog`/`AuditLogRepository` (com.tabibma.shared.audit — append-only, PRD NFR-8) and `VerificationReviewService`/`VerificationReviewController` (com.tabibma.clinic) under /api/v1/admin/platform/verification-queue, reusing SecurityConfig's existing path-based PLATFORM_ADMIN restriction rather than adding a redundant role check (matches the AdminAccessController precedent from Epic 1). Approve/reject both guard against re-reviewing an already-decided profile (ConflictException) and mark all of that profile's verification documents reviewed. 10 new tests (5 unit + 5 integration). Full suite: 41 tests green, jacocoTestCoverageVerification passes at 83.1% instruction coverage (logged to .logs/metrics.md).
Not yet done: commit Batch 2, then Batches 3-6 (frontend Story 2.1, frontend Story 2.2, tests/coverage, verify+ship) per the confirmed plan.

## 2026-07-29 — EXECUTE: Epic 2 Batch 3 complete (frontend Story 2.1)
Added `RequireRole` (shared/components) — first route-guard component in the app, redirects to /login when unauthenticated or / when the role doesn't match; needed since nothing gated routes by role before this. Added `features/doctor-onboarding`: schemas.ts (zod, mirrors backend validation), DoctorProfileForm, DocumentUploadForm (multipart upload via FormData — openapi-fetch passes FormData through untouched per its source, confirmed by reading node_modules/openapi-fetch/src/index.js since the generated schema mistypes the multipart body as JSON), DoctorOnboardingPage (fetches own profile via the new GET /me, shows the create form if none exists, otherwise status badge + document list + upload form). Wired route at /doctor/onboarding (DOCTOR-only) and a conditional nav link in RootLayout. Full fr/ar i18n added.
Regenerated shared/api/schema.d.ts against the running backend (confirms the Batch 1/2 clinic + admin endpoints are all present).
Fixed a zod v4 + react-hook-form typing conflict: z.coerce.number() gives the schema a different input vs. output type, which useForm<T>'s single-generic form doesn't accept — switched to plain z.number() with the number input's onChange doing e.target.valueAsNumber instead of coercing in the schema.
Verified live in Chrome (not just tsc/build/lint, which were also clean): registered a DOCTOR account, created a profile, saw the PENDING status badge, uploaded a document and saw it listed, reloaded the page and confirmed the profile/documents persisted (this is exactly the gap the new GET endpoints closed), logged out and confirmed direct navigation to /doctor/onboarding redirects to /login.
Bundle size unchanged concern carried forward (still one 629KB/196KB-gzip chunk, pre-existing from Epic 1).
Not yet done: Batch 4 (frontend Story 2.2 — platform admin verification queue UI), Batch 5 (tests/coverage), Batch 6 (verify+ship).

## 2026-07-29 — EXECUTE: Epic 2 Batch 4 complete (frontend Story 2.2)
Added `features/platform-admin`: VerificationQueuePage (lists PENDING doctor profiles) + VerificationQueueItem (per-row expandable document list, Approve/Reject buttons, handles the CONFLICT error if a profile was already reviewed by another admin action in-flight). Route at /platform-admin/verification-queue (PLATFORM_ADMIN-only via RequireRole) + conditional nav link.
Found and fixed a real gap while testing this live: no PLATFORM_ADMIN account existed anywhere except inside test code, because the 2026-07-28 BRAINSTORM's "seed via Flyway" decision was never actually implemented across Batches 1-2. Added V3__seed_platform_admin.sql (details in .logs/decisions.md) to close it — full backend suite re-verified green (48 tests) after adding it.
Verified live end-to-end: logged in as the seeded admin, saw the doctor profile created in Batch 3 in the pending queue, viewed its uploaded document, approved it, watched it disappear from the queue, then logged back in as the doctor and confirmed the status badge flipped from "En attente de vérification" to "Vérifié". This closes the loop across both Story 2.1 and 2.2's frontends against the real backend.
tsc/oxlint/vite build all clean.
Not yet done: Batch 5 (automated tests + coverage check for both frontend features), Batch 6 (verify+ship — e2e/video recording, final push).

## 2026-07-29 — EXECUTE: Epic 2 Batch 5 complete (frontend automated tests + coverage)
Added a clinicHandlers.ts MSW fake (src/test/) mirroring authHandlers.ts's established style — in-memory doctor-profiles/verification-queue store, exported resetClinicState() wired into setup.ts's afterEach, plus test-only seedDoctorProfile()/seedUserAndIssueTokens() helpers. Added loginAs.ts test helper that sets both the in-memory access token (so apiClient calls succeed immediately) and the refresh token (so AuthProvider's own bootstrap effect settles to 'authenticated' — needed by anything reading useAuth() directly, like the new RequireRole guard).
25 new tests: doctor-onboarding schemas/DoctorProfileForm/DocumentUploadForm/DoctorOnboardingPage, platform-admin VerificationQueueItem/VerificationQueuePage, RequireRole. Full suite: 51 tests green.
Found two real test-writing bugs while getting these green (not app bugs): (1) RequireRole tests initially failed because `loginAs` only set the access token directly — RequireRole reads useAuth()'s status/user, which only updates via AuthProvider's own bootstrap-from-refresh-token flow, not a raw tokenStore write; fixed by also seeding the refresh token. (2) `user.upload()` from @testing-library/user-event honors the file input's `accept` attribute by default (simulating real browser file-picker filtering), so the "reject a disallowed content type" test never actually attached the bad file — needed `userEvent.setup({ applyAccept: false })` to exercise that code path at all.
Extended src/test/renderWithProviders.tsx with an optional `initialEntries` param (backward compatible, defaults to `['/']`) so RequireRole's redirect tests could render at a route below the guard.
Coverage: 88.84% statements / 88.7% lines on the same scope convention as Story 1.1 (features/**+shared/**, excluding vendor UI + generated schema + the already-precedented thin-wrapper pages) — clears the 80% gate (logged to .logs/metrics.md).
tsc/oxlint clean.
Not yet done: Batch 6 (verify+ship — Playwright e2e + video recording per rule 9, final combined coverage check, push).

## 2026-07-29 — EXECUTE: Epic 2 Batch 6 complete (verify+ship) — Sprint 2 Epic 2 CLOSED
Added frontend/e2e/clinic-onboarding.spec.ts (2 tests, real backend not MSW): (1) full round-trip — doctor registers, creates a profile, uploads a document, logs out; the seeded PLATFORM_ADMIN (V3__seed_platform_admin.sql) logs in, sees it in the verification queue, views the document, approves it, queue empties; doctor logs back in and sees APPROVED status — (2) a PATIENT is redirected away from both /doctor/onboarding and /platform-admin/verification-queue. Full e2e suite (7 tests total, including the 5 from Epic 1's auth.spec.ts): all green.
Video recording (rule 9): `RECORDING_VERSION=0.2.0 node scripts/collect-e2e-video.mjs` -> .recordings/v0.2.0-2026-07-29.webm (47.4s, all 7 flows). "0.2.0" chosen as the next shippable increment after Epic 1's "0.1.0".
Final re-verification (not just trusting earlier batch numbers): backend `./gradlew test jacocoTestCoverageVerification` green (48 tests, 83.1% instruction coverage); frontend `vitest run --coverage` green (51 tests, 88.84%/88.7% statement/line coverage); `vite build` clean, bundle unchanged at 197KB gzip.
**Sprint 2 Epic 2 (Doctor & Clinic Onboarding, Stories 2.1+2.2) is now fully CLOSED**: backend + frontend + e2e recorded + both coverage gates cleared. Next: commit, push, monitor CI (rules 7/11).
