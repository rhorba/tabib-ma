# Database: Tabib.ma
**References**: docs/architecture-tabib-ma.md, docs/security-tabib-ma.md | **Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DBA | **Status**: Draft

## 1. Sizing Inputs
Per System Design capacity estimation: ~3,000 DAU by month 6, ~500 bookings/day, < 1GB/day writes. **PostgreSQL 16** is the right default — no case for NoSQL/document store here (highly relational data: users, appointments, payments all have strong referential integrity needs).

## 2. Entities & Relationships

```
User ──1:1──► DoctorProfile (if role=DOCTOR)
User ──1:N──► ClinicStaffMembership ──N:1──► Clinic
Clinic ──1:N──► DoctorProfile (via membership, a doctor can belong to 0 or more clinics)
DoctorProfile ──1:N──► AvailabilitySlot
DoctorProfile ──1:N──► VerificationDocument
User(patient) ──1:N──► Appointment ──N:1──► DoctorProfile
Appointment ──1:1──► Payment
Appointment ──1:1──► Consultation (only if location_type = VIDEO)
Consultation ──1:1──► Prescription
Prescription ──1:N──► PrescriptionItem
Appointment ──1:1──► Review (only after status=COMPLETED)
User ──1:N──► RefreshToken
(all state-changing admin actions) ──► AuditLogEntry
```

## 3. Schema (DDL — Sprint 1 design, refined during implementation)

```sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "btree_gist"; -- required for EXCLUDE constraint below

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL UNIQUE,
    phone           TEXT UNIQUE,
    password_hash   TEXT NOT NULL,
    role            TEXT NOT NULL CHECK (role IN ('PATIENT','DOCTOR','CLINIC_ADMIN','PLATFORM_ADMIN')),
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    mfa_enabled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ -- soft delete, see Security doc data-subject-rights note
);
CREATE INDEX idx_users_role ON users(role);

CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    token_hash      TEXT NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

CREATE TABLE clinics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            TEXT NOT NULL,
    city            TEXT NOT NULL,
    address         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_clinics_city ON clinics(city);

CREATE TABLE doctor_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id),
    specialty       TEXT NOT NULL,
    bio             TEXT,
    consultation_fee_mad NUMERIC(10,2) NOT NULL,
    verification_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (verification_status IN ('PENDING','APPROVED','REJECTED')),
    city            TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_doctor_profiles_specialty_city ON doctor_profiles(specialty, city);
CREATE INDEX idx_doctor_profiles_verification ON doctor_profiles(verification_status) WHERE verification_status = 'PENDING';

CREATE TABLE clinic_staff_memberships (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID NOT NULL REFERENCES clinics(id),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    role_in_clinic  TEXT NOT NULL DEFAULT 'DOCTOR',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(clinic_id, doctor_profile_id)
);

CREATE TABLE verification_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    document_type   TEXT NOT NULL,
    object_storage_key TEXT NOT NULL,
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE availability_slots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    location_type   TEXT NOT NULL CHECK (location_type IN ('IN_PERSON','VIDEO')),
    clinic_id       UUID REFERENCES clinics(id), -- nullable: independent doctors
    is_booked       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at)
);
CREATE INDEX idx_availability_doctor_time ON availability_slots(doctor_profile_id, starts_at) WHERE is_booked = FALSE;

CREATE TABLE appointments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id      UUID NOT NULL REFERENCES users(id),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    availability_slot_id UUID NOT NULL REFERENCES availability_slots(id),
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    location_type   TEXT NOT NULL CHECK (location_type IN ('IN_PERSON','VIDEO')),
    status          TEXT NOT NULL DEFAULT 'PENDING_PAYMENT'
                    CHECK (status IN ('PENDING_PAYMENT','CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')),
    cancellation_window_hours INT NOT NULL DEFAULT 24,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- ADR-4 (Architecture doc): hard DB-level guarantee against double-booking,
    -- independent of application-layer locking. Only enforced for non-cancelled appointments.
    EXCLUDE USING gist (
        doctor_profile_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status IN ('PENDING_PAYMENT','CONFIRMED'))
);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_status ON appointments(doctor_profile_id, status);

CREATE TABLE payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL UNIQUE REFERENCES appointments(id),
    amount_mad      NUMERIC(10,2) NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCEEDED','FAILED','REFUNDED')),
    cmi_transaction_ref TEXT UNIQUE,
    idempotency_key TEXT NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE consultations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL UNIQUE REFERENCES appointments(id),
    status          TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','FAILED')),
    started_at      TIMESTAMPTZ,
    ended_at        TIMESTAMPTZ
);

CREATE TABLE prescriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL REFERENCES consultations(id),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    patient_id      UUID NOT NULL REFERENCES users(id),
    pdf_object_key  TEXT NOT NULL,
    content_hash    TEXT NOT NULL, -- tamper-evidence, see Security doc
    supersedes_id   UUID REFERENCES prescriptions(id), -- correction chain, never edit in place
    signed_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id);

CREATE TABLE prescription_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prescription_id UUID NOT NULL REFERENCES prescriptions(id),
    drug_name       TEXT NOT NULL,
    dosage          TEXT NOT NULL,
    instructions    TEXT
);

CREATE TABLE reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL UNIQUE REFERENCES appointments(id),
    patient_id      UUID NOT NULL REFERENCES users(id),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    rating          SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_reviews_doctor ON reviews(doctor_profile_id);

CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id   UUID NOT NULL REFERENCES users(id),
    action          TEXT NOT NULL,
    target_type     TEXT NOT NULL,
    target_id       UUID NOT NULL,
    metadata        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_log_target ON audit_log(target_type, target_id);
-- audit_log is append-only: no UPDATE/DELETE grants for the application role (see Section 6)
```

## 4. Indexing Rationale
- `idx_availability_doctor_time` is **partial** (`WHERE is_booked = FALSE`) — the booking search only ever looks at open slots, so indexing booked slots is wasted space.
- `idx_doctor_profiles_verification` is partial on `PENDING` — the admin verification queue is the only consumer of this index and only cares about pending rows.
- Composite `idx_doctor_profiles_specialty_city` supports the primary search pattern (search doctor by specialty + city) with specialty first since it's typically the more selective filter — confirm selectivity once real data exists and re-order if city turns out more selective.
- No index on booleans/low-cardinality columns beyond the partial indexes above (per DBA YAGNI rule).

## 5. The Double-Booking Constraint (ADR-4 follow-through)
The `EXCLUDE USING gist` constraint on `appointments` is the DB-level backstop. Requires `btree_gist` extension (enabled above) since it combines an equality check (`doctor_profile_id`) with a range-overlap check (`tstzrange`). This means **even a buggy application-layer row lock cannot cause a double-booking** — Postgres itself rejects the conflicting insert with a constraint violation, which the Backend Dev must catch and translate into the `ConflictException` defined in the Architecture doc's error-handling section.

## 6. Access Control at the DB Level
- Application connects via a dedicated `tabibma_app` role — **not** the superuser/owner role.
- `tabibma_app` has no `DELETE`/`UPDATE` grant on `audit_log` (append-only enforced at the DB layer, not just application discipline) — supports Security doc's repudiation mitigation.
- Migrations run via a separate `tabibma_migrator` role with DDL privileges, not used by the running application.

## 7. Connection Pooling
Spring Boot's default HikariCP is sufficient at this scale — no PgBouncer needed yet. Recommended starting pool size: `maximum-pool-size: 10` for a single app instance (formula: `connections = ((core_count * 2) + effective_spindle_count)`, adjusted down since this is a light-CPU, I/O-bound workload). Re-evaluate per DBA performance-fix ordering (add PgBouncer before considering read replicas, per the standard ordering) if connection exhaustion is observed.

## 8. Backup Strategy
| Method | Frequency | Retention |
|---|---|---|
| pg_dump (logical) | Daily | 30 days |
| WAL archiving (PITR) | Continuous | 7 days (supports RPO < 15min target from System Design) |
| Cloud snapshots | Daily | 30 days |

Test restores monthly — logged to `.logs/activity.md` once operational (DevOps responsibility post-launch).

## 9. Migration Plan (Sprint 2 kickoff)
Flyway (pairs naturally with Spring Boot) — one migration file per schema change, forward-only in production, additive-first for any column changes (nullable → backfill → constrain), per DBA migration rules.

## 10. Handoff Points
- **→ Backend Dev**: Full DDL above as the Sprint 2 starting schema (Flyway migration V1).
- **→ Security Engineer**: `audit_log` append-only DB grant confirms Section 6's repudiation mitigation is enforced at the DB layer, not just app code.
- **→ Test Architect**: The `EXCLUDE` constraint (Section 5) needs a concurrency test — two simultaneous booking requests for the same slot, assert exactly one succeeds.
- **→ DevOps**: Connection pool sizing (Section 7), backup/restore cadence (Section 8) for infra provisioning.
