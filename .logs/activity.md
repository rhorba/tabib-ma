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

PUSH (rule 7): `git push origin main` (16ca678, 8 commits ahead of the previous session's HEAD). CI run 30456793712: GREEN on first try (backend-build-test 1m4s, security 43s — Semgrep/Trivy/Gitleaks all clean, no repeat of the react-router-class CVE surprise from Epic 1 since no new frontend dependencies were added this epic).

## 2026-07-29 — Story 2.3 kickoff: BRAINSTORM + PLAN, then EXECUTE Batch 1 (backend clinic creation + doctor invitation)
BRAINSTORM: user picked "in-app accept, no email infrastructure" for the invitation flow, and "self-service clinic + seeded admin" for provisioning (both logged in .logs/decisions.md). PLAN: 6 batches mirroring Epic 2's rhythm, confirmed by user.
Batch 1: V4__clinic_invitations.sql (adds `admin_user_id` to `clinics` — it had no owner column before; new `clinic_invitations` table with no token, matched by the doctor's authenticated email at accept time; seeds one dev/test CLINIC_ADMIN, same pattern as V3's PLATFORM_ADMIN). New entities/repos: Clinic, ClinicInvitation (+InvitationStatus enum), ClinicStaffMembership. `ClinicOnboardingService`/`ClinicController`: create-my-clinic, invite-doctor-by-email, list-my-clinic's-invitations, all ownership-checked the same way DoctorOnboardingService checks doctor-profile ownership.
Found and fixed a real bug while writing tests: the service used `clinic.getId()` (null in unit tests, since the entity is a plain constructed object never persisted) instead of the method's own `clinicId` parameter for `findAllByClinicId`/invitation construction — inconsistent with the established `DoctorOnboardingService` pattern of using the path parameter directly. Fixed both call sites.
13 new tests (9 unit + 4 integration). Full suite: 62 tests green, 83.3% instruction coverage (logged to .logs/metrics.md).
Not yet done: commit Batch 1, continue to Batch 2 (backend doctor-side accept/decline).

## 2026-07-29 — EXECUTE: Story 2.3 Batch 1 committed (2c355b6), Batch 2 complete (backend doctor accept/decline)
Extended `DoctorOnboardingService` with listMyPendingInvitations/acceptInvitation/declineInvitation, new `ClinicInvitationController` (/api/v1/clinic/invitations). Accept requires the caller to already have a DoctorProfile (ConflictException if not — "create your profile before joining a clinic"), creates the ClinicStaffMembership (skipping it, but still marking the invitation accepted, if a membership already exists — defensive against the same doctor being invited twice), and both accept/decline check the invitation is addressed to the caller's own email and still PENDING.
**Found and fixed a real infrastructure bug**, not just a test-writing one: `JwtTokenProvider` never put an `email` claim in the JWT — `UserContext.email()` had been silently `null` from every real token since Epic 1 (Story 1.1), invisible until now because this is the first feature to ever compare on `principal.email()` rather than `principal.userId()`. My own unit tests for the new methods didn't catch it either, since they construct `UserContext` directly rather than round-tripping through a real JWT like the integration tests do. Fixed by adding the email claim to both encode and decode sides. Logged in .logs/decisions.md.
11 new tests (7 unit + 4 integration). Full suite: 74 tests green, 85.3% instruction coverage.
Not yet done: commit Batch 2, continue to Batch 3 (frontend clinic-admin UI).

## 2026-07-29 — EXECUTE: Story 2.3 Batch 2 committed (e793daa), Batch 3 complete (frontend clinic-admin UI)
Added features/clinic-admin: CreateClinicForm, InviteDoctorForm, ClinicAdminPage (create-clinic form if none exists, else clinic details + invitations list with status badges + invite form). Route at /clinic-admin (CLINIC_ADMIN-only) + conditional nav link. Full fr/ar i18n.
Verified live in Chrome as the seeded clinic-admin@tabibma.dev: created a clinic, invited dr.bennani@example.com (the doctor account from Epic 2's manual testing), saw it appear with a PENDING badge.
Browser automation hit two unrelated transient hangs this batch (CDP screenshot timeouts on an existing tab) — resolved both times by opening a fresh tab; not a product bug, just extension/tab flakiness, not investigated further.
tsc/oxlint/vite build all clean.
Not yet done: commit Batch 3, continue to Batch 4 (doctor-side pending-invitations UI on DoctorOnboardingPage).

## 2026-07-29 — EXECUTE: Story 2.3 Batch 3 committed (5450d5a), Batch 4 complete (doctor-side pending invitations UI)
Before building this batch, found a UX gap: the doctor-facing invitation list only had a raw `clinicId` UUID to show, no clinic name. Added a `clinicName` field to `ClinicInvitationResponse`, resolved in `ClinicInvitationController` by injecting `ClinicRepository` directly (mirrors `AdminAccessController`'s existing precedent of a controller reading a repository for a simple lookup) — committed separately (a4c933a) before the frontend work, full suite reverified green (74 tests).
Added `PendingInvitationsList` (features/doctor-onboarding), wired into `DoctorOnboardingPage` right after the profile card — renders nothing when there are no pending invitations, a card with accept/decline buttons otherwise; surfaces the CONFLICT error ("create your profile first") if a doctor without a profile tries to accept.
Verified live end-to-end across the whole Story 2.3 chain: logged in as the seeded clinic-admin, created "Cabinet Al Amal", invited dr.bennani@example.com (the doctor from Epic 2's testing) — PENDING badge shown; logged in as that doctor, saw "Cabinet Al Amal" in a pending-invitations card with the resolved name (not a UUID), clicked Accepter, card disappeared (invitation now ACCEPTED, ClinicStaffMembership created).
Browser automation hit two more transient CDP hangs this batch (screenshot timeouts) — both resolved on retry, no product impact.
tsc/oxlint/vite build all clean.
Not yet done: Batch 5 (automated tests + coverage), Batch 6 (e2e + video + verify+ship).

## 2026-07-29 — EXECUTE: Story 2.3 Batch 4 committed (1392bb4), Batch 5 complete (frontend automated tests + coverage)
Extended clinicHandlers.ts (src/test/) with clinic/invitation MSW fakes (create/get-my-clinic, invite/list-invitations, list-my-pending-invitations, accept/decline) mirroring ClinicOnboardingService's and DoctorOnboardingService's ownership/role/conflict rules, plus seedClinic/seedClinicInvitation test-only helpers (same style as the existing seedDoctorProfile). FakeUser's role union gained CLINIC_ADMIN.
12 new tests: CreateClinicForm (validation + successful create), InviteDoctorForm (validation + successful invite + duplicate-pending-invite conflict), ClinicAdminPage (create-form/clinic-details/invitations-list states), PendingInvitationsList (empty state, accept, decline, needs-profile conflict error). Full suite: 63 tests green.
Coverage: 90.18% statements / 90.09% lines (scope: src/features/**+src/shared/**, same exclusions as prior batches) — clears the 80% gate, up from 88.84%/88.7% at Epic 2 close (logged to .logs/metrics.md).
tsc/oxlint clean.
Not yet done: Batch 6 (Playwright e2e for the full clinic-onboarding flow, video recording, final combined coverage re-check, push).

## 2026-07-30 — EXECUTE: Story 2.3 Batch 6 complete (verify+ship) — Sprint 2 Story 2.3 CLOSED
Docker Desktop was stopped; started it, then `docker compose up -d --build backend` (db/redis already had a persistent `pgdata` volume from prior manual Chrome verification in Batches 3-4, so the seeded clinic-admin already owned a clinic from that testing — accounted for below). Started the frontend dev server (`npm run dev`) on the host per the established split (db+redis+backend containerized, frontend runs on host via Vite).
Added `frontend/e2e/story-2.3-clinic-onboarding.spec.ts` (2 tests, real backend not MSW): (1) full round-trip — a doctor registers and creates a profile (accepting an invitation requires an existing profile); the seeded CLINIC_ADMIN (`clinic-admin@tabibma.dev`, V4__clinic_invitations.sql) logs in, creates a clinic **only if one doesn't already exist** (the `admin_user_id` column is UNIQUE — one clinic per admin for life of the DB — so the test detects and skips creation if a prior manual/e2e run already created one, then continues with the existing clinic's invite form), invites the doctor by email, doctor logs back in, sees the invitation with the resolved clinic name, accepts it, card disappears; clinic admin logs back in and sees the invitation status flip to "Acceptée" — (2) a DOCTOR (not just an unauthenticated/PATIENT user, to more directly test the CLINIC_ADMIN-only guard against the next-most-privileged adjacent role) is redirected away from `/clinic-admin`. Full e2e suite (9 tests total, the 7 from Epics 1-2 plus these 2): all green.
Video recording (rule 9): `RECORDING_VERSION=0.3.0 npm run e2e:record` -> `.recordings/v0.3.0-2026-07-30.webm` (all 9 flows, ~52s). "0.3.0" chosen as the next shippable increment after Epic 2's "0.2.0".
Final re-verification: backend `./gradlew test jacocoTestCoverageVerification` — UP-TO-DATE (no backend changes this batch), still green at 85% instruction coverage; frontend `tsc --noEmit` + `oxlint` clean, `vitest run --coverage` green (63 tests, 90.18%/90.09% statement/line coverage, unchanged from Batch 5 since no new frontend source landed). Both clear the 80% gate (logged to .logs/metrics.md).
**Sprint 2 Story 2.3 (clinic self-service onboarding + doctor invitation) is now fully CLOSED** — backend + frontend + e2e recorded + both coverage gates cleared.
Committed 62751d1. PUSH (rule 7): `git push origin main` (4966ffb..62751d1, 10 commits). CI run 30544351518: GREEN (all jobs passed — backend-build-test, security).
Local state after this batch: docker containers (db/redis/backend) and the frontend dev server were left running to allow immediate follow-up work; stop them (`docker compose down`, kill the `npm run dev` process) before ending the session if no further work is planned this session.

## 2026-07-30 — Epic 3 kickoff: BRAINSTORM + PLAN, then EXECUTE Batch 1 (backend doctor search)
BRAINSTORM: user picked Epic 3 (Doctor Search & Discovery, Stories 3.1+3.2) to complete Sprint 3. Scoped 3.1's availability filter out (Story 4.1 doesn't exist yet), deferred the story's k6 10k-doctor load-test AC to a fast-follow, and chose to add Redis caching now per the already-approved system-design data flow (all three logged in .logs/decisions.md). PLAN: 6 batches confirmed by user.
Batch 1: `GET /api/v1/clinic/doctor-profiles/search` (specialty+city filters, case-insensitive, APPROVED-only, paginated) in `DoctorSearchService`/`DoctorProfileController` (clinic module — no new backend module, matching the architecture doc which only calls out a frontend `features/search`). Added spring-boot-starter-data-redis + spring-boot-starter-cache, `@Cacheable` on the search method (60s TTL via spring.cache.redis.time-to-live), new partial index `idx_doctor_profiles_approved_specialty_city` (V5 migration) mirroring the existing PENDING partial-index pattern. Extended `AbstractIntegrationTest` with a singleton Redis testcontainer (same pattern as the Postgres singleton fix from 2026-07-29) so integration tests exercise a real Redis, not a mock.
**Found and fixed a real bug** verifying live against the docker-compose backend: the JPQL `(:specialty IS NULL OR LOWER(d.specialty) = LOWER(:specialty))` pattern — needed so callers can omit either filter — hit `ERROR: function lower(bytea) does not exist` on Postgres whenever a filter was blank. Hibernate 6 can't infer a JDBC type for a null String parameter used in this dual IS-NULL/comparison shape and defaults to sending it as `bytea`. Fixed with an explicit JPQL `CAST(:specialty AS string)`. Re-verified live: no-filter, single-filter, and no-match searches all return correct results, and `redis-cli KEYS` confirmed three distinct cache entries after three distinct queries.
8 new tests (4 unit + 4 integration). Full suite: 82 tests green, 86.29% instruction coverage (logged to .logs/metrics.md).
Committed b9a12ce. Not yet done: Batch 2 (backend public doctor profile endpoint, Story 3.2).

## 2026-07-30 — EXECUTE: Epic 3 Batch 2 complete (backend public doctor profile)
Added `GET /api/v1/clinic/doctor-profiles/{id}/public` (`DoctorSearchService.getPublicProfile`, `DoctorPublicProfileResponse`): name/specialty/city/bio/fee for an APPROVED profile, 404 for a non-existent or not-yet-approved one (a PENDING/REJECTED profile isn't publicly visible). `averageRating`/`reviewCount` stubbed to null/0 — no review module yet (Epic 9), and the story explicitly allows degrading gracefully here rather than blocking on it.
7 new tests (3 unit + 4 integration, incl. unauthenticated-rejected, 404-nonexistent, 404-pending, 200-approved-with-stubbed-rating). Full suite: 89 tests green, 86.66% instruction coverage (logged to .logs/metrics.md).
Verified live against the docker-compose backend: fetched an APPROVED profile via the new endpoint (correct name/specialty/fee, null rating/0 reviews), confirmed a random UUID 404s.
Committed 4da1caa. Not yet done: Batch 3 (frontend search page), Batch 4 (frontend profile page), Batch 5 (tests/coverage), Batch 6 (verify+ship).

## 2026-07-30 — EXECUTE: Epic 3 Batch 3 complete (frontend search page)
Regenerated `shared/api/schema.d.ts` against the Batch 1/2 backend (confirmed the search + public-profile endpoints are present). Added `features/search`: `SearchPage` (specialty/city filter form, explicit submit rather than live-search, matching this app's established form pattern) + `DoctorResultCard` (name/specialty/city/fee, links to `/doctors/{id}` for Batch 4's page). Route at `/search`, always-visible nav link (not role-gated — any authenticated role can search) since the backend endpoint itself isn't role-restricted. Full fr/ar i18n, plus `doctorPublicProfile` keys staged ahead for Batch 4 rather than splitting the locale-file edit across two commits.
**Browser automation gap**: the Claude-in-Chrome extension wasn't connecting this session (both `tabs_context_mcp` calls failed with "extension is not connected"), so this batch could not get the live-in-Chrome verification every prior frontend batch has had. Asked the user how to proceed; they chose to continue on tsc/lint/build + the already-curl-verified backend endpoints alone, flagged as a real gap rather than silently skipped. Revisit live-browser verification before Sprint 3 closes if the extension reconnects.
tsc/oxlint/vite build all clean.
Not yet done: Batch 4 (frontend doctor public-profile page), Batch 5 (tests/coverage), Batch 6 (verify+ship).

## 2026-07-30 — EXECUTE: Epic 3 Batch 4 complete (frontend doctor public-profile page)
Added `DoctorPublicProfilePage` (features/search/pages) at `/doctors/:doctorProfileId` — first dynamic route param in the app (`useParams`). Reads `GET .../{id}/public`, 404-handled the same way `ClinicAdminPage`/`DoctorOnboardingPage` handle their own-resource 404s (check `response.status === 404` before the generic error throw). Shows "no reviews yet" when `averageRating` is null, matching the backend's Epic-9-not-built stub.
Retried the Claude-in-Chrome extension before starting this batch — still not connecting, same as Batch 3. Proceeding on tsc/lint/build alone per the user's standing decision from Batch 3; not re-asking each batch.
tsc/oxlint/vite build all clean.
Not yet done: Batch 5 (automated tests + coverage for both frontend features), Batch 6 (verify+ship — e2e/video recording, final push).

## 2026-07-30 — EXECUTE: Epic 3 Batch 5 complete (frontend automated tests + coverage)
Extended `clinicHandlers.ts` with search (specialty+city filter, APPROVED-only) and public-profile MSW fakes; added `findUserById` to `authHandlers.ts` (exported the same way `getAuthenticatedUser` already is) so the fakes can resolve a doctor's display name from the seeded user, mirroring the real backend's `DoctorSearchService` joining `DoctorProfile.userId` against `UserRepository`.
6 new tests: `SearchPage` (empty state, filtered results by specialty+city, excludes non-approved profiles) + `DoctorPublicProfilePage` (not-found for a nonexistent id, not-found for a still-PENDING profile, full details + "no reviews yet" for an approved one — the latter using the same `<Routes><Route path=":id">` wrapper pattern `RequireRole.test.tsx` established for route-param components). Full suite: 69 tests green, 90.71%/90.63% statement/line coverage (up from 90.18%/90.09% at Story 2.3 close, logged to .logs/metrics.md).
tsc/oxlint clean.
Committed 57a78e7. Not yet done: Batch 6 (Playwright e2e for the search-to-profile flow, video recording, final combined coverage re-check, push).

## 2026-07-30 — EXECUTE: Epic 3 Batch 6 complete (verify+ship) — Sprint 3 Epic 3 CLOSED
Added `frontend/e2e/epic-3-search.spec.ts` (2 tests, real backend not MSW): (1) full round-trip — a doctor registers, creates a profile with a run-unique specialty, the seeded PLATFORM_ADMIN approves it; a patient searches by that specialty+city, sees the result, clicks through to the public profile, sees name/specialty/"Pas encore d'avis" — (2) an unapproved doctor's specialty returns no search results, and `/doctors/<random-uuid>` shows the not-found message.
**Found and fixed a real bug** running the full 11-test e2e suite together (not just the new spec in isolation): the pre-existing Epic 2 test (`clinic-onboarding.spec.ts`) asserted the verification queue was *globally* empty after approving one profile — fragile against the persistent dev DB, and it broke this run because `story-2.3-clinic-onboarding.spec.ts`'s doctor (fixed `Dermatologie` specialty, never approved/rejected by that test) had accumulated stray PENDING rows across two prior sessions. Fixed both assertions to check the specific queue item is gone rather than the whole list being empty (logged in .logs/decisions.md); manually rejected the 3 stray profiles to unblock this run. Re-ran: 11/11 green.
Video recording (rule 9): `RECORDING_VERSION=0.4.0 npm run e2e:record` -> `.recordings/v0.4.0-2026-07-30.webm` (all 11 flows, ~73s). "0.4.0" chosen as the next shippable increment after Story 2.3's "0.3.0".
Final re-verification: backend `./gradlew test jacocoTestCoverageVerification` — UP-TO-DATE, 86.66% instruction coverage (89 tests, unchanged this batch); frontend `tsc --noEmit`/`oxlint` clean, `vitest run --coverage` green (69 tests, 90.71%/90.63% statement/line coverage, unchanged this batch — e2e specs aren't counted). Both clear the 80% gate.
**Sprint 3 Epic 3 (Doctor Search & Discovery, Stories 3.1+3.2) is now fully CLOSED** — backend + frontend + e2e recorded + both coverage gates cleared.
Committed f77e11c, 5911b5a. PUSH (rule 7): `git push origin main` (78071bd..5911b5a, 12 commits). CI run 30560623242: GREEN (all jobs passed).
Local state: docker containers (db/redis/backend) and the frontend dev server left running (same as end of Story 2.3) in case of immediate follow-up work.

## 2026-07-31 — Epic 4 PLAN confirmed
User confirmed the 8-batch plan: backend availability (4.1) -> double-booking guard (4.3) -> payment capture (5.1) -> booking orchestration (4.2) -> cancellation+reminders (4.4+4.5) -> frontend booking/payment UI -> frontend cancel/reschedule UI+tests -> verify+ship (e2e/video v0.5.0/push). Proceeding to EXECUTE, Batch 1.

## 2026-07-31 — Epic 4 Batch 1 complete (Story 4.1)
Backend `booking` module: AvailabilityRule (recurring weekly) + AvailabilityBlockedDate (exception dates, table `availability_exceptions`) + AvailabilitySlot entities/repos, AvailabilityService (role+ownership checks mirroring DoctorOnboardingService's pattern, generation algorithm expanding rules into concrete slots for a bounded [from,to) window in Africa/Casablanca timezone converted to UTC-stored Instants per Test Strategy's "UTC-stored, locale-displayed" rule, skips exception dates, de-duplicates against existing slots for idempotent re-runs, capped at 90 days per call), AvailabilityController (rules/exceptions CRUD-lite + generate + open-slots query, all under auth). Flyway V6.
Verified: `./gradlew compileJava compileTestJava` clean, 12 new unit tests + 5 new integration tests (Testcontainers Postgres) all green, full suite 106/106 green (up from 89), 87.19% instruction coverage (up from 86.66%) — clears the 80% gate.
Committed a61001c.

## 2026-07-31 — Epic 4 Batch 2 complete (Story 4.3, highest-risk story)
Appointment aggregate + DoubleBookingGuard (SELECT...FOR UPDATE row lock on AvailabilitySlot, catches DataIntegrityViolationException from the appointments EXCLUDE USING gist constraint (Flyway V7, btree_gist) and translates both failure paths to ConflictException). Adversarial concurrency suite proves both lines of defense on a real Postgres via two-thread races (ExecutorService+CountDownLatch): same-slot race (row lock) and overlapping-different-slot race (EXCLUDE constraint catches what the row lock structurally cannot — different rows, no lock contention). Hit and fixed one self-inflicted test bug: initially gave two slots identical starts_at, which tripped Batch 1's own availability_slots UNIQUE(doctor_profile_id,starts_at) constraint before reaching the EXCLUDE constraint at all — fixed by using overlapping-but-distinct times. Concurrency suite re-run 3x clean (no flakiness).
Full suite: 112/112 green (up from 106), 86.5% instruction coverage (clears the 80% gate).
Committed 0bb08d4.

## 2026-07-31 — Epic 4 Batch 3 complete (Story 5.1)
New `payment` module: Payment entity, PaymentGateway Strategy interface, MockCmiPaymentGatewayAdapter (always-succeeds mock — real CMI still has changeme placeholders in .env.example, same pattern as Epic 1's mock TURN provider). No webhook/PaymentController built: the mock is synchronous so there's nothing async to receive; deferred until a real CmiPaymentGatewayAdapter exists. PaymentService.capturePayment is idempotent by client-supplied idempotencyKey with a DataIntegrityViolationException fallback for the concurrent-insert race.
Full suite: 121/121 green (up from 112), 86.4% instruction coverage (clears the 80% gate).
Committed b5538fc.

## 2026-07-31 — Epic 4 Batch 4 complete (Story 4.2)
BookingService orchestrates DoubleBookingGuard (4.3) + PaymentService (5.1): reserve slot -> server-recomputed fee (no price field on the request DTO at all, so tampered-price is structurally impossible) -> capture payment -> confirm on success (+ publish BookingConfirmedEvent, no listener yet) or cancel+release-slot on failure. BookingController POST/GET /api/v1/booking/appointments. Full-stack integration test proves the double-booking rejection through the real HTTP+DB stack (409), not just at the service layer.
Fast-follow flagged (not blocking): booking doesn't check doctor verification status yet — not in Story 4.2's AC, but a real gap (unapproved doctors' slots are technically bookable).
Full suite: 128/128 green (up from 121), 88.27% instruction coverage (clears the 80% gate).
Committed e30be71.

## 2026-07-31 — Epic 4 Batch 5 complete (Stories 4.4 + 4.5)
CancellationPolicy (inclusive boundary at exactly windowHours) + CancellationService (cancel always allowed, refund conditional). New `notification` module: SmsSender/EmailSender mocks + BookingNotificationListener (@Async @TransactionalEventListener AFTER_COMMIT) consuming BookingConfirmedEvent + new ReminderDueEvent from ReminderService's @Scheduled sweep (24h default lead time). First @EnableAsync/@EnableScheduling in the codebase (shared/config/AsyncSchedulingConfig).
**Real bug found and fixed via the adversarial suite**: Postgres's GiST EXCLUDE constraint check can deadlock (CannotAcquireLockException) under concurrent overlapping inserts, not just cleanly reject (DataIntegrityViolationException) — DoubleBookingGuard's catch was too narrow, widened to DataAccessException, regression test added, re-confirmed clean across 5 reruns.
Environment snag hit twice this batch (same OneDrive/build-dir issue logged 2026-07-27): Gradle's incremental build choked on "not a regular file" for both a stray bash.exe.stackdump under build/resources and later a .class file — both times `rm -rf build` (safe, generated dir only) resolved it immediately.
Full suite: 151/151 green (up from 128), 89.16% instruction coverage (clears the 80% gate).
Committed 05248d0.

## 2026-07-31 — Epic 4 Batch 6 complete (frontend booking + payment UI)
New features/booking: DoctorAvailabilityPage (rules/exceptions/generate), BookAppointmentPage (SlotPicker + confirm-and-pay), MyAppointmentsPage (read-only list). DoctorPublicProfilePage gained a patient-only "Book Appointment" link. Routes gated by RequireRole. Regenerated OpenAPI client (backend run via `docker compose up -d --build backend` on port 8090 for codegen + live verification). Full fr/ar i18n.
Verified live end-to-end via Chrome: doctor sets weekly rule + blocks a date -> generates 48 slots correctly skipping the blocked date -> platform admin approves the profile -> patient searches, views public profile, books+pays (mock gateway), sees CONFIRMED -> appointment shows correctly in "Mes rendez-vous".
Found and logged two pre-existing (not Epic-4-introduced) fast-follows in .logs/decisions.md: no auto-retry-on-401 causing hard-navigation races on every protected page, and Redis's missing host port mapping tripping up host-side `bootRun` against dockerized deps.
tsc/oxlint/vite build clean. No automated tests yet (Batch 7).
Committed 7dacee8.

## 2026-07-31 — Epic 4 Batch 7 complete (cancel/reschedule UI + tests)
MyAppointmentsPage: Cancel + Reschedule buttons on PENDING_PAYMENT/CONFIRMED appointments (hidden for CANCELLED/COMPLETED/NO_SHOW). Reschedule = cancel (existing endpoint) + navigate to /doctors/{id}/book, no new backend endpoint (matches the 2026-07-31 BRAINSTORM decision).
New src/test/bookingHandlers.ts MSW fake (rules/exceptions/slots/appointments) + seedAvailabilitySlot/seedAppointment helpers, wired into mswServer.ts/setup.ts. Added clinicHandlers.findDoctorProfileByUserId for cross-reference.
29 new tests covering all of Epic 4's frontend surface (schemas, both list+form components, GenerateSlotsButton, SlotPicker, DoctorAvailabilityPage, BookAppointmentPage incl. a TOCTOU slot-taken-mid-flow test, MyAppointmentsPage cancel/reschedule/status-gating). One flaky findByRole timeout hit and fixed (bumped timeout to 3000ms for a two-sequential-query page).
Frontend total: 98 tests (up from 69), 91.88%/91.84% statement/line coverage (up from 90.71%/90.63%) — clears the 80% gate. tsc/oxlint/vite build clean.
Committed bc8ce9f.

## 2026-07-31 — Epic 4 Batch 8 complete (Verify + Ship)
Playwright e2e: new e2e/epic-4-booking.spec.ts, 2 tests against the real backend —
  1. full happy path: doctor sets availability + generates slots -> admin approves -> patient books+pays -> CONFIRMED -> cancels -> CANCELLED, buttons hidden -> re-searches and confirms the same slot is bookable again (proves CancellationService actually released it).
  2. double-booking rejection: two independent browser contexts (real concurrent patients) select the same slot and click Confirm via Promise.all — exactly one gets CONFIRMED, the other gets the "slot was just booked" conflict message. Re-run 4x clean, no flakiness.
Found and fixed two authoring bugs during setup: the slot-button locator regex assumed a zero-padded DD/MM/YYYY format but Playwright's default Chromium locale renders `toLocaleString()` as en-US M/D/YYYY; and `video: 'on'` in playwright.config.ts only auto-attaches to the default `page` fixture, not manually-created contexts, so the race test's two extra patient contexts needed `recordVideo` passed explicity to be captured.
Full e2e suite: 13/13 green (11 pre-existing + 2 new).
Video: `RECORDING_VERSION=0.5.0 npm run e2e:record` -> `.recordings/v0.5.0-2026-07-31.webm` (15 clips, all specs including both new-context sessions of the race test).
Final re-verification: backend `./gradlew test jacocoTestCoverageVerification` — 151 tests, 89.16% instruction coverage. Frontend `tsc --noEmit`/`oxlint` clean, `vitest run --coverage` — 98 tests, 91.88%/91.84% statement/line coverage. Both clear the 80% gate.
**Sprint 4 Epic 4 (Appointment Booking & Scheduling, Stories 4.1-4.5) + Epic 5 Story 5.1 (CMI payment capture, mocked) are now fully CLOSED** — backend + frontend + e2e recorded + coverage gates cleared.

## 2026-07-31 — Epic 4 push confirmed CI green
`git push origin main` (dd0a95d..80b8a2d, 8 commits). CI run 30666728859: GREEN (security 59s, backend-build-test 1m14s). No frontend job in this CI pipeline yet (backend-only per the existing workflow) — pre-existing scope, not an Epic 4 regression.

## 2026-08-01 — Epic 6+7 Batch 1 complete (backend: consultation module, Story 6.1)
Built the `consultation` module: Consultation entity (SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED), 1:1 with Appointment, created via ConsultationBookingListener reacting to the existing BookingConfirmedEvent when slot.locationType == VIDEO. JoinWindowPolicy (Specification-lite, ±10min, mirrors CancellationPolicy). TurnCredentialProvider Strategy interface + MockTurnCredentialProvider (Google public STUN only — no real Twilio Video/Daily.co vendor exists, same mock pattern as CMI/Twilio SMS). SignalingTokenIssuer Strategy interface + JwtSignalingTokenIssuer (reuses the existing RS256 keypair) issuing a short-lived token scoped to one (consultationId, userId) pair. Added a self-hosted WebSocket signaling relay at /ws/consultations (ConsultationSignalingHandler + SignalingHandshakeInterceptor, spring-boot-starter-websocket) — necessary because raw WebRTC (unlike a full Twilio Video SDK) still needs somewhere to exchange SDP offer/answer + ICE candidates between exactly 2 peers; the handshake is authenticated via the SignalingToken as a query param since browsers can't set a custom header on a WS upgrade. Flyway V10.
**Found and fixed a real bug**: ConsultationBookingListener's `@TransactionalEventListener(phase = AFTER_COMMIT)` write was silently not persisting — a documented Spring caveat where a plain `@Transactional` (REQUIRED) write inside an AFTER_COMMIT callback "participates" in the original transaction's already-committed, dead resources instead of opening a new one, so no commit ever follows. `save()` returned a real generated id but the row never became visible on a subsequent read. Fixed with `@Transactional(propagation = Propagation.REQUIRES_NEW)` on the listener method. Caught by ConsultationControllerIntegrationTest's booking→GET-by-appointment round trip (a real DB, not a mock, per test-strategy §5) — exactly the kind of bug that class of test exists to catch.
Backend: 197 tests (up from 151), 89% instruction coverage — clears the 80% gate.

## 2026-08-01 — Epic 6+7 Batch 2 complete (backend: prescription module + Story 6.3)
Built the `prescription` module: Prescription entity (immutable by construction — no setters at all, only a constructor; a correction is always a new row with supersedesId pointing at the original, never an UPDATE) + PrescriptionItem value object (@ElementCollection). PrescriptionPdfGenerator renders a plain single-page PDF via Apache PDFBox (Apache 2.0 license — avoids iText's AGPL terms for a document type we generate, not just render). PrescriptionService: issue() (internal, trusted caller — same boundary as PaymentService.capturePayment), correct() and getById()/loadPdf() (directly controller-facing, do their own ownership checks). Reuses the existing LocalFilesystemObjectStorageClient for PDF storage (same interface Epic 4/5 built for a different purpose).
Wired Story 6.3: ConsultationService.complete() — doctor-only, issues the prescription and transitions the consultation to COMPLETED in the same transaction (no "complete without prescribing" path, matching the AC).
Backend: 210 tests (up from 197), 90% instruction coverage — clears the 80% gate. Recurring OneDrive build-dir file-locking issue hit twice this batch (4th time since 2026-07-27) — worked around with `rm -rf build` each time, still not permanently fixed.
