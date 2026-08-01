-- Epic 7 Story 7.1 (signed, immutable e-prescription PDF) — see docs/architecture-tabib-ma.md
-- Section 3. Immutability: a "correction" always INSERTs a new row with supersedes_id pointing
-- at the original rather than UPDATEing it (Test Strategy §2, "Maximum risk" AC).
CREATE TABLE prescriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL REFERENCES consultations(id),
    doctor_id       UUID NOT NULL,
    patient_id      UUID NOT NULL,
    supersedes_id   UUID REFERENCES prescriptions(id),
    pdf_storage_key TEXT NOT NULL,
    signed_at       TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE prescription_items (
    prescription_id UUID NOT NULL REFERENCES prescriptions(id),
    item_order      INTEGER NOT NULL,
    medication_name TEXT NOT NULL,
    dosage          TEXT NOT NULL,
    instructions    TEXT,
    PRIMARY KEY (prescription_id, item_order)
);

CREATE INDEX idx_prescriptions_patient_id ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_doctor_id ON prescriptions(doctor_id);
