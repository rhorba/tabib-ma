-- Epic 6 Story 6.1 (WebRTC video join) — see docs/architecture-tabib-ma.md Section 3.
-- One Consultation per VIDEO appointment, created by ConsultationBookingListener when
-- BookingConfirmedEvent fires for a slot with location_type = VIDEO.
CREATE TABLE consultations (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    status         TEXT NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    started_at     TIMESTAMPTZ,
    ended_at       TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
