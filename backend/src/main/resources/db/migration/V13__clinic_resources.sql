-- Epic 8 Story 8.2 (shared clinic resource management) — see docs/stories-tabib-ma.md.
-- Rooms/equipment a clinic admin manages; conflict prevention against these lands in a later
-- migration once appointments can require them (mirrors booking_appointments' own two-migration
-- split between the table and its EXCLUDE constraint use).
CREATE TABLE clinic_resources (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id   UUID NOT NULL REFERENCES clinics(id),
    type        TEXT NOT NULL CHECK (type IN ('ROOM', 'EQUIPMENT')),
    name        TEXT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_clinic_resources_clinic ON clinic_resources(clinic_id);
