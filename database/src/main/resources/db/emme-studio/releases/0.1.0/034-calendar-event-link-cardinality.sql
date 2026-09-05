-- liquibase formatted sql
-- changeset emme:034-calendar-event-link-cardinality
-- comment: Enforce one provider link per appointment in each tenant schema.

DO $$
BEGIN
    IF EXISTS (
        SELECT tenant_id, appointment_id, provider
        FROM calendar_event_link
        GROUP BY tenant_id, appointment_id, provider
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'duplicate calendar event links prevent calendar_event_link_duplicate_key creation';
    END IF;
END
$$;

ALTER TABLE calendar_event_link
    ADD CONSTRAINT calendar_event_link_duplicate_key
    UNIQUE (tenant_id, appointment_id, provider);

-- rollback: ALTER TABLE calendar_event_link DROP CONSTRAINT IF EXISTS calendar_event_link_duplicate_key;
