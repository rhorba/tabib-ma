-- Epic 8 Story 8.2 Batch 2 — doctor-side resource selection on IN_PERSON availability rules.
-- Many-to-many between availability_rules and clinic_resources; conflict prevention against
-- these at booking time lands in a later migration (Batch 3), mirroring V13's own note.
CREATE TABLE availability_rule_resources (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    availability_rule_id  UUID NOT NULL REFERENCES availability_rules(id),
    resource_id           UUID NOT NULL REFERENCES clinic_resources(id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (availability_rule_id, resource_id)
);
CREATE INDEX idx_availability_rule_resources_rule ON availability_rule_resources(availability_rule_id);
CREATE INDEX idx_availability_rule_resources_resource ON availability_rule_resources(resource_id);
