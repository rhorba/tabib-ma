# PRD: Tabib.ma — Telemedicine & Doctor Booking Platform
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: PM | **Status**: Draft

## 1. Problem Statement
Morocco has roughly 1 doctor per 1,600 people in rural areas, and urban specialists carry wait times of 3+ weeks. Patients have no reliable way to discover, compare, and book doctors online; clinics run appointment books by phone and paper, causing no-shows, double-booking, and lost revenue. Doctors — especially specialists split across multiple clinics — have no unified way to manage availability or reach patients outside their existing network. Tabib.ma is a three-sided marketplace connecting Patients, Doctors, and Clinics, with video consultation, e-prescription, and local payment (CMI) built in from day one.

## 2. Goals & Success Metrics
| Goal | Metric | Target (6 months post-launch) |
|---|---|---|
| Reduce time-to-appointment | Median days from search to booked slot | < 5 days (vs. 21+ today) |
| Prove marketplace liquidity | Active doctors with ≥1 booking/week | 150+ doctors |
| Drive booking completion | Search-to-booking conversion rate | ≥ 12% |
| Reduce no-shows | No-show rate on booked appointments | < 8% |
| Validate telemedicine adoption | % of appointments completed via video | ≥ 25% |
| Payment reliability | CMI transaction success rate | ≥ 98% |

## 3. Key Roles
- **Patient** — searches doctors/specialties, books appointments (in-person or video), pays via CMI, receives e-prescriptions, manages personal health record basics.
- **Doctor** — manages availability calendar, accepts/declines/reschedules bookings, conducts video consults, issues e-prescriptions, views earnings.
- **Clinic Admin** — onboards doctors under their clinic, manages shared calendars/rooms, views clinic-wide booking and revenue reports.
- **Platform Admin** — approves doctor/clinic verification (medical license checks), manages disputes, monitors platform health, handles refunds/escalations.

## 4. User Stories

### Patient
- [ ] As a patient, I want to search doctors by specialty, city, and availability, so I can find someone who can see me soon.
- [ ] As a patient, I want to see a doctor's profile (credentials, reviews, consultation fee), so I can decide if they're a good fit.
- [ ] As a patient, I want to book and pay for an appointment in one flow, so I don't have to call the clinic.
- [ ] As a patient, I want to join a video consultation from my browser with no app install, so the experience is frictionless.
- [ ] As a patient, I want to receive my e-prescription after a consult, so I can fill it at any pharmacy.
- [ ] As a patient, I want to reschedule or cancel within a policy window, so I'm not penalized for reasonable changes.
- [ ] As a patient, I want appointment reminders via SMS/email, so I don't forget or no-show.
- [ ] As a patient, I want to leave a review after a completed consult, so future patients can trust the marketplace.

### Doctor
- [ ] As a doctor, I want to set my recurring weekly availability and block out exceptions (holidays, personal time), so my calendar stays accurate without manual daily updates.
- [ ] As a doctor, I want to see my upcoming appointments with patient context (reason for visit, history if returning patient), so I can prepare.
- [ ] As a doctor, I want to conduct a video consultation and issue an e-prescription in the same session, so my workflow isn't fragmented across tools.
- [ ] As a doctor, I want to see my earnings and pending payouts, so I can track income from the platform.
- [ ] As a doctor affiliated with multiple clinics, I want a single calendar that reflects bookings from all locations, so I never double-book myself.

### Clinic Admin
- [ ] As a clinic admin, I want to invite and onboard doctors under my clinic's account, so they inherit clinic branding and shared scheduling.
- [ ] As a clinic admin, I want to see clinic-wide booking volume and revenue, so I can manage staffing and cash flow.
- [ ] As a clinic admin, I want to manage shared resources (physical rooms, equipment) alongside doctor calendars, so in-person bookings don't conflict.

### Platform Admin
- [ ] As a platform admin, I want to review and approve doctor license/credential submissions before they go live, so patients trust verified providers.
- [ ] As a platform admin, I want to see flagged disputes (no-shows, payment issues, complaints) in one queue, so I can resolve them quickly.
- [ ] As a platform admin, I want platform-wide health dashboards (bookings, payment failures, video call quality), so I can catch systemic issues early.
- [ ] As a platform admin, I want to issue refunds or override booking states in exceptional cases, so support can resolve edge cases without engineering involvement.

## 5. Scope

### In Scope (v1 / MVP-through-comprehensive per Sprint 1 docs)
- Patient search & discovery (specialty, city, availability, price filter)
- Doctor & clinic onboarding with credential verification workflow
- Appointment booking (in-person + video), rescheduling, cancellation policy
- CMI payment integration (booking deposit or full payment)
- WebRTC video consultation (browser-based, no native app)
- E-prescription generation (PDF, tied to consultation record)
- SMS/email notifications (booking confirmation, reminders, cancellation)
- Ratings & reviews (post-consultation only, to prevent fake reviews)
- Platform admin dashboard (verification queue, disputes, health metrics)
- Role-based access control (Patient, Doctor, Clinic Admin, Platform Admin)

### Out of Scope (explicitly deferred)
- Native mobile apps (iOS/Android) — web-responsive only for v1
- Insurance claim integration beyond CMI (e.g., CNSS, private insurers) — Phase 2
- In-app messaging/chat between patient and doctor outside consult sessions
- AI-based symptom-to-specialist matching (pgvector groundwork may exist in DB layer, but no ML model in v1)
- Multi-language beyond French/Arabic UI (English deferred)
- Pharmacy-side e-prescription fulfillment tracking
- Doctor-to-doctor referral network features

## 6. Requirements

### Functional
- FR-1: System supports search/filter of doctors by specialty, location, and available time slots.
- FR-2: System enforces double-booking prevention across all clinics a doctor is affiliated with.
- FR-3: System integrates CMI for payment capture at booking time, with configurable deposit vs. full-payment policy.
- FR-4: System supports WebRTC video sessions with TURN/STUN fallback for NAT traversal.
- FR-5: System generates a signed, tamper-evident e-prescription PDF per completed consultation.
- FR-6: System sends SMS and email notifications for booking lifecycle events (confirmed, reminder, cancelled, rescheduled).
- FR-7: System supports role-based access control for 4 roles with distinct permission sets.
- FR-8: System supports a doctor/clinic verification workflow with document upload and admin approval/rejection.
- FR-9: System records post-consultation reviews, visible only after appointment completion status.
- FR-10: System supports appointment cancellation/rescheduling subject to a configurable time-window policy.

### Non-Functional
- NFR-1: Performance — search results return in < 1.5s p95 for catalogs up to 10,000 doctors.
- NFR-2: Availability — 99.5% uptime target for booking and payment paths (highest business criticality).
- NFR-3: Security — all PII and health data encrypted at rest and in transit; JWT-based auth with short-lived access tokens + refresh tokens.
- NFR-4: Compliance — architecture must accommodate Morocco's Loi 09-08 (data protection) and CNDP registration requirements; health data handling reviewed by Security Engineer (see `docs/security-tabib-ma.md`).
- NFR-5: Scalability — system must handle 10x current projected load (from ~150 to 1,500 doctors) without architectural rework, per System Designer doc.
- NFR-6: Accessibility — patient-facing web UI meets WCAG 2.1 AA at minimum for core booking flow.
- NFR-7: Video quality — target < 300ms round-trip latency for in-region (Morocco/EU) video sessions; graceful degradation to audio-only on poor connections.
- NFR-8: Auditability — all admin actions (verification approval, refund, dispute resolution) are logged immutably for compliance review.

## 7. Constraints & Assumptions
- **Constraint**: CMI is the only payment processor in scope for v1 (Moroccan market requirement).
- **Constraint**: Video infrastructure must work over variable-quality Moroccan mobile/broadband connections — audio-first fallback is mandatory, not optional.
- **Constraint**: Team is committing to Java Spring Boot + React + PostgreSQL + Docker (pivoted from an earlier Next.js prototype direction) — no legacy code carries over.
- **Assumption**: Doctor credential verification is manual (admin-reviewed) for v1 — no automated license-registry API integration exists yet in Morocco to rely on.
- **Assumption**: Single-region deployment (Morocco/EU) is sufficient for v1; multi-region is a Phase 2 concern.
- **Assumption**: E-prescriptions are informational PDFs for patient use at any pharmacy — no real-time pharmacy inventory or fulfillment integration exists yet.

## 8. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| CMI integration delays (sandbox/certification process) | M | H | Start CMI sandbox integration early in Sprint 2; have a manual-payment fallback for pilot clinics |
| Doctor supply-side cold start (marketplace liquidity) | H | H | Manually onboard first 20-30 doctors via direct outreach before public launch; clinic partnerships as anchor supply |
| Video quality on low-bandwidth connections | M | M | Audio-only fallback mode; adaptive bitrate; TURN relay for NAT traversal |
| Health data compliance gaps (Loi 09-08 / CNDP) | M | H | Security Engineer produces compliance-mapped doc before any PII schema is finalized (see `docs/security-tabib-ma.md`) |
| Scope creep from "nice to have" features (insurance, chat, AI matching) | H | M | YAGNI enforcement — explicit out-of-scope list above; revisit only after v1 metrics validate demand |

## 9. Timeline
| Milestone | Target |
|---|---|
| PRD Approved | Sprint 1 |
| Architecture + System Design Approved | Sprint 1 |
| Security + Database Design Approved | Sprint 1 |
| UX + UI Approved | Sprint 1 |
| Test Strategy + DevOps Approved | Sprint 1 |
| Stories Ready, Docs Committed & Pushed | End of Sprint 1 |
| Implementation Start | Sprint 2 |
| MVP Ready (core booking + payment + video) | Sprint 6 (estimate, to be refined by Scrum Master in stories doc) |
