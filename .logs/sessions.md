# SESSIONS — Tabib.ma



## SESSION_START — 2026-07-21
Sprint 1 kickoff: document-first foundation for new project (no docs/ existed yet).

## SESSION_END — 2026-07-21
Sprint 1 (foundation docs, comprehensive depth) — 5 of 6 batches complete.

Done:
  - docs/prd-tabib-ma.md
  - docs/system-design-tabib-ma.md
  - docs/architecture-tabib-ma.md
  - docs/security-tabib-ma.md
  - docs/database-tabib-ma.md
  - docs/ux-tabib-ma.md
  - docs/ui-tabib-ma.md
  - docs/test-strategy-tabib-ma.md
  - docs/devops-tabib-ma.md
  - .env.example updated
  - README.md updated (stack pivot: Java Spring Boot 3 + React 18 + PostgreSQL + Docker)

Remaining for next session:
  - Batch 6: Stories doc (Scrum Master) → docs/stories-tabib-ma.md
  - Batch 7: Review all docs together with user, then commit + push per rule 13
  - NOTE: this directory is not yet a git repository ("Is a git repository: false") —
    will need `git init` (and a remote) before the mandatory docs commit/push can happen.

Key open items carried forward:
  - CNDP/Loi 09-08 filing — legal action item, tracked in .logs/risks.md, blocks production launch not Sprint 1/2.
  - Managed WebRTC vendor selection (Twilio Video vs Daily.co) — deferred to a Sprint 2 spike.
  - Build tool for backend (Maven vs Gradle) — not yet decided, Tech Lead call in Sprint 2.

Resume by: reading this entry, confirming with user whether to proceed straight to Batch 6 (Stories) or revisit earlier docs first.

## SESSION_END — 2026-07-22
Sprint 1 foundation COMPLETE — all 7 batches done.

Done this session:
  - Batch 6: docs/stories-tabib-ma.md (10 epics, 27 stories, full FR traceability)
  - Batch 7: git init, remote added (https://github.com/rhorba/tabib-ma), docs/ committed (c9a94b7) and pushed to main

Sprint 1 is closed. Implementation (Sprint 2) can start next session: Epic 1 (Identity & RBAC) — Stories 1.1-1.4.

Carried-forward open items (unchanged):
  - CNDP/Loi 09-08 filing — legal, blocks production launch, tracked in .logs/risks.md
  - Managed WebRTC vendor selection (Twilio Video vs Daily.co) — Sprint 2 spike, blocks Story 6.1
  - Build tool for backend (Maven vs Gradle) — Tech Lead call, needed before Sprint 2 backend work starts

New this session:
  - .env.example, README.md, CLAUDE.md, .logs/, .claude/ remain untracked (not part of rule 13's docs-only commit) — decide in next session whether/how to commit them
  - bash.exe.stackdump found in repo root — looks like a stray crash-dump file, not project content; not committed, recommend deleting

Resume by: confirm Maven vs Gradle + WebRTC vendor with user, then Tech Lead breaks down Sprint 2 stories (1.1-1.4) for implementation start.

## SESSION_END — 2026-07-22 (continued same day)
User picked Gradle + mock Twilio, then said "start". Sprint 2 Epic 1 (Identity & Access) executed in 3 batches:
  1. Backend: AuthController/Service, JWT (RS256/15min per Security doc, not Architecture doc's looser 1hr), argon2id, refresh rotation+replay detection, RBAC, ArchUnit fitness test, Flyway V1. 18 tests, 81.9% coverage, Semgrep clean. Commits 5c2a4fc, d51668a. Pushed.
  2. CI pipeline (.github/workflows/ci.yml), Gradle-adapted from the devops doc's Maven draft, actions pinned by SHA. Commits ec161bc, 90d0447 (gradlew executable-bit fix — first push went red). Pushed, verified GREEN.
  3. Docker Compose (backend+Postgres+Redis), smoke-tested end-to-end against the real containers (not just automated tests) — caught a real CorrelationIdFilter ordering bug this way. Commit da9c457. **NOT YET PUSHED** — user interrupted before the push completed.

Decisions made along the way (all logged in .logs/decisions.md): Gradle over Maven; mock TurnCredentialProvider for now; JWT algorithm/expiry follows the Security doc over the Architecture doc; Story 1.5 (Platform Admin MFA) added as a pre-launch gate, not a Sprint 2 blocker; Trivy SCA gap for Gradle deps documented as a fast-follow.

Local state: containers tabib-ma-backend-1/db-1/redis-1 still running (ports 8090/5434/6379) — user may want `docker compose down` next session if not continuing immediately.

Resume by: `git push origin main` (da9c457 is one commit ahead of origin), then continue with either the React frontend for Story 1.1 login/register, or another priority the user names.

## SESSION_END — 2026-07-23
Resumed from 2026-07-22 session (confirmed da9c457 was already pushed — prior log entry flagging it unpushed was stale, not an actual gap). User chose to proceed with Story 1.1 frontend (React login/register) over the other carried-forward options.

Ran orchestrator phases: UNDERSTAND → BRAINSTORM (user picked 🔴 COMPREHENSIVE: shadcn/ui + OpenAPI codegen + full i18n(ar/fr)/RTL, not just the balanced two-forms scope) → PLAN (6 batches) → EXECUTE Batch 1 only.

Done this session:
  - Batch 1/6 (Scaffold & Foundation) complete — see .logs/activity.md for full detail:
    - frontend/ scaffolded: Vite + React 18 + TS, Tailwind v4 + shadcn/ui (new-york), design tokens from docs/ui-tabib-ma.md §2
    - Feature-folder structure per Architecture doc §6, React Router + React Query wired, placeholder /login /register routes
    - Fixed a shadcn CLI Windows path-alias bug (root tsconfig.json needed "paths") and manually installed deps the CLI skipped (clsx, tailwind-merge, class-variance-authority, lucide-react, tw-animate-css, shared/lib/utils.ts)
    - Corrected root .env.example: FRONTEND_URL/CORS_ALLOWED_ORIGINS were guessed at :3000 before frontend existed, now :5173 (Vite's real default); added VITE_API_BASE_URL (root + new frontend/.env.example, defaults to :8080 matching BACKEND_HOST_PORT default)
    - Verified via tsc/build/lint (all clean) AND a live Chrome check — homepage + placeholder /login render correctly, no console errors
  - **NOT YET DONE**: Batches 2-6 (OpenAPI codegen, i18n/RTL, actual Login/Register forms, tests, verify+ship)
  - **NOT COMMITTED** — user explicitly chose to leave the Batch 1 work uncommitted this session (asked directly, declined). Working tree has untracked frontend/ plus modified .env.example.

Local state: no dev servers left running (killed the ad-hoc one on port 5190 after verification). Note: unrelated dev server(s) were found already listening on ports 5173/5174 during this session (not ours, not investigated further — probably another project on this machine; be aware when starting frontend/ dev server next time, may need an explicit --port again).

Resume by: `cd frontend`, decide whether to commit Batch 1 now or fold it into a later commit, then continue with Batch 2 (springdoc-openapi on backend + openapi-typescript codegen in frontend) per the plan already agreed with the user.

## SESSION_END — 2026-07-27
Resumed from 2026-07-23 (Batch 1 was done but uncommitted). User approved committing Batch 1 as-is, then worked straight through Batches 2-5 of the Story 1.1 frontend plan.

Done this session:
  - Batch 1 committed as its own commit (860b541) — no changes, just resolved the carried-forward "commit now or later" question.
  - Batch 2 (3a35096): OpenAPI codegen pipeline (springdoc → openapi-typescript → openapi-fetch typed client). Found/fixed springdoc-openapi 2.6.0 being incompatible with Spring Boot 3.5.3 (bumped to 2.8.9) and a stale CORS default + a VITE_API_BASE_URL double-/api/v1-prefix bug.
  - Batch 3 (b507125): react-i18next, French primary / Arabic secondary, full RTL (useSyncHtmlDir, self-hosted Inter + Noto Sans Arabic).
  - Batch 4 (03154dd): real Login/Register forms wired to AuthContext + the live backend. Found/fixed two real bugs: Radix Select ignoring <html dir> (needed an app-wide Direction.Provider, not just Tailwind logical properties) and React 19 StrictMode double-firing the session-bootstrap effect (burning the one-time-use refresh token twice per load).
  - Batch 5 (f38b1a7): Vitest + React Testing Library + MSW test suite, 26 tests, 85.51%/85.31% coverage on features/**+shared/** (clears the 80% gate). Found/fixed openapi-fetch capturing globalThis.fetch at client-creation time, which silently defeated MSW mocking.
  - Environment snag (unrelated to the code): Gradle test runs were blocked by OneDrive file-locking on backend/build/ (project lives in a OneDrive-synced folder) — resolved per user's choice (paused OneDrive, force-cleared the stale build dir, resumed sync after). Will likely recur — worth a permanent fix (exclude backend/build, frontend/node_modules, frontend/dist, frontend/coverage from OneDrive sync) if it keeps happening.

NOT done: Batch 6 (verify+ship) — Playwright e2e smoke test with video recording (CLAUDE.md rule 9, project-version completion), a final combined frontend+backend coverage check, and `git push origin main` (rule 7 — 5 commits ahead of origin, none pushed yet this sprint).

Local state: no docker containers running, no dev servers running, working tree clean except .logs/decisions.md (tracked, ~47 lines of this session's decisions appended but never included in a commit — same unresolved "how to handle .logs/ commits" question carried since 2026-07-22, still not decided with the user). .claude/, README.md, CLAUDE.md, and most of .logs/ remain untracked, also still unresolved.

Resume by: confirm whether to commit the pending .logs/decisions.md changes (and decide the broader .logs/ commit question), then run Batch 6 — Playwright setup + e2e smoke (register/login, French and Arabic) with video recording to .recordings/, final coverage check, push origin main.

## SESSION_END — 2026-07-28
Resumed from 2026-07-27. User resolved both carried-forward questions upfront: commit the untracked process files now (own commit), and proceed straight to Batch 6.

Done this session:
  - chore commit (f97d8ad): .claude/ skills, CLAUDE.md, README.md, .logs/ now tracked — the "how to handle .logs/ commits" question that had carried since 2026-07-22 is resolved.
  - Batch 6 (a0747ee): Playwright e2e suite (5 tests: register/login/wrong-password in French, language-switch + full register in Arabic/RTL) against the real backend, not MSW. Found and fixed two real bugs along the way: my own getByLabel('Nom') substring-matching 'Prénom', and a genuine a11y gap — shadcn's CardTitle is a bare div with no heading role, so Login/Register had zero semantic headings (fixed with role="heading" aria-level={1}). Recorded to .recordings/v0.1.0-2026-07-28.webm via a new ffmpeg-concat script (frontend/scripts/collect-e2e-video.mjs). Final coverage: frontend 85.51%/85.31%, backend 80.75%/81.88% — both clear the 80% gate.
  - PUSH (rule 7) surfaced this sprint's first CI run in weeks, which came back RED: Trivy caught GHSA-qwww-vcr4-c8h2 (react-router CSRF bypass, HIGH) — the same advisory flagged-but-deferred earlier in this batch when `npm audit fix --force` wanted a breaking downgrade. Per rule 11, stopped and fixed it: migrated react-router-dom -> react-router v8 (v8 merged the packages; react-router-dom stops at 7.18.1), plus npm overrides for two more transitive high-severity advisories (js-yaml, brace-expansion) via @redocly/openapi-core. Re-verified everything (tsc/lint/26 vitest/5 e2e/build) still green after the swap. Bundle grew 98KB->189KB gzipped as a side effect — logged as a fast-follow, not blocking.
  - Final push (f00d39c) confirmed GREEN (run 30358247150).

**Sprint 2 Epic 1 (Identity & Access) is now fully CLOSED** — backend + frontend + CI green + e2e recorded + coverage gates cleared + everything pushed to origin/main.

Local state: no docker containers running, no dev servers running, working tree clean. bash.exe.stackdump still sitting in repo root (gitignored via `*.stackdump`, harmless, never addressed — could delete anytime).

Carried-forward open items (unchanged): CNDP/Loi 09-08 filing (legal, blocks production launch), Story 1.4's full clinic/platform data-scoping (blocked on Epics 2/8/10). New fast-follows: frontend bundle code-splitting (189KB gzipped, one chunk), Trivy Gradle-lockfile gap (documented 2026-07-22, still not resolved).

Resume by: confirm next priority with the user — likely starting Epic 2 (or whichever epic the user picks next from docs/stories-tabib-ma.md), following the same UNDERSTAND -> BRAINSTORM -> PLAN gate sequence as Epic 1.
