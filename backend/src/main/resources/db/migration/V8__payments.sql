-- Epic 5 Story 5.1 (CMI payment capture) — see docs/database-tabib-ma.md Section 3 and
-- docs/architecture-tabib-ma.md (PaymentGateway Strategy interface). Status vocabulary follows
-- the DB doc's schema (PENDING/SUCCEEDED/FAILED/REFUNDED) rather than the stories doc's looser
-- "CONFIRMED" language — the DB doc is the binding schema design.
CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id      UUID NOT NULL UNIQUE REFERENCES appointments(id),
    amount_mad          NUMERIC(10,2) NOT NULL,
    status              TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SUCCEEDED','FAILED','REFUNDED')),
    cmi_transaction_ref TEXT UNIQUE,
    idempotency_key     TEXT NOT NULL UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
