# System Designer — Non-Functional Requirements Catalog

Load when defining SLAs, designing system topology, or setting quality gates.

---

## NFR Categories

```
Reliability    — does it stay up and recover from failure?
Performance    — is it fast enough under load?
Scalability    — can it grow without redesign?
Security       — is it protected? (see security-engineer/references/)
Maintainability — can the team evolve it?
Observability  — can you tell when it breaks?
Compliance     — does it meet legal/regulatory requirements?
```

---

## Availability SLA Targets

| SLA | Max Downtime / Month | Strategy |
|---|---|---|
| 99% | 7.3 hours | Single instance + restart |
| 99.9% | 43 minutes | Health checks + auto-restart |
| 99.95% | 22 minutes | Active-passive failover |
| 99.99% | 4.3 minutes | Active-active, multi-AZ |
| 99.999% | 26 seconds | Multi-region, chaos engineering |

**Recovery targets:**
- **RTO** — how fast must you recover?
- **RPO** — how much data loss is acceptable?

---

## Performance Targets by Request Type

| Request Type | P50 | P95 | P99 |
|---|---|---|---|
| Simple read (cached) | < 10 ms | < 50 ms | < 100 ms |
| Simple read (DB) | < 50 ms | < 200 ms | < 500 ms |
| Write (single record) | < 100 ms | < 300 ms | < 1 s |
| Complex query / report | < 500 ms | < 2 s | < 5 s |
| Background job | — | — | < 30 s |

---

## Scalability Patterns

| Pattern | When | Trade-off |
|---|---|---|
| Vertical scaling | Simple, stateful services | Hard ceiling |
| Horizontal scaling | Stateless services | Requires shared state (Redis/DB) |
| Read replicas | Read-heavy, eventual consistency OK | Replication lag |
| Connection pooling | Many short-lived connections | PgBouncer / RDS Proxy |
| Caching layer | Repeated identical reads | Cache invalidation complexity |
| Partitioning | Table > 50M rows | Query complexity |

---

## Observability Stack (Three Pillars)

```
Logs    → structured JSON, correlation ID, no PII
Metrics → request duration (histogram), error rate, DB latency
Traces  → distributed request tracing, W3C propagation
```

**Minimum viable alerts:**
```
error_rate > 1% for 5 min           → page on-call
p95 latency > 2x baseline for 10 min → page on-call
availability < 99.9% rolling 1hr   → page on-call
```

---

## System Topology Templates

### Simple Web App (< 10K users/day)
```
Browser → CDN → Load Balancer → App Servers → Postgres
                                             → Redis (cache/sessions)
```

### Multi-Service (separate scaling proven)
```
Browser → API Gateway → Auth Service
                     → Product Service → Postgres
                     → Order Service  → Postgres → Queue → Worker
```

### High-Read Platform
```
Browser → CDN → API Gateway → Read Service  → Redis → Postgres Replica
                            → Write Service → Postgres Primary
                            → Search        → Elasticsearch
```

---

## Capacity Estimation Worksheet

```
DAU:                         [N]
Requests per user/day:       [R]
Peak RPS (10× avg):          (N × R) / 86400 × 10

DB connections needed:       app_instances × pool_size
  If total > 200 → add PgBouncer

1-year storage:              daily_writes × 365 × retention_multiplier
```

---

## NFR Definition Template

```markdown
## NFR: [Name]
Category: [Reliability / Performance / Scalability / ...]
Requirement: [specific, measurable — e.g., "P95 < 500ms at 1,000 concurrent users"]
Measurement: [load test tool + scenario]
Consequence if missed: [user / business impact]
Current baseline: [measured today]
Target date: [when this must hold]
```
