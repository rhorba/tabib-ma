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
