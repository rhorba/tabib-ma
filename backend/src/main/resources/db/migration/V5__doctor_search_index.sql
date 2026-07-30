-- Story 3.1 search hot path: only APPROVED doctors are ever returned by search, and every
-- search query filters on that status. idx_doctor_profiles_specialty_city (V2) covers the
-- specialty+city filter but not the status predicate; this partial index mirrors the existing
-- idx_doctor_profiles_verification convention (a status-scoped partial index) for the query
-- shape search actually runs.
CREATE INDEX idx_doctor_profiles_approved_specialty_city
    ON doctor_profiles(specialty, city)
    WHERE verification_status = 'APPROVED';
