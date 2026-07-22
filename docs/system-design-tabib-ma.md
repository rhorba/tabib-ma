# System Design: Tabib.ma
**PRD Reference**: docs/prd-tabib-ma.md | **Version**: 1.0 | **Date**: 2026-07-21 | **Author**: System Designer | **Status**: Draft

## 1. Non-Functional Requirements (locked from PRD)

```
Availability:     99.9% SLA on booking/payment paths (8.7hr/year downtime budget)
Latency (p99):    < 1.5s search, < 300ms round-trip for video signaling
Throughput:       Design for 1,500 doctors, ~50k patients by month 12 (10x current MVP target)
Data volume:      < 1 GB/day writes at launch (bookings, consult records, prescriptions)
Retention:        Consultation/prescription records retained 5 years (Moroccan medical record norms) — TBD confirm with legal
Geo:              1 region (Morocco/EU-West) for v1 — no multi-region
Recovery (RTO):   < 1 hour
Recovery (RPO):   < 15 minutes (point-in-time DB restore)
```

## 2. YAGNI Baseline Decisions
- **Modular monolith**, not microservices — team size and current load don't justify service-mesh complexity or the operational overhead of independently deployed services.
- **Single Postgres instance** (with read replica planned for month 6+), not sharded — data volume is well under sharding thresholds.
- **Vertical scaling first** — a single Spring Boot instance behind a load balancer handles projected load; horizontal scaling (2+ instances) added when CPU/memory saturates, not preemptively.
- **Single region** — Morocco's user base doesn't need multi-region; adds RTO/RPO complexity with no benefit yet.
- **Managed services where possible** — managed Postgres, managed Redis, managed TURN (or a well-known provider like Twilio/coturn-as-a-service) over self-hosting, to keep DevOps surface small.

## 3. System Topology

```
[Clients: Patient/Doctor/Clinic/Admin Web — React SPA]
        ↓ HTTPS
[CDN]  ←── static assets (React build), WAF-lite (rate limit, basic bot filtering)
        ↓
[Load Balancer]  ←── SSL termination, health checks
        ↓
[Spring Boot API (modular monolith)]
  ├── Identity/Auth module        → [Postgres: users, roles, sessions]
  ├── Booking module              → [Postgres: appointments, availability] + [Redis: availability cache]
  ├── Payment module              → [CMI Payment Gateway] (webhook callback)
  ├── Consultation module         → [WebRTC signaling] → [TURN/STUN server]
  ├── Prescription module         → [Postgres: prescriptions] → [PDF generation, S3-compatible object storage]
  ├── Notification module        → [Async queue: Spring Events → SMTP / SMS Provider]
  ├── Clinic/Admin module        → [Postgres: clinics, verification docs]
        ↓
[External Integrations: CMI, SMTP, SMS Provider, TURN provider]
        ↓
[Observability: Structured logs → Loki/CloudWatch, Metrics → Prometheus/Grafana, Alerts]
```

**Note**: All modules live in one deployable Spring Boot artifact for v1 (see Architecture doc for internal boundaries). This is not a set of microservices — module boundaries are enforced at the code/package level, not the network level.

## 4. Capacity Estimation

```
Assumptions (month 6 target):
  Daily Active Users (DAU):        ~3,000 (patients + doctors combined)
  Avg requests/user/day:           ~15 (search, view profile, booking actions)
  Peak multiplier:                 5x (morning booking rush, evening browsing)

  Peak RPS = 3,000 × 15 / 86,400 × 5 ≈ 2.6 RPS sustained, bursts to ~15-20 RPS
  → A single Spring Boot instance (2-4 vCPU) comfortably handles this. Re-evaluate at 10x this DAU.

Storage:
  ~500 bookings/day × ~2KB/record ≈ 1MB/day booking data
  Prescription PDFs: ~200/day × ~150KB ≈ 30MB/day → ~11GB/year (object storage, not DB)
  Video: not recorded/stored by default (privacy-first — see Security doc) → no storage cost
```

## 5. Data Flow Design

### Booking + Payment (write path — highest criticality)
```
[Patient clicks "Book"] → [API: validate slot still available]
  → [DB: acquire row lock on availability slot] → [DB: create appointment (status=PENDING_PAYMENT)]
  → [CMI: create payment session] → [Patient redirected to CMI] → [CMI webhook: payment result]
  → [API: verify webhook signature] → [DB: update appointment status=CONFIRMED or PAYMENT_FAILED]
  → [Notification module: emit BookingConfirmed event] → [Async: SMS + email sent]
```
Row-level locking (`SELECT ... FOR UPDATE`) on the availability slot prevents double-booking under concurrent requests — this is the #1 correctness risk in the whole system and is called out explicitly for the DBA and Backend Dev.

### Search (read path)
```
[Patient search query] → [API] → [Redis cache check: specialty+city+date key]
  → cache miss → [Postgres: indexed query on doctors + availability] → [cache result, 60s TTL] → [Response]
```
No Elasticsearch/dedicated search engine for v1 — Postgres with proper indexes (specialty, city, GIN on searchable text) is sufficient at this data volume. Re-evaluate if catalog exceeds ~50k doctors or free-text search quality becomes a complaint.

### Video Consultation
```
[Doctor + Patient join at scheduled time] → [API issues short-lived WebRTC signaling tokens]
  → [Peer-to-peer connection attempt] → [STUN for NAT discovery]
  → [fallback to TURN relay if P2P fails] → [Session established]
  → [On disconnect/end] → [API marks consultation status=COMPLETED, unlocks prescription entry]
```
No media server (SFU/MCU) needed for 1:1 consultations — P2P WebRTC with TURN fallback is sufficient. Re-evaluate only if group consultations (multiple specialists) become a requirement.

### Async Notifications
```
[Domain event (BookingConfirmed, BookingCancelled, ReminderDue)] → [Spring Application Event]
  → [In-process async listener] → [SMS/Email provider API call]
```
In-process async (Spring `@Async` + `@TransactionalEventListener`) is used instead of a message broker (RabbitMQ/Kafka) — throughput doesn't justify broker operational overhead yet. Re-evaluate if notification volume causes API latency impact or a broker's durability guarantees become necessary.

## 6. Integration Patterns

| Integration | Pattern | Notes |
|---|---|---|
| CMI Payment | Webhook + signature verification | Idempotency key required — CMI may retry webhooks |
| SMS Provider | REST/HTTP, sync call from async listener | Circuit breaker (Resilience4j) — don't let SMS outage block bookings |
| Email (SMTP) | REST/HTTP via provider (not raw SMTP) | Same circuit breaker treatment as SMS |
| WebRTC/TURN | STUN/TURN protocol, short-lived credentials | Credentials scoped per-session, expire after consultation window |
| Object storage (prescriptions) | S3-compatible API | Signed URLs for patient download, no public bucket access |

## 7. Availability Design
**Target: 99.9%** (active-passive to start).
- Load balancer health checks against `/actuator/health`.
- Single Spring Boot instance at launch; horizontal scale-out (2+ instances behind LB) triggered by CPU > 70% sustained or the capacity re-evaluation above.
- Postgres: managed instance with automated backups (point-in-time recovery, RPO < 15 min) + a read replica added at month 6 for reporting/search read offload, not for HA failover yet.
- Payment and booking paths are the highest-priority for uptime — circuit breakers isolate SMS/Email/TURN provider failures from blocking a booking.

## 8. Observability Stack
```
Logs     → structured JSON (Logback + Logstash encoder) → aggregator (self-hosted Loki or managed, DevOps decides)
Metrics  → Micrometer → Prometheus (request rate, error rate, p50/p99 latency, JVM/DB pool saturation)
Traces   → not required at v1 scale (single monolith, no cross-service hops) — re-evaluate if microservices split ever happens
Alerts   → booking failure rate spike, payment webhook failures, DB connection pool exhaustion, disk/memory thresholds
```

## 9. System Design Decision Records

### SDR-1: Modular Monolith vs. Microservices
**NFR Driver**: Team size, current throughput (~3 RPS avg), budget
**Options**:
- 🟢 Simple: Modular monolith, single deployable
- 🟡 Balanced: Monolith with 1-2 extracted services (e.g., video signaling) if load demands
- 🔴 Custom: Full microservices (identity, booking, payment, consultation, notification as separate services)
**Decision**: Modular monolith. Throughput and team size don't justify microservices operational cost.
**Trade-offs**: Less independent scalability/deployability per module — acceptable at this scale.
**Re-evaluate when**: Any single module needs independent scaling (e.g., video signaling under heavy concurrent load) or team grows past ~15 engineers with deployment coordination pain.

### SDR-2: Search Implementation
**NFR Driver**: Catalog size (~150-1,500 doctors), latency < 1.5s p95
**Options**:
- 🟢 Simple: Postgres with indexes + Redis cache
- 🟡 Balanced: Postgres full-text search (tsvector) for free-text doctor bios
- 🔴 Custom: Dedicated search engine (Elasticsearch/Meilisearch)
**Decision**: Postgres + Redis cache (🟢), with `tsvector` for bio/name free-text if simple `ILIKE` proves insufficient.
**Trade-offs**: Less relevance-tuning power than a dedicated search engine.
**Re-evaluate when**: Catalog exceeds ~50k doctors or search relevance complaints become frequent.

### SDR-3: Video Infrastructure — Build vs. Buy
**NFR Driver**: Video quality NFR (< 300ms latency, audio-fallback), team bandwidth
**Options**:
- 🟢 Simple: Self-managed WebRTC (P2P + coturn TURN server)
- 🟡 Balanced: Managed WebRTC platform (Twilio Video, Daily.co, Vonage)
- 🔴 Custom: Self-hosted SFU (mediasoup/Janus) for future group-consult features
**Decision**: 🟡 Managed WebRTC platform for v1 — video reliability is core to the product's trust, and the team shouldn't spend Sprint 1-4 debugging NAT traversal edge cases across Moroccan ISPs. Final vendor selection is a DevOps/Tech Lead decision informed by CMI-region latency testing.
**Trade-offs**: Recurring per-minute cost vs. self-hosted; some vendor lock-in.
**Re-evaluate when**: Video minutes/month make managed pricing exceed self-hosting cost by a wide margin, or group consultations require an SFU anyway.

### SDR-4: Notification Delivery — Sync/In-Process vs. Message Broker
**NFR Driver**: Current notification volume (~500-1000/day), booking-path latency
**Options**:
- 🟢 Simple: In-process async (Spring `@Async` events)
- 🟡 Balanced: Lightweight queue (e.g., managed SQS-equivalent) for durability
- 🔴 Custom: Kafka/RabbitMQ event bus
**Decision**: 🟢 In-process async for v1.
**Trade-offs**: Notification loss possible if the instance crashes mid-send (acceptable — reminders can be regenerated from appointment state; confirmations are not the sole record of booking, the DB row is).
**Re-evaluate when**: Notification volume grows 10x+, or a durability guarantee becomes a compliance requirement.

## 10. Handoff Points
- **→ Software Architect**: Module boundaries above (Identity, Booking, Payment, Consultation, Prescription, Notification, Clinic/Admin) become the internal package structure.
- **→ DBA**: Data volume, read/write patterns (booking is write-heavy + lock-sensitive; search is read-heavy + cacheable), retention requirements.
- **→ Security Engineer**: Attack surface — CMI webhook endpoint, TURN credential issuance, patient PII/health data at rest.
- **→ DevOps/DevSecOps**: Topology above for Docker/infra provisioning; managed WebRTC vendor decision needed early (SDR-3).
- **→ Test Architect**: System boundaries (module seams) for integration test strategy; booking concurrency (double-booking prevention) is the top scenario to stress-test.
