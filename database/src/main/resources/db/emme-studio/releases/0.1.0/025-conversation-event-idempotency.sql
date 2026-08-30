-- liquibase formatted sql
-- changeset emme:025-conversation-event-idempotency
-- comment: Durable AI-turn reconciliation marker on assistant conversation events.

ALTER TABLE conversation_event
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

ALTER TABLE conversation_event
    ADD COLUMN IF NOT EXISTS idempotency_principal_id UUID;

DROP INDEX IF EXISTS idx_conversation_event_idempotency;

CREATE UNIQUE INDEX IF NOT EXISTS idx_conversation_event_idempotency
    ON conversation_event(
        tenant_id,
        idempotency_principal_id,
        conversation_id,
        event_type,
        idempotency_key
    )
    WHERE idempotency_key IS NOT NULL
      AND idempotency_principal_id IS NOT NULL;

-- rollback: DROP INDEX IF EXISTS idx_conversation_event_idempotency;
-- rollback: ALTER TABLE conversation_event DROP COLUMN IF EXISTS idempotency_principal_id;
-- rollback: ALTER TABLE conversation_event DROP COLUMN IF EXISTS idempotency_key;
