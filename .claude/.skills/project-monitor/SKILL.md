---
name: project-monitor
description: >
  Project monitoring and logging skill that tracks all team activity, decisions, issues, risks,
  corrections, and communications. Creates structured log files for KPI measurement, retrospectives,
  and session resumption. Use this skill whenever logging is needed, when the user asks "what did we do",
  "show me the status", "what's changed", "project report", "KPIs", "metrics", "retrospective",
  "what's been decided", "show activity", "what issues do we have", or when resuming a session.
  Also trigger on: "log this", "track this", "record this decision", "status report", "changelog",
  "audit trail", "project history", "what happened", or any monitoring/tracking request.
  This skill is ALSO called automatically by the orchestrator at key workflow points.
---

# Project Monitor

## Role
You are the project's memory. You track everything that happens — every decision, every bug, every
risk, every change to the plan. You make the invisible visible.

**YAGNI for Monitoring**: Don't log trivial actions (typo fixes, formatting). Log decisions, completions, issues, and changes. If it would matter in a retrospective, log it.

## Log Architecture

All logs live in `.logs/` at the project root. Each file is append-only markdown.

```
.logs/
├── activity.md          ← What was done (completed tasks, milestones)
├── decisions.md         ← Architecture/approach decisions (mini-ADRs)
├── issues.md            ← Bugs, errors, blockers (with status)
├── risks.md             ← Risks identified + mitigations
├── corrections.md       ← Plan changes, scope changes, pivots
├── communications.md    ← Key questions, answers, handoffs, user preferences
├── sessions.md          ← Session summaries (start/end snapshots)
└── metrics.md           ← KPI snapshots over time
```

## CRITICAL: Logging Protocol

### When to Log (automatic triggers)

The orchestrator and all specialists MUST log at these moments:

| Event | Log File | Who Triggers |
|---|---|---|
| Task completed | `activity.md` | Any specialist after finishing a task |
| User picks an approach | `decisions.md` | Orchestrator / Tech Lead |
| Bug found or fixed | `issues.md` | Tester / any dev |
| Blocker hit or resolved | `issues.md` | Any specialist |
| Risk identified | `risks.md` | Security / PM / Tech Lead |
| Plan changed | `corrections.md` | Scrum Master / PM |
| Scope added or removed | `corrections.md` | PM / Orchestrator |
| Key user preference noted | `communications.md` | Orchestrator |
| Handoff between specialists | `communications.md` | Orchestrator |
| Skill-to-skill consultation | `communications.md` | Any specialist asking another |
| New skill added or modified | `activity.md` | Orchestrator |
| Session starts | `sessions.md` | Orchestrator |
| Session ends / checkpoint | `sessions.md` | Orchestrator |
| Sprint/batch completed | `metrics.md` | Scrum Master |

### How to Log (entry format)

Every log entry follows this format. **Append only — never edit previous entries.**

```markdown
### [YYYY-MM-DD HH:MM] [CATEGORY] — [Short Title]
- **Specialist**: [who logged this]
- **Summary**: [1-2 sentences max]
- **Details**: [brief context if needed — skip if summary is enough]
- **Status**: open | in-progress | resolved | closed
- **Impact**: low | medium | high | critical
---
```

### Token-Efficient Logging Rules

1. **Log AFTER the action, not before** — don't waste tokens describing what you're about to do
2. **One entry = one action** — don't batch multiple events into one entry
3. **Summary is mandatory, details are optional** — keep it short
4. **Don't read logs to write logs** — just append blindly
5. **Read logs ONLY when**: resuming a session, generating reports, or user asks

## Log File Templates

### activity.md
```markdown
# Activity Log

### [date time] COMPLETED — [task name]
- **Specialist**: Backend Dev
- **Summary**: Created user registration endpoint with email validation
- **Status**: resolved
- **Impact**: medium
---

### [date time] MILESTONE — [milestone name]
- **Specialist**: Orchestrator
- **Summary**: Batch 1 (data layer) complete — 4/4 tasks done
- **Status**: resolved
- **Impact**: high
---
```

### decisions.md
```markdown
# Decision Log

### [date time] ARCHITECTURE — [decision title]
- **Specialist**: Tech Lead
- **Summary**: Chose PostgreSQL over MongoDB for primary database
- **Options considered**: PostgreSQL (chosen), MongoDB, MySQL
- **Rationale**: Relational data model, ACID transactions needed, team familiarity
- **Status**: resolved
- **Impact**: high
---

### [date time] APPROACH — [decision title]
- **Specialist**: Orchestrator
- **Summary**: User chose 🟡 BALANCED approach for auth implementation
- **Status**: resolved
- **Impact**: medium
---
```

### issues.md
```markdown
# Issue Log

### [date time] BUG — [issue title]
- **Specialist**: Tester
- **Summary**: Login returns 500 when email contains special characters
- **Location**: src/auth/controller.ts:45
- **Severity**: high
- **Status**: open
- **Impact**: high
---

### [date time] BLOCKER — [blocker title]
- **Specialist**: Backend Dev
- **Summary**: Cannot connect to external payment API — credentials missing
- **Workaround**: Using mock responses for now, need real creds from user
- **Status**: open
- **Impact**: critical
---
```

### risks.md
```markdown
# Risk Log

### [date time] SECURITY — [risk title]
- **Specialist**: Security Engineer
- **Summary**: API endpoints lack rate limiting — vulnerable to brute force
- **Probability**: high
- **Mitigation**: Add express-rate-limit middleware, 100 req/min per IP
- **Status**: in-progress
- **Impact**: high
---
```

### corrections.md
```markdown
# Corrections & Plan Changes

### [date time] SCOPE_CHANGE — [what changed]
- **Specialist**: PM
- **Summary**: User requested adding OAuth login — originally planned email/password only
- **Original plan**: Email/password auth
- **New plan**: Email/password + Google OAuth
- **Effort impact**: +2 tasks (~1 hour)
- **Status**: resolved
- **Impact**: medium
---

### [date time] PIVOT — [what pivoted]
- **Specialist**: Tech Lead
- **Summary**: Switched from REST to GraphQL after discovering complex nested data needs
- **Reason**: Frontend needed 5 separate REST calls for one page — GraphQL solves with 1 query
- **Status**: resolved
- **Impact**: high
---
```

### communications.md
```markdown
# Communications Log

### [date time] PREFERENCE — [what the user prefers]
- **Specialist**: Orchestrator
- **Summary**: User prefers TypeScript over JavaScript, wants strict mode
- **Status**: resolved
- **Impact**: medium
---

### [date time] HANDOFF — [from → to]
- **Specialist**: Orchestrator
- **Summary**: Backend Dev → Frontend Dev. API for /users CRUD is complete. Frontend needs login form + dashboard page.
- **Status**: resolved
- **Impact**: low
---

### [date time] INTER_SKILL — [skill A ↔ skill B]
- **Specialist**: [who initiated]
- **Summary**: Tech Lead asked DBA to review schema for performance. DBA recommended composite index on (user_id, created_at).
- **From**: Tech Lead
- **To**: DBA
- **Decision/Outcome**: Added composite index, expected 10x query improvement
- **Status**: resolved
- **Impact**: medium
---

### [date time] QUESTION — [topic]
- **Specialist**: Tech Lead
- **Summary**: Asked user about deployment target. Answer: Vercel for frontend, Railway for backend.
- **Status**: resolved
- **Impact**: medium
---

### [date time] SKILL_CHANGE — [what changed]
- **Specialist**: Orchestrator
- **Summary**: Added Creative Intelligence skill (brainstorming, design thinking, innovation)
- **Change type**: added | modified | removed
- **Files affected**: skills/creative-intelligence/SKILL.md
- **Routing updated**: Yes — orchestrator, CLAUDE.md, QUICKSTART.md
- **Status**: resolved
- **Impact**: medium
---
```

### sessions.md
```markdown
# Session Log

### [date time] SESSION_START
- **Context**: User wants to build a task management app
- **Resuming from**: Fresh start (no previous sessions)
- **Plan**: Brainstorm → plan → scaffold → first feature
---

### [date time] SESSION_END
- **Completed**: Project scaffold, DB schema, auth API (Batch 1 done)
- **In progress**: Frontend login form (Task 3.1)
- **Blocked**: Nothing
- **Next session**: Complete Batch 2 (frontend components)
- **Open issues**: 1 (rate limiting not yet added)
- **Open risks**: 1 (no HTTPS in dev — acceptable)
---
```

### metrics.md
```markdown
# Project Metrics

### [date time] SPRINT_SNAPSHOT — Sprint 1
- **Planned tasks**: 12
- **Completed**: 8
- **In progress**: 2
- **Blocked**: 1
- **Dropped**: 1
- **Velocity**: 8 tasks/sprint
- **Bugs found**: 3
- **Bugs fixed**: 2
- **Security issues**: 1 (resolved)
- **Scope changes**: 1 (OAuth added)
- **Estimated vs actual**: 6h planned → 7.5h actual (+25%)
---
```

## KPI Definitions

Track these metrics across sprints/sessions:

| KPI | Formula | Target |
|---|---|---|
| **Velocity** | Tasks completed per sprint/session | Stable or increasing |
| **Completion Rate** | Completed / Planned × 100 | ≥ 80% |
| **Bug Rate** | Bugs found / Tasks completed | < 0.5 |
| **Bug Fix Rate** | Bugs fixed / Bugs found × 100 | ≥ 90% |
| **Blocker Duration** | Time from blocker opened → resolved | < 1 session |
| **Scope Creep** | Scope changes / Original tasks × 100 | < 20% |
| **Estimation Accuracy** | Actual time / Estimated time | 0.8 - 1.2 |
| **Security Issue Rate** | Security issues / Tasks completed | < 0.2 |
| **Rework Rate** | Corrections / Completed tasks × 100 | < 15% |
| **Decision Speed** | Decisions made vs decisions pending | No stale decisions |

## Status Report (generate on demand)

When the user asks for status, read the log files and produce:

```markdown
## Project Status Report — [Date]

### Health: 🟢 On Track / 🟡 At Risk / 🔴 Blocked

### Progress
- **Tasks completed**: X / Y total (Z%)
- **Current batch**: Batch N — [description]
- **Current task**: [what's being worked on]

### Key Metrics
| KPI | Value | Trend |
|---|---|---|
| Velocity | X tasks/session | ↑↓→ |
| Completion rate | X% | ↑↓→ |
| Open bugs | X | ↑↓→ |
| Open risks | X | ↑↓→ |

### Open Issues (by severity)
1. 🔴 [critical issues]
2. 🟡 [high issues]
3. 🟢 [medium/low issues]

### Recent Decisions
- [last 3-5 decisions]

### Plan Changes Since Last Report
- [scope changes, pivots, corrections]

### Risks
| Risk | Status | Mitigation |
|---|---|---|
| [risk] | [status] | [action] |

### Next Steps
- [what's coming next]
```

## Session Resumption Protocol

When a new session starts:

1. **Check if `.logs/sessions.md` exists**
   - YES → Read the LAST `SESSION_END` entry only (not the whole file)
   - NO → Fresh project, create `.logs/` directory and all log files

2. **Present to user:**
   ```
   📋 Last session summary:
   - Completed: [what was done]
   - In progress: [what was started]
   - Blocked: [any blockers]
   - Next: [what was planned]
   
   "Continue from here, or start something new?"
   ```

3. **Don't read all log files** — only read specific logs if the user asks about issues, decisions, etc.

## Retrospective (generate on demand)

When user asks for a retrospective, read all logs and produce:

```markdown
## Retrospective — [Date Range]

### What Went Well ✅
- [derived from smooth completions, fast decisions]

### What Didn't Go Well ❌
- [derived from blockers, rework, missed estimates]

### Patterns Noticed 🔍
- [recurring issues, common blocker types]

### Action Items for Next Sprint 💡
- [concrete improvements based on data]

### Metrics Summary
| Metric | This Sprint | Last Sprint | Trend |
|---|---|---|---|
| Velocity | | | |
| Completion rate | | | |
| Bug rate | | | |
| Scope creep | | | |
```

## Handoff Points
- **← From Orchestrator**: Receives session start/end signals, milestone completions
- **← From ALL specialists**: Receives log entries at trigger points
- **→ PM**: Provides metrics, status reports, retrospective data
- **→ Scrum Master**: Provides velocity, completion rates for sprint planning
- **→ Orchestrator**: Provides session resumption context
- **→ User**: Provides status reports, dashboards, retrospectives on demand

## Reference Files & Tools

| File | Purpose | When to Read |
|---|---|---|
| `references/kpi-analysis.md` | KPI formulas, bash queries, trend analysis | Generating reports or analyzing metrics |
| `references/logging-examples.md` | Exact bash commands for token-efficient logging | First time logging, or as a quick reference |
| `templates/init-logs.sh` | Initialize `.logs/` directory | Project setup |
| `templates/generate-report.py` | Auto-generate status report from logs | When user asks for status/KPIs |
