# ISSUES — Tabib.ma



### [2026-11-18 09:10] [VERIFY] — CVE dependency patches + demo-polish (01cc589) regression
- Tests: backend 339/339 (./gradlew check, coverage gate passed); frontend 177/177, lint/typecheck/build clean; e2e 19/19 — 17 passed first run, 2 auth.spec (fr register) timed out on cold backend start under low host memory (0.5 GB free), passed 5/5 on immediate rerun of auth.spec; no code change between runs.
- Security: Trivy 0.69.3 local (fresh DB) 0 HIGH/CRITICAL with 3 time-boxed ignores (exp 2027-01-17, user-accepted); Semgrep/Gitleaks run in CI.
- Env note: local tabib-ma_pgdata password reset to match .env (changed 2026-11-06 after volume init), user-approved option A.
- Status: pass

### [2026-11-18 09:40] [ISSUE] — CI red on 85c0065: date-dependent consultation test
- CI run 44272857482: backend-build-test failed, ConsultationControllerIntegrationTest.join_rejectedBeforeTheJoinWindowOpens. Security + frontend green.
- Root cause (pre-existing, not from the dep bumps): helper used nextOrSame(dayOfWeek) from today; run on a Wednesday booked today's 09:00 slot, inside the ±10-min join window. Fixed by generating from tomorrow (same pattern ResourceUtilizationControllerIntegrationTest already uses).
- Latent, NOT fixed (scoped out to keep the CI fix minimal): same nextOrSame-from-today pattern in 9 other test helpers (Dispute, Availability, Booking, Cancellation x2, ClinicDashboard, ResourceAllocation, Prescription x2, Review) — can flake on matching weekday after 09:00. Follow-up candidate.

### [2026-11-18 09:45] [VERIFY] — CI fix: consultation test date dependency
- Tests: backend ./gradlew check 339/339 on a Wednesday (the failing condition), coverage gate passed; frontend unchanged since prior VERIFY (177/177).
- Security: test-only change; Trivy unchanged (security job green on 44272857482).
- Status: pass
