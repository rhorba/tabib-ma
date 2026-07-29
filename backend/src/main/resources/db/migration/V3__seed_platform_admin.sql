-- Epic 2 BRAINSTORM decision (.logs/decisions.md, 2026-07-28): bootstrap the first PLATFORM_ADMIN
-- via a seeded row rather than building an admin-creation endpoint nothing else needs yet.
-- Dev/test credentials only — email tabib-admin@tabibma.dev / password changeme-admin-dev-only,
-- documented in .env.example. Must be rotated or removed before any production launch.
INSERT INTO users (email, password_hash, role, first_name, last_name)
VALUES (
    'tabib-admin@tabibma.dev',
    '$argon2id$v=19$m=16384,t=2,p=1$IqXQkEBkdoJwQ2PjPz4t3w$KBIn/V4DsXvrMNpZVlB78JxKJA4pvvtSULm1m0P6NxQ',
    'PLATFORM_ADMIN',
    'Platform',
    'Admin'
);
