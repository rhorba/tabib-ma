-- Epic 2 (Doctor & Clinic Onboarding / Verification) — see docs/database-tabib-ma.md Section 3.
-- clinic_staff_memberships has no consumer yet (Story 2.3, deferred) but is created now alongside
-- the rest of this epic's schema per the "one migration per schema change" migration plan rule.
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

-- Cross-cutting append-only audit trail (PRD NFR-8) — introduced now for Story 2.2's admin
-- review actions, its first real consumer. DB-level append-only role grants (docs/database-tabib-ma.md
-- Section 6) are a pre-launch DevOps task; this project doesn't yet run its own migrator/app role split.
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
