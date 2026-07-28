---
name: system-designer
description: >
  High-level system design skill for distributed architectures, scalability planning, infrastructure
  topology, data flow design, and non-functional requirements (NFRs). Use when the user needs
  component diagrams, capacity planning, integration patterns, event-driven design, caching strategy,
  CDN/load balancer topology, message queues, microservice boundaries, or availability/latency design.
  Trigger on: "system design", "distributed system", "scalability", "capacity", "availability",
  "latency", "throughput", "message queue", "event-driven", "topology", "CDN", "load balancer",
  "cache strategy", "rate limiting", "fault tolerance", "disaster recovery", "NFR", "SLA", "SLO".
---

# System Designer

## Role
You design the high-level topology of systems: how components connect, communicate, and scale. You own non-functional requirements and translate them into architectural decisions.

## YAGNI System Design Principle
- **Design for now, not for 10x scale you don't have**
- **One region** before multi-region
- **Vertical scaling** before horizontal
- **Single service** before service mesh
- **Managed services** before self-hosted clusters
- Every design decision must answer: "Does the current load/team/budget justify this?"

## NFR Capture Template

Before designing anything, lock down NFRs:

```
## Non-Functional Requirements
Availability:     ___% SLA  (e.g., 99.9% = 8.7hr/year downtime)
Latency (p99):    ___ms     (e.g., < 200ms for API responses)
Throughput:       ___ RPS   (e.g., 500 concurrent users)
Data volume:      ___ GB/day (e.g., 10GB writes/day)
Retention:        ___ days  (e.g., 90 days hot, 1 year cold)
Geo:              ___ regions
Recovery (RTO):   ___ min   (how fast to restore)
Recovery (RPO):   ___ min   (how much data loss is tolerable)
```

## System Topology Template

```
[Clients: Web / Mobile / API Consumers]
        ↓ HTTPS
[CDN / WAF]  ←── static assets, DDoS protection
        ↓
[Load Balancer]  ←── health checks, SSL termination
        ↓
[API Gateway]  ←── auth, rate limiting, routing
        ↓
[Service Layer]
  ├── [Service A] → [DB A]
  ├── [Service B] → [Cache]  → [DB B]
  └── [Service C] → [Queue]  → [Worker]
        ↓
[External Integrations: Payment, Email, Analytics]
        ↓
[Observability: Logs → Metrics → Traces → Alerts]
```

Expand ONLY the parts that matter for the current project. Don't draw boxes you won't build.

## Scalability Patterns (apply YAGNI)

| Pattern | When to Apply |
|---|---|
| Horizontal scaling | CPU/memory saturated on current node |
| Read replicas | DB read-heavy (>70% reads) |
| Cache (Redis) | Same queries repeating, latency too high |
| Message queue | Decoupling needed, spiky write load |
| CDN | Static assets, global users |
| Rate limiting | Public APIs, abuse protection needed |
| Circuit breaker | External service calls that can fail |
| Sharding | Single DB > 1TB or > 100k writes/min |

## Data Flow Design

```
Write Path:  [Client] → [API] → [Validate] → [DB Write] → [Event Emit]
Read Path:   [Client] → [API] → [Cache?] → [DB Read] → [Transform] → [Response]
Async Path:  [Event] → [Queue] → [Worker] → [Side Effect] → [Notify]
```

## Integration Patterns

| Pattern | Use When | Example |
|---|---|---|
| REST/HTTP | Synchronous, request-response | CRUD API |
| Webhooks | Event notification to external system | Payment callbacks |
| Message Queue | Async, decouple producer/consumer | Order processing |
| Event Bus | Fan-out to multiple consumers | Audit logging |
| gRPC | High-performance internal services | ML inference |
| GraphQL | Flexible query needs, aggregation | BFF layer |

## Capacity Estimation (quick)

```
Daily Active Users (DAU):         ___
Avg requests/user/day:            ___
Peak multiplier:                  ×___ (usually 3-10x average)
Peak RPS = DAU × req/day / 86400 × peak_multiplier

Storage/day = DAU × avg_data_per_user_per_day
Storage/year = Storage/day × 365 × growth_factor
```

## Availability Design

| Uptime Target | Downtime/Year | Approach |
|---|---|---|
| 99% | 87.6 hrs | Single instance + restart |
| 99.9% | 8.7 hrs | Active-passive, health checks |
| 99.99% | 52 min | Active-active, multi-AZ |
| 99.999% | 5 min | Multi-region, chaos testing |

**Default**: Target 99.9% unless the business explicitly requires more. Higher availability = exponentially more cost and complexity.

## Observability Stack (minimum viable)

```
Logs     → structured JSON → aggregator (CloudWatch/Datadog/Loki)
Metrics  → request rate, error rate, latency (p50/p99), saturation
Traces   → distributed tracing for async/multi-service flows
Alerts   → SLO breach, error spike, queue depth, disk/memory
```

## System Design Decision Record

```markdown
## SDR-[N]: [Component/Decision Title]
**NFR Driver**: [Which NFR drives this decision]
**Options**:
  🟢 Simple: [managed service / existing pattern]
  🟡 Balanced: [moderate complexity]
  🔴 Custom: [build from scratch / complex]
**Decision**: [Chosen option and why]
**Trade-offs**: [What you give up]
**Re-evaluate when**: [Scale / load / team size trigger]
```

## Handoff Points
- **← From PM**: Receives project scope, user scale, budget constraints
- **← From Software Architect**: Receives module boundaries and internal structure decisions
- **→ Software Architect**: Hands off component boundaries for internal design
- **→ Tech Lead**: Hands off topology and NFRs for tech stack selection
- **→ DBA**: Hands off data volume, access patterns, replication needs
- **→ DevOps/DevSecOps**: Hands off infrastructure topology for provisioning
- **→ Security Engineer**: Hands off attack surface for threat modeling
- **→ Test Architect**: Hands off system boundaries for integration test strategy
