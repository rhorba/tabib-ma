# COMMUNICATIONS — Tabib.ma



## 2026-07-21 — UNDERSTAND
User request: "Start sprint 1 by documenting all project needed docs; use Java Spring stack, React frontend, Docker deployment."
Decision: Full stack pivot confirmed by user — replaces README's Next.js/Drizzle stack.
- Backend: Java Spring Boot (REST API)
- Frontend: React (chosen over Angular)
- DB: PostgreSQL (unchanged, via Spring Data JPA instead of Drizzle)
- Deployment: Docker
Rule 13 applies: Session 1 of new project → all foundation docs before any code.

## 2026-07-23 — Session resume
User confirmed priority: React frontend for Story 1.1 (login/register), over committing untracked root files or other work. Sprint 2 backend (Epic 1) + CI + Docker already shipped and pushed as of last session; da9c457 confirmed present on origin/main (session-end log entry flagging it unpushed was stale).

### [2026-11-18] [UNDERSTAND] — CVE remediation + demo-polish wrap-up
- Track: Standard (security + backend + frontend + CI, more than one specialist).
- Scope: fix red Trivy scan on feat/demo-polish, verify + PR the demo-polish commit, then plan Spring Boot 4 migration.
- Env vars: none new.
