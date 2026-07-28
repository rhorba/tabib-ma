# Project Manager — Risk, Charter & Status Templates

Load when starting a project, managing risk, or producing status reports.

---

## Project Charter Template

```markdown
# Project Charter: [Project Name]

## Summary
[2-3 sentences: what we're building, why, when it ships]

## Problem Statement
[What problem? Who has it? Cost of not solving it?]

## Goals & Success Metrics
| Goal | Metric | Target | Baseline |
|------|--------|--------|----------|
| [goal] | [metric] | [target] | [today] |

## Out of Scope
- [Explicitly list what this project will NOT do]

## Stakeholders
| Name | Role | RACI |
|------|------|------|
| [name] | Sponsor | Accountable |
| [name] | PO | Responsible |

## Timeline
| Milestone | Target Date | Owner |
|-----------|-------------|-------|
| Requirements locked | [date] | PO |
| Architecture approved | [date] | Tech Lead |
| MVP shipped | [date] | Dev team |

## Top Risks (see risk register for full list)
| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
```

---

## Risk Register

```markdown
# Risk Register — [Project Name]

| ID | Risk | Category | Probability | Impact | Severity | Owner | Mitigation | Status |
|----|------|----------|-------------|--------|----------|-------|------------|--------|
| R01 | Third-party API down | Technical | Med | High | High | Tech Lead | Mock + fallback | Open |
| R02 | Scope creep | Scope | High | Med | High | PM | Change control | Open |
```

### Risk Severity Matrix
```
           Low Impact  Med Impact  High Impact  Critical
High Prob │   Med    │   High   │    Crit    │   Crit  │
Med Prob  │   Low    │   Med    │    High    │   Crit  │
Low Prob  │   Low    │   Low    │    Med     │   High  │
```

### Response Strategies
| Strategy | When |
|---|---|
| Avoid | Eliminate by changing the plan |
| Mitigate | Reduce probability or impact |
| Transfer | Insurance, contracts, outsourcing |
| Accept | Low severity + mitigation cost > impact |

---

## RACI Matrix

```
R = Responsible  A = Accountable  C = Consulted  I = Informed

| Activity             | PM | PO | Tech Lead | Dev | QA |
|----------------------|----|----|-----------|----|-----|
| Requirements sign-off| C  | A  | C         | I  | I   |
| Architecture         | I  | I  | A         | R  | C   |
| Sprint planning      | R  | C  | C         | R  | R   |
| Release approval     | C  | A  | C         | I  | C   |
```

---

## Weekly Status Report

```markdown
# Status — [Project Name] — Week of [date]

Overall: Green / Yellow / Red

## Summary (2-3 sentences)

## Completed
- [item]

## Planned Next Week
- [item]

## Risks & Issues
| # | Description | Impact | Action | Owner | Due |
|---|-------------|--------|--------|-------|-----|

## Decisions Needed
- [Decision] — needed by [date] to avoid [consequence]

## KPIs
Stories done: [N]/[committed] | Velocity: [N]pts | Bugs: [opened]/[closed]
```

---

## Change Control Process

```
1. IDENTIFY  — document: what changes, why, impact estimate
2. ASSESS    — PM + Tech Lead: effort, timeline, cost delta
3. DECIDE    — sponsor approves/rejects within 48 hr
4. IMPLEMENT — update plan + backlog + charter
5. COMMUNICATE — notify all stakeholders
```

**Golden rule**: No scope change without explicit approval + timeline adjustment.

---

## Escalation Matrix

| Situation | First | Then | SLA |
|---|---|---|---|
| Blocker > 1 day | Tech Lead | PM | 24 hr |
| Budget overrun > 10% | PM | Sponsor | 48 hr |
| Schedule slip > 1 week | PM | Sponsor | 24 hr |
| Security incident | Security Lead | CISO + PM | Immediate |
