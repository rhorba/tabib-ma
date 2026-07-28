# Tech Lead — ADR Templates & Technical Decision Reference

Load when documenting a significant technical decision or running a technical review.

---

## ADR Template

```markdown
## ADR-[N]: [Short imperative title]

Status: Proposed | Accepted | Deprecated | Superseded by ADR-[N]
Date: [YYYY-MM-DD]
Deciders: [names or roles]

Context:
[2-4 sentences. What situation forced this decision? What constraints exist?]

Decision:
[The actual decision, stated as a command: "We will use X because Y."]

Options Considered:
| Option | Pros | Cons |
|--------|------|------|
| A: [option] | [pros] | [cons] |
| B: [option] | [pros] | [cons] |

Consequences:
- Good: [positive outcomes]
- Bad: [trade-offs accepted]

Review Trigger: [When to revisit — e.g., "If DAU exceeds 1M"]
```

---

## Common ADR Examples

### Auth Strategy
```
ADR-001: Use JWTs for API authentication
Context: SPA needs stateless auth across 3 services. No existing auth infra.
Decision: Short-lived JWTs (15 min) + refresh tokens in HttpOnly cookies.
          Revocation via token blocklist in Redis.
Rejected: Opaque session tokens (require shared session store),
          long-lived JWTs (cannot revoke on logout).
```

### Database Selection
```
ADR-002: Use PostgreSQL as primary database
Context: Relational data, ACID needed, team knows SQL. No proven NoSQL need.
Decision: PostgreSQL 16 on managed RDS.
Rejected: MongoDB (team unfamiliar, schema flexibility unproven need).
```

### API Design
```
ADR-003: REST over GraphQL for v1 API
Context: External partners integrating. Team = 2 backend devs. No dynamic query needs yet.
Decision: REST + OpenAPI spec. Revisit GraphQL if client patterns diverge.
Rejected: GraphQL (steeper learning curve, overfetch not yet a problem).
```

---

## Technical Debt Register

```markdown
# Technical Debt Register

| ID | Description | Category | Severity | Fix Effort | Impact if Ignored | Owner | Target |
|----|-------------|----------|----------|------------|-------------------|-------|--------|
| TD-01 | No integration tests on payment flow | Testing | High | 3 days | Silent regressions | QA | Sprint 8 |
| TD-02 | N+1 queries on /orders | Performance | Med | 1 day | Latency at scale | Backend | Sprint 9 |
| TD-03 | Hardcoded config in 3 files | Maintenance | Low | 2 hr | Env-specific bugs | Any | Sprint 10 |
```

**Rule**: High severity debt must have a fix plan within 2 sprints.

---

## Code Review Standards

### Always block merge on:
- Security issues: unescaped input, hardcoded secrets, broken auth
- Data loss risk: destructive operations without safeguards
- Untested business logic (80% coverage gate)
- Breaking API contract without versioning

### Advisory (suggestions, not blockers):
- N+1 queries, missing indexes
- Readability: confusing names, missing error handling
- YAGNI violations: unused abstractions

### PR Size Guidelines

| Lines Changed | Review Time | Action |
|---|---|---|
| < 200 | < 30 min | Ideal |
| 200-500 | 30-60 min | Acceptable |
| > 500 | > 60 min | Ask to split |
| > 1000 | Multiple sessions | Block until split |

---

## Stack Decision Matrix

```markdown
## Stack Decision: [Component]

Constraints: [team skills, existing infra, compliance, budget]

| Option | Learning Curve | Performance | Ecosystem | Maintenance | Score |
|--------|---------------|-------------|-----------|-------------|-------|
| A      | 3/5           | 4/5         | 5/5       | 4/5         | 16    |
| B      | 5/5           | 5/5         | 3/5       | 2/5         | 15    |

Recommendation: A — better ecosystem and maintenance long-term.
```

---

## Runbook Template

```markdown
# Runbook: [System Name]

## On-Call Escalation
1. [First responder] — PagerDuty rotation
2. [Secondary] — team Slack + mobile

## Alert → Action

### Alert: Error rate > 1%
Investigate:
  1. Check app logs: kubectl logs -l app=api --tail=100
  2. Check DB connections
  3. Check dependency health endpoints
Remediate:
  - Restart pod: kubectl rollout restart deployment/api
  - If DB: [failover steps]

## Rollback
  1. git log --oneline -5  (identify bad commit)
  2. Re-deploy previous image tag
  3. Notify stakeholders
```
