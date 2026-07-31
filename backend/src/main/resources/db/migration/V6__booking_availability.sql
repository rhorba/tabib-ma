-- Epic 4 (Appointment Booking & Scheduling), Story 4.1 — see docs/stories-tabib-ma.md and
-- docs/database-tabib-ma.md Section 3 (availability_slots design, unchanged from the Sprint 1
-- draft). availability_rules and availability_exceptions are new tables not in that draft — the
-- recurring-weekly-schedule + exception mechanism needed to generate concrete availability_slots
-- rows was left for implementation-time design per the doc's "refined during implementation" note.
CREATE TABLE availability_rules (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_profile_id      UUID NOT NULL REFERENCES doctor_profiles(id),
    day_of_week            TEXT NOT NULL CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY')),
    start_time             TIME NOT NULL,
    end_time               TIME NOT NULL,
    slot_duration_minutes  INT NOT NULL CHECK (slot_duration_minutes > 0),
    location_type          TEXT NOT NULL CHECK (location_type IN ('IN_PERSON','VIDEO')),
    clinic_id              UUID REFERENCES clinics(id),
    active                 BOOLEAN NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (end_time > start_time)
);
CREATE INDEX idx_availability_rules_doctor ON availability_rules(doctor_profile_id) WHERE active = TRUE;

-- Table name follows the story's own "exceptions" language; the Java entity is named
-- AvailabilityBlockedDate to avoid reading like a Throwable in stack traces/tooling.
CREATE TABLE availability_exceptions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    exception_date    DATE NOT NULL,
    reason            TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(doctor_profile_id, exception_date)
);

CREATE TABLE availability_slots (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_profile_id UUID NOT NULL REFERENCES doctor_profiles(id),
    starts_at         TIMESTAMPTZ NOT NULL,
    ends_at           TIMESTAMPTZ NOT NULL,
    location_type     TEXT NOT NULL CHECK (location_type IN ('IN_PERSON','VIDEO')),
    clinic_id         UUID REFERENCES clinics(id),
    is_booked         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at),
    -- Defense-in-depth against duplicate generation (the app-level generation service also
    -- checks in-memory before inserting); the real double-booking guard is Story 4.3's EXCLUDE
    -- constraint on `appointments`, not this table.
    UNIQUE(doctor_profile_id, starts_at)
);
CREATE INDEX idx_availability_doctor_time ON availability_slots(doctor_profile_id, starts_at) WHERE is_booked = FALSE;
