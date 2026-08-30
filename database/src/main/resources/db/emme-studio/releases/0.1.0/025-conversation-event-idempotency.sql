-- liquibase formatted sql
-- changeset emme:025-conversation-event-idempotency
-- comment: Durable AI-turn reconciliation marker on assistant conversation events.

ALTER TABLE conversation_event
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS idx_conversation_event_idempotency
    ON conversation_event(tenant_id, conversation_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- rollback: DROP INDEX IF EXISTS idx_conversation_event_idempotency;
-- rollback: ALTER TABLE conversation_event DROP COLUMN IF EXISTS idempotency_key;
