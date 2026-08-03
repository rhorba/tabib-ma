-- Epic 9 Story 9.1 (post-consultation review) — see docs/database-tabib-ma.md.
-- One review per appointment, enforced here (not just in the service layer):
-- appointment_id is UNIQUE.
CREATE TABLE reviews (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id    UUID NOT NULL UNIQUE REFERENCES appointments(id),
    patient_id        UUID NOT NULL,
    doctor_profile_id UUID NOT NULL,
    rating            INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment           TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reviews_doctor ON reviews(doctor_profile_id);
CREATE INDEX idx_reviews_patient ON reviews(patient_id);
