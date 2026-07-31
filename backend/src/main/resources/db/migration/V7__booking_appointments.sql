-- Epic 4 Story 4.3 (double-booking prevention, ADR-4) — see docs/architecture-tabib-ma.md ADR-4
-- and docs/database-tabib-ma.md Section 5. EXCLUDE constraint requires btree_gist, not enabled
-- by any prior migration.
CREATE EXTENSION IF NOT EXISTS "btree_gist";

CREATE TABLE appointments (
    id                         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id                 UUID NOT NULL REFERENCES users(id),
    doctor_profile_id          UUID NOT NULL REFERENCES doctor_profiles(id),
    availability_slot_id       UUID NOT NULL REFERENCES availability_slots(id),
    starts_at                  TIMESTAMPTZ NOT NULL,
    ends_at                    TIMESTAMPTZ NOT NULL,
    location_type              TEXT NOT NULL CHECK (location_type IN ('IN_PERSON','VIDEO')),
    status                     TEXT NOT NULL DEFAULT 'PENDING_PAYMENT'
                               CHECK (status IN ('PENDING_PAYMENT','CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')),
    cancellation_window_hours  INT NOT NULL DEFAULT 24,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at),
    -- ADR-4: DB-level backstop against double-booking, independent of the application-layer row
    -- lock (DoubleBookingGuard's SELECT...FOR UPDATE on availability_slots). Catches the case a
    -- row lock can't: two DIFFERENT availability_slot rows for the same doctor with overlapping
    -- time ranges. Only enforced for non-cancelled appointments.
    EXCLUDE USING gist (
        doctor_profile_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    ) WHERE (status IN ('PENDING_PAYMENT','CONFIRMED'))
);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor_status ON appointments(doctor_profile_id, status);
