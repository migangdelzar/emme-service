-- liquibase formatted sql
-- changeset emme:015-ai-semantic-cache-idempotency
-- comment: Add durable idempotency for semantic-cache writes.

ALTER TABLE ai_semantic_cache
    ADD COLUMN IF NOT EXISTS write_idempotency_key VARCHAR(160);

UPDATE ai_semantic_cache
SET write_idempotency_key = 'legacy-' || id::text
WHERE write_idempotency_key IS NULL;

ALTER TABLE ai_semantic_cache
    ALTER COLUMN write_idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_ai_cache_write_idempotency
    ON ai_semantic_cache(tenant_id, principal_id, write_idempotency_key);

-- rollback: DROP INDEX IF EXISTS idx_ai_cache_write_idempotency;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS write_idempotency_key;
