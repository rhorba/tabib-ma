-- Story 3.1's k6 load test needs a "seeded 10k-doctor dataset" (Test Strategy doc §7) to be
-- realistic — search's own index (idx_doctor_profiles_approved_specialty_city) only matters
-- once there's enough data for Postgres to actually use it over a sequential scan. Seeds
-- directly via SQL (not the HTTP API) since 10k profile-creation + admin-approval round trips
-- would take far longer than the load test itself and isn't what's under test here.
--
-- Run against the dev DB: docker compose exec -T db psql -U tabibma -d tabibma -f - < seed-doctors.sql
-- (password_hash is a dummy value — these users are never logged into, only searched for.)

WITH specialties(name) AS (
    VALUES ('Cardiologie'), ('Dermatologie'), ('Pediatrie'), ('Gynecologie'), ('Neurologie'),
           ('Psychiatrie'), ('Rhumatologie'), ('Endocrinologie'), ('Gastroenterologie'), ('Ophtalmologie')
),
cities(name) AS (
    VALUES ('Rabat'), ('Casablanca'), ('Marrakech'), ('Fes'), ('Tanger'),
           ('Agadir'), ('Meknes'), ('Oujda'), ('Kenitra'), ('Tetouan')
),
new_users AS (
    INSERT INTO users (email, password_hash, role, first_name, last_name)
    SELECT
        'loadtest-doctor-' || gs || '@example.com',
        '$2a$10$loadtestloadtestloadtestloadtestloadtestloadtestloadte', -- unused, never logged into
        'DOCTOR',
        'LoadTest',
        'Doctor' || gs
    FROM generate_series(1, 10000) AS gs
    RETURNING id, email
)
INSERT INTO doctor_profiles (user_id, specialty, bio, consultation_fee_mad, verification_status, city)
SELECT
    u.id,
    (SELECT name FROM specialties OFFSET floor(random() * 10) LIMIT 1),
    'Seeded for k6 load test.',
    (100 + floor(random() * 400))::numeric(10,2),
    'APPROVED',
    (SELECT name FROM cities OFFSET floor(random() * 10) LIMIT 1)
FROM new_users u;

-- Cleanup (run after the load test if you don't want this data lingering in the dev DB):
-- DELETE FROM doctor_profiles WHERE bio = 'Seeded for k6 load test.';
-- DELETE FROM users WHERE email LIKE 'loadtest-doctor-%@example.com';
