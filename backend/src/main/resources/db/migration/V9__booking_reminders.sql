-- Epic 4 Story 4.5 (appointment reminders) — tracks whether the reminder sweep already fired for
-- this appointment, so a repeated @Scheduled tick doesn't resend it.
ALTER TABLE appointments ADD COLUMN reminder_sent_at TIMESTAMPTZ;
