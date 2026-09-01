-- liquibase formatted sql
-- changeset emme:030-ai-semantic-cache-response-identity
-- comment: Include response and dependency versions in semantic-cache identity.

ALTER TABLE ai_semantic_cache
    ADD COLUMN IF NOT EXISTS response_provider VARCHAR(120),
    ADD COLUMN IF NOT EXISTS response_model VARCHAR(160),
    ADD COLUMN IF NOT EXISTS knowledge_version VARCHAR(160),
    ADD COLUMN IF NOT EXISTS policy_version VARCHAR(160),
    ADD COLUMN IF NOT EXISTS source_version VARCHAR(160);

CREATE INDEX IF NOT EXISTS idx_ai_semantic_cache_response_identity
    ON ai_semantic_cache(
        tenant_id,
        principal_id,
        response_provider,
        response_model,
        knowledge_version,
        policy_version,
        source_version);

-- Existing rows remain unreadable until rewritten with a complete response identity.
-- rollback: DROP INDEX IF EXISTS idx_ai_semantic_cache_response_identity;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS source_version;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS policy_version;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS knowledge_version;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS response_model;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS response_provider;
