# ISSUES — Tabib.ma



### [2026-11-18 09:10] [VERIFY] — CVE dependency patches + demo-polish (01cc589) regression
- Tests: backend 339/339 (./gradlew check, coverage gate passed); frontend 177/177, lint/typecheck/build clean; e2e 19/19 — 17 passed first run, 2 auth.spec (fr register) timed out on cold backend start under low host memory (0.5 GB free), passed 5/5 on immediate rerun of auth.spec; no code change between runs.
- Security: Trivy 0.69.3 local (fresh DB) 0 HIGH/CRITICAL with 3 time-boxed ignores (exp 2027-01-17, user-accepted); Semgrep/Gitleaks run in CI.
- Env note: local tabib-ma_pgdata password reset to match .env (changed 2026-11-06 after volume init), user-approved option A.
- Status: pass
