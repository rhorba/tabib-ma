# Workflow Templates

Quick-start paths for common scenarios. These templates fast-track phases — they do NOT skip them. Every numbered step below is MANDATORY. If a step is lightweight, complete it quickly and log it, then move on. Do not silently omit any step.

---

## 🐛 Bug Fix Workflow

**Duration**: 15-60 min | **Skills**: Tech Lead → Backend or Frontend (+ DBA if data issue) → Tester

```
1. REPRODUCE
   → User describes the bug
   → Locate the relevant code
   → If DB-related: DBA reviews queries/schema
   → Confirm: "I see the issue in [file:line]. Here's what's happening: [explanation]"

2. OPTIONS (simple → complex)
   🟢 Quick patch: [minimal change]
   🟡 Proper fix: [addresses root cause]
   🔴 Refactor: [fixes the pattern, not just this instance]
   → "Which approach?"

3. FIX
   → Apply the chosen fix
   → If migration needed: DBA writes migration
   → Show the diff briefly

4. TEST
   → Write/run test that would have caught this bug
   → Run existing tests to check no regressions
   → If auth/security related: Security Engineer quick review

5. DONE
   → "Fixed and tested. Ready to commit?"
```

---

## ✨ New Feature Workflow

**Duration**: 1-4 hours | **Skills**: PM → System Designer (large) → Software Architect → UX → Tech Lead → DBA → Backend/Frontend → UI → Tester → Security → Deployment

```
1. SCOPE (5 min)
   → What does the feature do? (1-2 sentences)
   → Who is it for?
   → What's the success criteria?
   → YAGNI check: what's the MINIMUM viable version?
   → ENV VARS: any new env vars needed? Collect values now.

2. ARCHITECTURE REVIEW (5-10 min — fast-track to 2 min for trivial features)
   → Software Architect: does this feature fit the existing module structure?
   → If large: System Designer reviews topology impact (new service? new queue?)
   → Define module boundaries, interfaces, and dependency rules for this feature
   → If no changes to existing arch: note "fits existing arch" and log it

3. UX DESIGN (10-20 min — fast-track to 2 min for pure backend features: confirm "no UI impact" and log it)
   → UX Designer: user flow for the feature
   → Key screens wireframed (text-based)
   → Error/empty states defined
   → Copywriter: microcopy if needed

4. TECHNICAL DESIGN (10 min)
   → Tech Lead: tech approach confirming Software Architect module boundaries
   → User picks approach
   → DBA: schema design if new data is involved
   → Security Engineer: quick threat check if auth/data involved
   → Define API contract / component structure

5. VISUAL DESIGN (10-15 min — fast-track to 1 min if using existing design system: confirm "using existing tokens" and log it)
   → UI Designer: design tokens or component styles
   → Only for new visual patterns, not existing ones

6. PLAN (5 min)
   → Scrum Master: break into batches
   📋 Batch 1: Data layer (migrations, models, API)
   📋 Batch 2: Business logic (services, validation)
   📋 Batch 3: UI (components, pages, integration)
   📋 Batch 4: Tests + coverage check (must hit ≥80%)

7. EXECUTE (batch by batch)
   → One task at a time
   → Checkpoint after each batch

8. VERIFY
   → Tester: run tests
   → Run coverage report → must be ≥ 80%
   → Security Engineer: quick review (if auth/data touched)
   → User acceptance
   📝 Log: coverage % to .logs/metrics.md

9. SHIP (mandatory — even if "ship" means "commit and tag for review")
   → Deployment: push to staging/production
   → git push origin <branch>
   📝 Log: PUSH + coverage % to .logs/activity.md
   → If this is a version release: record Playwright E2E video to .recordings/
   → If not deploying yet: log "SHIP deferred — reason: [X]" to .logs/activity.md
```

---

## 📝 Documentation Workflow

**Duration**: 15-60 min | **Skills**: Tech Lead → relevant specialist

```
1. WHAT TO DOCUMENT
   → README / API docs / Architecture / User guide / Setup guide?

2. GATHER
   → Read existing code/configs
   → Identify what's undocumented or outdated

3. WRITE
   → Follow the project's doc style
   → Code examples must be tested/runnable
   → Keep it scannable (headers, short paragraphs)

4. REVIEW
   → "Here's the draft. Anything to add or change?"
```

---

## 🚀 New Project Workflow

**Duration**: 2-6 hours | **Skills**: PM → System Designer → Software Architect → UX → Tech Lead → DBA → all devs → DevOps → Security → Tester

```
1. CHARTER (10 min)
   → PM: project charter (scope, goals, constraints)
   → YAGNI check: what's the absolute minimum to launch?

1a. ENV VARIABLES (5 min) ← MANDATORY before any code
   → Identify ALL env vars the project will need
   → Ask user for each value
   → Write to .env.example (never .env)
   📝 Log: ENV_VARS_COLLECTED to .logs/activity.md

2. SYSTEM DESIGN (15-20 min) ← ALWAYS run for new projects
   → System Designer: NFR capture (availability, latency, throughput, storage)
   → System Designer: component topology diagram
   → System Designer: integration patterns (sync/async, queues, cache)
   → User confirms topology before moving to software architecture

3. SOFTWARE ARCHITECTURE (15 min) ← ALWAYS run for new projects
   → Software Architect: architecture style selection (Layered / Clean / Hexagonal)
   → Software Architect: module/package structure
   → Software Architect: design patterns to apply
   → Software Architect: dependency rules
   → ADR written for key architectural decisions

4. UX FOUNDATION (15-20 min)
   → UX Designer: core user flows (top 3 journeys)
   → UX Designer: site map / information architecture
   → Copywriter: key UI copy (app name, tagline, CTAs)

5. TECH STACK + SECURITY (15 min)
   → Tech Lead: stack decision confirming System Designer + Software Architect choices
   → DBA: database selection and initial schema
   → Security Engineer: auth strategy, data protection requirements, threat model
   → User confirms

6. VISUAL FOUNDATION (10-15 min)
   → UI Designer: design tokens (colors, typography, spacing)
   → Or: pick a UI framework and move on (YAGNI)

7. SCAFFOLD (20 min)
   → Create project structure per Software Architect's module design
   → Setup configs (linting, formatting, git, dependency-cruiser for arch enforcement)
   → Initial dependencies
   → Docker/docker-compose for dev
   → DBA: initial migration files
   → DevOps: CI pipeline with coverage gate (fail if < 80%)

8. FOUNDATION (30-60 min)
   → Backend: DB setup, auth, base API following architecture patterns
   → Frontend: routing, layout, base components
   → DevOps: CI pipeline, basic security scanning

9. FIRST FEATURE (varies)
   → Switch to Feature Workflow above

10. DEPLOY SETUP + SPRINT CLOSE
    → DevOps: staging environment
    → Deployment: first deploy
    → git push origin <branch>
    📝 Log: PUSH to .logs/activity.md
```

---

## 📢 Marketing Launch Workflow

**Duration**: 1-3 hours | **Skills**: Digital Marketer → Copywriter → Content Marketer

```
1. STRATEGY (10 min)
   → Digital Marketer: campaign plan
   → Define audience, channels, goals

2. CONTENT CREATION (30-60 min)
   → Copywriter: landing page copy, CTAs, email copy
   → Content Marketer: blog post, social media posts

3. TECHNICAL SETUP (15 min)
   → Frontend Dev: implement landing page changes
   → Digital Marketer: analytics tracking, SEO

4. LAUNCH
   → Publish content
   → Send emails
   → Post to social media
   → Monitor initial metrics
```

---

## 🔒 Security Audit Workflow

**Duration**: 30-90 min | **Skills**: Security Engineer → DevOps/DevSecOps → DBA → Backend/Frontend

```
1. SCOPE (Security Engineer)
   → What's being audited? (full app / feature / infra)
   → YAGNI: match depth to risk level

2. THREAT MODEL (Security Engineer — 10 min)
   → STRIDE on key components
   → Attack surface analysis
   → Top risks identified

3. AUTOMATED SCAN (DevSecOps — 10 min)
   → Run SAST (Semgrep), SCA (Trivy), secrets scan (Gitleaks)
   → If Docker: container image scan
   → If IaC: Checkov scan
   → DBA: check DB access permissions, encryption

4. TRIAGE (Security Engineer — 10 min)
   → Classify findings: Critical / High / Medium / Low
   → Present to user: "Found X issues. Here are the critical ones:"

5. FIX (batch by severity)
   📋 Batch 1: Critical (fix now)
   📋 Batch 2: High (fix today)
   📋 Batch 3: Medium (track in backlog)

6. VERIFY
   → DevSecOps: re-run scans
   → Tester: confirm fixes don't break functionality
```

---

## 🔄 Refactor Workflow

**Duration**: 30-120 min | **Skills**: Tech Lead → Backend/Frontend → Tester

```
1. ASSESS
   → What needs refactoring and why?
   → Risk: what could break?
   → Scope: how much to change?

2. PLAN
   → Define the target state
   → Break into safe, incremental steps
   → Each step must leave code working

3. EXECUTE (step by step)
   → Make one change → run tests → confirm green
   → Repeat
   → Never make two structural changes at once

4. VERIFY
   → All tests pass
   → No behavior changes (unless intended)
   → Performance not degraded
```

---

## 🎨 UX/UI Design Workflow

**Duration**: 30-120 min | **Skills**: UX Designer → UI Designer → Frontend Dev → Copywriter

```
1. UNDERSTAND (5 min)
   → Who is the user? What's their goal?
   → YAGNI: do we need full personas or is a 1-liner enough?

2. FLOW (10-20 min)
   → UX Designer: happy path user flow
   → Error paths and edge cases
   → Screen states: empty, loading, error, success

3. WIREFRAME (10-20 min — fast-track to 2 min for trivial changes: state "no new layout — existing pattern reused" and log it)
   → Low-fi layout: content hierarchy, primary CTA
   → YAGNI: boxes + text only, no colors yet

4. VISUAL (10-20 min)
   → UI Designer: apply design tokens (colors, type, spacing)
   → YAGNI: use existing component library unless brand requires custom
   → Dark mode only if explicitly needed

5. COPY (5-10 min)
   → Copywriter: button labels, error messages, empty states
   → UX writing: help text, tooltips

6. HANDOFF → Frontend Dev
   → Specs: tokens used, states, responsive behavior
```

---

## 🗄️ Database Design Workflow

**Duration**: 15-60 min | **Skills**: DBA → Tech Lead → Backend Dev

```
1. REQUIREMENTS (5 min)
   → What data? What queries? How much volume?
   → YAGNI: don't design for scale you don't have

2. SCHEMA (10-20 min)
   → DBA: entities, relationships, table design
   → Normalize to 3NF by default
   → YAGNI: no denormalization until you've measured a real bottleneck

3. MIGRATION (5-10 min)
   → Write migration files
   → Ensure backward-compatible (nullable new columns first)

4. INDEXES (5 min)
   → Foreign keys + WHERE clause columns
   → YAGNI: don't index everything, add when queries are slow

5. VERIFY
   → Run migration on dev
   → Test key queries with EXPLAIN ANALYZE
```

---

## 🔐 Security Review Workflow

**Duration**: 20-60 min | **Skills**: Security Engineer → DevSecOps → Backend/Frontend

```
1. SCOPE (5 min)
   → What are we reviewing? (new feature / full app / infra)
   → YAGNI: match depth to risk level
     - Internal tool → quick checklist
     - Public app handling PII → full review

2. THREAT MODEL (10 min)
   → Security Engineer: 5-minute threat model
   → Identify top 3 threats
   → YAGNI: STRIDE only for high-risk components

3. REVIEW (10-20 min)
   → Security checklist (auth, input, data, infra)
   → Run automated scans if code exists

4. REMEDIATE
   → Fix critical/high issues
   → Backlog medium/low
   → YAGNI: don't add WAF for an internal dashboard

5. VERIFY
   → Re-scan, confirm fixes
```

---

## 🎨 UX/UI Design Workflow

**Duration**: 30-120 min | **Skills**: UX Designer → UI Designer → Frontend Dev

```
1. UNDERSTAND (5 min)
   → Who is the user?
   → What task are they completing?
   → YAGNI check: scale the depth, not the phases
     Small (button/modal)   → fast-track steps 2-3 (1 min each, note "minimal — no new flow/layout")
     Medium (page/feature)  → normal depth for steps 2-3
     Large (new product)    → full depth for all steps

2. FLOW (UX — 15 min)
   → User flow diagram (text-based)
   → Key screens identified
   → Error/empty states planned

3. WIREFRAME (UX — 15 min)
   → Text-based wireframe for key screens
   → Navigation + hierarchy decisions
   → Usability heuristic check

4. VISUAL (UI — 15-30 min)
   → Design tokens (or pick UI framework)
   → Component styling decisions
   → Responsive adaptations noted

5. HANDOFF → Frontend Dev
   → Flow + wireframe + visual specs
   → Interaction details (hover, focus, transitions)
   → Copy from Copywriter if needed
```

---

## 🗄️ Database Design Workflow

**Duration**: 15-60 min | **Skills**: DBA → Backend Dev → Security Engineer

```
1. REQUIREMENTS (5 min)
   → What entities? What relationships?
   → Read vs write heavy?
   → YAGNI: start with Postgres unless there's a real reason not to

2. SCHEMA (10-20 min)
   → Entity list → relationship map → table design
   → YAGNI: only tables/columns needed for current feature
   → Naming conventions applied

3. MIGRATION (5-10 min)
   → Migration file(s) with up + down
   → Test on dev data

4. INDEXES (5 min)
   → Index foreign keys + frequent WHERE columns
   → YAGNI: don't preemptively index everything

5. SECURITY CHECK
   → Encryption for sensitive columns?
   → Row-level security for multi-tenant?
   → Access permissions scoped?

6. HANDOFF → Backend Dev
   → Schema + migrations + query patterns
```

---

## 🔐 Security Review Workflow

**Duration**: 20-60 min | **Skills**: Security Engineer → DevSecOps → relevant dev

```
1. SCOPE
   → What's being reviewed? (feature / architecture / full app)
   → YAGNI: match security depth to project maturity

2. THREAT MODEL (Security Engineer)
   → STRIDE on relevant components only
   → Identify top 3-5 risks

3. REQUIREMENTS
   → Auth/authz design
   → Data protection needs
   → Logging requirements

4. SCAN (DevSecOps)
   → Run automated scanners
   → Triage findings by severity

5. REMEDIATE
   → Fix critical/high findings
   → Backlog medium/low

6. VERIFY
   → Re-scan
   → Confirm fixes
```

---

## 💡 Brainstorm / Ideation Workflow

**Duration**: 15-45 min | **Skills**: Creative Intelligence → PM or Tech Lead

```
1. FRAME (5 min)
   → What are we exploring? (feature idea / product direction / problem solving)
   → Any constraints? (time, budget, tech, audience)
   → YAGNI: quick brainstorm or full design thinking workshop?

2. DIVERGE (10-15 min — Creative Intelligence)
   → Pick framework: Brainstorming / SCAMPER / Design Thinking / First Principles
   → Generate 10+ ideas, no filtering
   → "Yes, and..." mindset

3. CLUSTER (5 min)
   → Group ideas by theme
   → Identify patterns

4. CONVERGE (5 min)
   → Score ideas: Impact × Confidence / Effort
   → Present top 3 ranked options
   → "Which resonates? Or combine elements?"

5. VALIDATE (5 min)
   → Quick feasibility check with Tech Lead
   → Quick user fit check with UX Designer
   → Decision logged to .logs/decisions.md

6. HANDOFF
   → If building: → PM for scope → normal feature workflow
   → If pitching: → Copywriter/Content for narrative
```

---

## 📋 Document-First Build Workflow

**Duration**: Session 1 — all docs (1-3 hrs) | Session 2+ — execution | **Skills**: PM → System Designer → Software Architect → Security → DBA → UX → UI → Test Architect → DevOps → Scrum → devs

Use for new projects and large features (3+ days of work).
Read `references/document-chain.md` for full templates for every artifact.

> **FIRST SESSION RULE**: All expert docs below MUST be generated, user-approved, and pushed in Session 1 before any code is written. Do NOT skip any specialist doc.

```
SESSION 1 — Foundation (all docs, no code):

0. ENV VARIABLES (5 min) ← BEFORE anything else
   → Identify ALL env vars the project will need
   → Ask user for each value now
   → Write to .env.example (never .env)
   📝 Log: ENV_VARS_COLLECTED to .logs/activity.md

1. PRD (PM — 15-20 min)
   → Problem, goals, user stories, scope, requirements, risks, timeline
   → Save to docs/prd-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "PRD approved" → .logs/activity.md

2. SYSTEM DESIGN (System Designer — 15 min)
   → NFR capture: availability, latency, throughput, data volume, RTO/RPO
   → Component topology diagram (text-based)
   → Integration patterns, scalability strategy, SDRs
   → Save to docs/system-design-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "System design approved"

3. SOFTWARE ARCHITECTURE (Software Architect — 15 min)
   → Architecture style (Layered / Clean / Hexagonal / DDD)
   → Module/package structure and dependency rules
   → Design patterns, cross-cutting concerns, fitness functions
   → Save to docs/architecture-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "Architecture approved"
   📝 Log: ARCHITECTURE decisions → .logs/decisions.md

4. SECURITY BASELINE (Security Engineer — 10-15 min)
   → 5-minute threat model + STRIDE top risks
   → Auth strategy, authorization model, data protection plan
   → Security requirements handed to dev team
   → Save to docs/security-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "Security baseline approved"

5. DATABASE DESIGN (DBA — 10-15 min)
   → Database engine selection (YAGNI default: PostgreSQL)
   → Entity-relationship model, schema design, index strategy
   → Migration plan, access patterns, sensitive data fields
   → Save to docs/database-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "Database design approved"

6. UX FOUNDATION (UX Designer — 15-20 min)
   → Minimal personas, information architecture / site map
   → Core user flows (top 3 journeys), key screen wireframes (text-based)
   → Screen states: empty, loading, error, success
   → Save to docs/ux-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "UX foundation approved"

7. UI FOUNDATION (UI Designer — 10-15 min)
   → Design approach: UI framework or custom tokens (YAGNI)
   → Design tokens if custom; component inventory; responsive breakpoints
   → Accessibility baseline
   → Save to docs/ui-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "UI foundation approved"

8. TEST STRATEGY (Test Architect — 10-15 min)
   → Risk assessment per component
   → Test pyramid targets (combined ≥ 80% — non-negotiable gate)
   → ATDD acceptance scenarios for critical paths
   → Adversarial checklist for high-risk components
   → Release gate criteria
   → Save to docs/test-strategy-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "Test strategy approved"

9. DEVOPS FOUNDATION (DevOps/DevSecOps — 10-15 min)
   → Environment strategy (local / staging / prod)
   → CI pipeline stages: lint → test (coverage gate) → security scan → build → deploy
   → Infrastructure, secrets management, monitoring baseline
   → Save to docs/devops-[name].md
   → User approves before proceeding
   📝 Log: MILESTONE "DevOps foundation approved"

10. STORIES (Scrum Master + Test Architect — 10-15 min)
    → Epics → stories with ATDD acceptance criteria from Test Architect
    → Each story references PRD requirement + architecture decision
    → Save to docs/stories-[name].md
    → User approves before proceeding
    📝 Log: MILESTONE "Stories ready"

11. COMMIT ALL DOCS + PUSH (mandatory end of Session 1)
    git add docs/
    git commit -m "docs: foundation documents for [project-name]"
    git push origin <branch>
    📝 Log: PUSH → .logs/activity.md

SESSION 2+ — Execution:

12. EXECUTE (normal batch execution)
    → Follow the stories, batch by batch
    → Each story references PRD requirement + architecture decision
    → Test Architect validates acceptance criteria pass

13. VERIFY
    → Test Architect: adversarial review on high-risk components
    → Traceability check: every requirement → story → test → code
    → Coverage report: must be ≥ 80%
    📝 Log: coverage % to .logs/metrics.md

14. SHIP + SPRINT CLOSE
    → git push origin <branch>
    → If version release: Playwright video recording to .recordings/
    📝 Log: PUSH + VIDEO_RECORDED to .logs/activity.md
```

---

## 🧪 Test Strategy Workflow

**Duration**: 20-60 min | **Skills**: Test Architect → Tester → Security Engineer

```
1. RISK ASSESSMENT (Test Architect — 10 min)
   → Score each component: impact × frequency × complexity
   → Classify: maximum / high / standard / minimal testing
   → Set coverage targets per risk level (floor: ≥ 80% combined unit + integration)

2. ATDD SPECIFICATIONS (Test Architect — 10-15 min)
   → Write Gherkin acceptance scenarios for critical paths
   → User reviews and approves scenarios
   → These become the test suite

3. TEST PLAN (Test Architect — 5 min)
   → Map: unit / integration / e2e / adversarial per component
   → Set release gate criteria (coverage ≥ 80% is non-negotiable)
   → Hand off to Tester for implementation

4. ADVERSARIAL REVIEW (Test Architect — 10-20 min)
   → Run adversarial checklist on high-risk components
   → Input abuse, auth abuse, race conditions, business logic
   → Log findings to .logs/issues.md

5. EDGE CASE HUNT (Test Architect — 5-10 min)
   → Apply boundary/type/time/state/network categories
   → Document edge cases as additional test cases

6. REPORT
   → Traceability matrix: requirement → test → status
   → Release readiness assessment
   → Coverage report attached (must show ≥ 80%)
   📝 Log: SPRINT_SNAPSHOT with test metrics + coverage %
```

---

## 🎬 Video Recording Workflow

**When**: At every project version completion (major release, milestone sprint, or first deploy)
**Skills**: Tester → Frontend Dev (if UI changes) → Deployment

```
1. SETUP (5 min)
   → Ensure Playwright is installed and configured with video recording:
     playwright.config.ts: use: { video: 'on' }
   → Target environment: staging or local (never prod)
   → Output directory: .recordings/

2. SCENARIO LIST (Test Architect — 5 min)
   → Define scenarios to record (all critical user flows):
     ✅ Happy path: [main user journey]
     ✅ Auth: sign up, login, logout, password reset
     ✅ Core feature flows: [list per project]
     ✅ Error states: 404, form validation, API errors
     ✅ Edge cases: empty state, long content, mobile viewport

3. RECORD (Tester — 10-20 min)
   → Run: npx playwright test --reporter=html
   → Playwright saves .webm files per test automatically
   → Name files: .recordings/v[version]-[YYYY-MM-DD]-[scenario].webm
   → Or use one recording: .recordings/v[version]-[YYYY-MM-DD]-full.webm

4. REVIEW
   → Watch recording to verify all scenarios executed correctly
   → If a scenario failed visually: fix → re-record

5. LOG
   📝 Log: VIDEO_RECORDED → .logs/activity.md
   Entry format:
     VIDEO_RECORDED: v[version] on [date]
     File: .recordings/v[version]-[date]-full.webm
     Scenarios: [list]
     Duration: [total recording duration]
```
