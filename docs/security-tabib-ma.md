# Security: Tabib.ma
**References**: docs/prd-tabib-ma.md, docs/architecture-tabib-ma.md | **Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Security Engineer | **Status**: Draft

## 1. Stage & Posture
This is a **production-track healthcare marketplace handling PII and health data** (not a weekend MVP) — the "Growing app → Production at scale" security bar applies from day one: rate limiting, RBAC, structured audit logging, dependency scanning, and a documented compliance mapping are all in scope for Sprint 1, even though implementation happens later.

## 2. 5-Minute Threat Model
```
1. WHAT are we building? A 3-sided telemedicine marketplace: patients book/pay/video-consult with doctors;
   clinics manage staff; platform admins verify credentials and resolve disputes.
2. WHAT can go wrong?
   - Who: opportunistic attackers (credential stuffing, scraping doctor/patient PII), a competitor
     (scraping the doctor catalog), an insider (clinic admin or platform admin abusing access),
     a malicious patient/doctor (IDOR to view another user's health records or prescriptions).
   - Worst outcome: health data breach (regulatory + reputational), payment fraud via CMI webhook
     forgery, prescription forgery/tampering, booking-system abuse (fake slots, no-show griefing).
3. WHAT are we doing about it?
   - [see Auth/Authorization/Data sections below — one control per threat, itemized]
4. DID we do a good job?
   - Validation: security review checklist below signed off before each module ships; pen test
     before public launch (Test Architect + external pen test recommended pre-GA).
```

## 3. Authentication Design
**Decision (per decision tree)**: User-facing app, distributed (horizontally scalable per System Design) → **JWT** (access + refresh).

- Access token: JWT, RS256, **15 min** expiry, claims = `sub` (user id), `role`, `clinicId` (if applicable) — validated on every request (signature + expiry + issuer).
- Refresh token: opaque random value, stored **hashed** in DB (`refresh_tokens` table), 7-day expiry, rotated on every use (old one invalidated), revocable (logout, password change, admin-forced revoke).
- Passwords: **argon2id** (not bcrypt — better resistance to GPU cracking, acceptable Spring Security support), min 10 chars, checked against a breached-password list (e.g., HaveIBeenPwned k-anonymity API) at registration/change.
- MFA: **not required for Patient role at v1** (friction vs. risk trade-off for a consumer booking flow) — **mandatory for Platform Admin role** (TOTP) given the blast radius of that role. Doctor/Clinic Admin: optional at v1, revisit as a requirement once verification-fraud data exists.
- Account lockout: 5 failed attempts → exponential backoff (1min, 5min, 15min, 1hr), reset on successful login.
- Password reset: time-limited (15 min) single-use token, invalidates all existing refresh tokens on successful reset.

## 4. Authorization Design
**Decision**: **RBAC** (4 roles: Patient, Doctor, Clinic Admin, Platform Admin) + **per-resource ownership checks** to prevent IDOR — RBAC alone is insufficient because a Doctor role must only access *their own* appointments/patients, not every doctor's.

```
Role → Base Permissions
  Patient       → own appointments, own prescriptions, own reviews (write), public doctor search
  Doctor        → own calendar/availability, own appointments' patient data, own prescriptions (write)
  Clinic Admin  → own clinic's doctors/appointments (read/manage), NOT other clinics' data
  Platform Admin → verification queue, disputes, refunds, platform health (elevated, MFA-gated)
```
Every controller method enforcing "own X" must check resource ownership server-side (e.g., `appointment.doctorId == currentUser.id`) — **never trust a client-supplied ID alone**. This is the #1 OWASP risk for this domain (IDOR on health records) and must be part of the Test Architect's adversarial test suite (attempt cross-user/cross-clinic access on every resource endpoint).

## 5. STRIDE — Key Flows

### Booking + Payment
| Threat | Scenario | Mitigation |
|---|---|---|
| Spoofing | Attacker forges a CMI webhook call claiming payment succeeded | Verify CMI webhook signature (HMAC) on every callback; reject unsigned/invalid requests |
| Tampering | Client manipulates appointment price/slot in request payload | Server recomputes price from doctor's published fee; slot validated server-side against current availability, never trusts client-echoed values |
| Repudiation | Patient disputes a charge/booking after the fact | Immutable audit log of booking + payment state transitions (who, when, what) |
| Information Disclosure | Doctor A views Doctor B's appointment/patient list | Per-resource ownership check (Section 4) |
| Denial of Service | Booking endpoint hammered to lock all slots (griefing) | Rate limit per-IP and per-account on booking creation; slot lock has a TTL (release if payment not completed within N minutes) |
| Elevation of Privilege | Patient-role JWT reused to hit admin endpoints | Role claim validated server-side on every admin route; admin routes also require MFA-verified session flag |

### Video Consultation
| Threat | Scenario | Mitigation |
|---|---|---|
| Spoofing | Uninvited party joins a consultation | Signaling tokens are short-lived, scoped to one specific appointment ID, single-use for join |
| Information Disclosure | Video/audio intercepted or recorded without consent | WebRTC DTLS-SRTP encryption (standard); **no server-side recording by default** — if recording is ever added, it requires explicit two-party consent flow (out of scope for v1, noted for Phase 2) |

### Prescription
| Threat | Scenario | Mitigation |
|---|---|---|
| Tampering | Prescription PDF altered after signing | PDF stored with a hash recorded at signing time; served via signed, time-limited URLs from object storage; immutability enforced at the application layer (Architecture doc: corrections create a new record, never edit) |
| Information Disclosure | Prescription URL guessed/shared | Signed URLs with short expiry, not permanent public links |

## 6. Data Protection
- **Encryption at rest**: full-disk/volume encryption on the managed Postgres instance (provider-level) + column-level encryption for the most sensitive fields (national ID number if collected, if any free-text medical notes) using application-level encryption (e.g., AES-256-GCM via a KMS-managed key), not just relying on disk encryption alone.
- **Encryption in transit**: HTTPS/TLS 1.2+ everywhere, HSTS enabled, no plaintext fallback.
- **PII minimization**: collect only what booking/consultation/prescription actually requires. Do not collect national ID unless a specific regulatory or CMI requirement demands it — confirm with legal before adding that field (flagged as an open question below).
- **Secrets management**: all values in `.env.example` are placeholders only; real secrets live in a secrets manager (cloud provider's, or HashiCorp Vault if self-hosting) injected at deploy time — never in source control, never in logs or error responses.
- **Backups encrypted**: matches DBA backup strategy — backups inherit the same encryption-at-rest guarantee.

## 7. Compliance Mapping — Morocco Loi 09-08 (Data Protection) / CNDP
> **Open question flagged for PM/legal, not fully resolved here**: Loi 09-08 requires CNDP declaration/authorization for processing health data (sensitive personal data category). This project **must** file the appropriate CNDP declaration before collecting real patient health data in production. This is a legal/compliance action item, not a code change — tracked as a risk in `.logs/risks.md`.

Architecture implications ready now regardless of filing status:
- Data subject rights groundwork: user profile must support data export and deletion requests (Patient can request their data; deletion soft-deletes PII but retains anonymized records for the legally-required medical record retention period — reconcile exact retention with legal, currently assumed 5 years per PRD NFR-4).
- Data processing purpose limitation: health data (consultation notes, prescriptions) is not used for any purpose beyond direct care delivery (no ad targeting, no unrelated analytics) without explicit additional consent.
- Cross-border transfer: if the managed Postgres/object storage region is EU-based rather than Morocco-based, confirm this is compliant with Loi 09-08 transfer rules — a DevOps/legal decision, flagged here for visibility.

## 8. Security Review Checklist (baseline — re-run per module before ship)
```markdown
### Input/Output
- [ ] All inputs validated server-side (Bean Validation + business-rule validation)
- [ ] Output encoded for context (React auto-escapes JSX; still avoid dangerouslySetInnerHTML)
- [ ] File uploads (verification documents, prescription attachments if any): type-checked (magic-byte, not just extension), size-limited, stored outside webroot, virus-scanned before admin review

### Auth & Access
- [ ] Authentication required on all non-public endpoints
- [ ] Authorization checked per-resource (ownership check, not just role check) — see Section 4
- [ ] No IDOR — verified via adversarial test suite (Test Architect)

### Data
- [ ] Sensitive data (health notes, national ID if collected) encrypted at rest at column level
- [ ] PII minimized per Section 6
- [ ] No secrets in code, logs, or error messages — error responses never leak stack traces in production profile
- [ ] Backups encrypted

### Infrastructure
- [ ] HTTPS only, HSTS enabled
- [ ] Security headers set (CSP, X-Content-Type-Options, X-Frame-Options/frame-ancestors)
- [ ] Unnecessary ports/services closed (DevOps to confirm in Docker/infra config)
- [ ] Dependency scanning in CI (see DevOps doc)

### Verdict: ⚠️ Conditional — pending CNDP filing confirmation (Section 7) and MFA implementation for Platform Admin before production launch.
```

## 9. Incident Response (baseline plan)
```
1. DETECT   → Alerting on: repeated auth failures, webhook signature failures, abnormal admin-role activity,
              DB access pattern anomalies (see System Design observability stack)
2. ASSESS   → Severity (health data exposure = Sev1), scope (how many users/records), root cause hypothesis
3. CONTAIN  → Revoke affected tokens/sessions, disable compromised accounts, block offending IPs
4. FIX      → Patch vulnerability, rotate any exposed secrets/keys
5. RECOVER  → Restore from clean backup if data integrity affected; verify no persistence of attacker access
6. LEARN    → Post-mortem within 48h; if health data was exposed, PM/legal assesses CNDP breach-notification
              obligation under Loi 09-08
```

## 10. Handoff Points
- **→ Backend Dev**: Auth/authorization implementation requirements (Sections 3-4), STRIDE mitigations (Section 5).
- **→ DevOps/DevSecOps**: Secrets management, dependency scanning, security headers, HTTPS/TLS config (Section 6, 8).
- **→ Test Architect**: IDOR adversarial test suite is the top priority; webhook signature bypass attempts second.
- **→ PM**: CNDP filing is a legal action item, not engineering — flagged as a project risk, not blocking Sprint 1 docs but blocking production launch with real patient data.
