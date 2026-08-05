-- Epic 10 Story 10.1 (dispute queue) — see docs/database-tabib-ma.md (no dispute model was
-- originally specced; designed fresh at this epic's BRAINSTORM, 2026-08-05).
-- reported_by_user_id is nullable: null means system-generated (no-show / payment-failure
-- auto-flag, Batch 2) rather than a patient/doctor self-report.
CREATE TABLE disputes (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id       UUID NOT NULL REFERENCES appointments(id),
    type                 VARCHAR(20) NOT NULL,
    reason               TEXT,
    reported_by_user_id  UUID,
    status               VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at          TIMESTAMPTZ,
    resolved_by_user_id  UUID
);

CREATE INDEX idx_disputes_appointment ON disputes(appointment_id);
CREATE INDEX idx_disputes_status ON disputes(status);
