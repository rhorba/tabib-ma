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
