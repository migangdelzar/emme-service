-- liquibase formatted sql
-- changeset emme:033-appointment-overlap-constraint
-- comment: Enforce one active appointment per tenant, artist and time range.

CREATE EXTENSION IF NOT EXISTS btree_gist;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM appointment first_appointment
        JOIN appointment second_appointment
          ON first_appointment.id < second_appointment.id
         AND first_appointment.tenant_id = second_appointment.tenant_id
         AND first_appointment.artist_id = second_appointment.artist_id
         AND tstzrange(
                 first_appointment.starts_at,
                 first_appointment.ends_at,
                 '[)') && tstzrange(
                 second_appointment.starts_at,
                 second_appointment.ends_at,
                 '[)')
        WHERE first_appointment.status IN ('CONFIRMED', 'IN_PROGRESS')
          AND second_appointment.status IN ('CONFIRMED', 'IN_PROGRESS')
    ) THEN
        RAISE EXCEPTION
            'existing active appointment overlaps prevent collision constraint creation';
    END IF;
END
$$;

ALTER TABLE appointment
    ADD CONSTRAINT appointment_active_artist_no_overlap
    EXCLUDE USING gist (
        tenant_id WITH =,
        artist_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    )
    WHERE (status IN ('CONFIRMED', 'IN_PROGRESS'));

CREATE INDEX IF NOT EXISTS idx_appt_active_tenant_artist_time
    ON appointment (tenant_id, artist_id, starts_at, ends_at)
    WHERE status IN ('CONFIRMED', 'IN_PROGRESS');

-- rollback: DROP INDEX IF EXISTS idx_appt_active_tenant_artist_time;
-- rollback: ALTER TABLE appointment DROP CONSTRAINT IF EXISTS appointment_active_artist_no_overlap;
