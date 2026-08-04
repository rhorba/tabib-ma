-- Epic 8 Story 8.2 Batch 3 — conflict prevention for resource-requiring appointments.
-- Mirrors V7's appointments EXCLUDE constraint (ADR-4) but keyed on resource_id instead of
-- doctor_profile_id; btree_gist was already enabled by V7. Two tables:
--  1. availability_slot_resources denormalizes an availability rule's required resources onto
--     each slot it generates (same pattern as slots already copying locationType/clinicId from
--     their rule), so booking-time allocation doesn't need to look the rule back up.
--  2. appointment_resource_allocations is the EXCLUDE-guarded table itself. Unlike `appointments`,
--     rows here are deleted outright on cancellation (ResourceAllocationGuard.releaseForAppointment)
--     rather than filtered by a WHERE status clause — a cancelled appointment simply no longer
--     holds the resource.
CREATE TABLE availability_slot_resources (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    availability_slot_id  UUID NOT NULL REFERENCES availability_slots(id),
    resource_id           UUID NOT NULL REFERENCES clinic_resources(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (availability_slot_id, resource_id)
);
CREATE INDEX idx_availability_slot_resources_slot ON availability_slot_resources(availability_slot_id);

CREATE TABLE appointment_resource_allocations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id  UUID NOT NULL REFERENCES appointments(id),
    resource_id     UUID NOT NULL REFERENCES clinic_resources(id),
    starts_at       TIMESTAMPTZ NOT NULL,
    ends_at         TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (ends_at > starts_at),
    EXCLUDE USING gist (
        resource_id WITH =,
        tstzrange(starts_at, ends_at) WITH &&
    )
);
CREATE INDEX idx_appointment_resource_allocations_appointment ON appointment_resource_allocations(appointment_id);
