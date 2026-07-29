-- Story 2.3 (clinic onboarding + doctor invitation) — see docs/stories-tabib-ma.md Story 2.3
-- and .logs/decisions.md 2026-07-29 BRAINSTORM. clinics had no owner column in V2; clinic
-- admins self-create their own clinic, mirroring doctor_profiles.user_id from Story 2.1.
ALTER TABLE clinics ADD COLUMN admin_user_id UUID NOT NULL REFERENCES users(id);
ALTER TABLE clinics ADD CONSTRAINT uq_clinics_admin_user_id UNIQUE (admin_user_id);

-- No token/mailed-link — the invited doctor is matched by their authenticated email at
-- accept time (BRAINSTORM decision: in-app accept, no email-sending infrastructure yet).
CREATE TABLE clinic_invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID NOT NULL REFERENCES clinics(id),
    invited_email   TEXT NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACCEPTED','DECLINED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at      TIMESTAMPTZ
);
CREATE INDEX idx_clinic_invitations_lookup ON clinic_invitations(invited_email, status);

-- Seeded dev/test CLINIC_ADMIN (mirrors V3__seed_platform_admin.sql — dev/test credentials
-- only, documented in .env.example, rotate or remove before production launch).
INSERT INTO users (email, password_hash, role, first_name, last_name)
VALUES (
    'clinic-admin@tabibma.dev',
    '$argon2id$v=19$m=16384,t=2,p=1$LfiPF4LsdSeGbw3slKNGMw$twoRWV4aW5NcRqypUD8Uo88uMksHWPhiYM9u87HoHUg',
    'CLINIC_ADMIN',
    'Clinic',
    'Admin'
);
