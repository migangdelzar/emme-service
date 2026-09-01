-- liquibase formatted sql
-- changeset emme:032-ai-semantic-cache-full-response-identity
-- comment: Complete semantic response identity with channel, locale and quote-template version.

ALTER TABLE ai_semantic_cache
    ADD COLUMN IF NOT EXISTS channel VARCHAR(32),
    ADD COLUMN IF NOT EXISTS locale VARCHAR(32),
    ADD COLUMN IF NOT EXISTS quote_template_version VARCHAR(160);

CREATE INDEX IF NOT EXISTS idx_ai_semantic_cache_full_response_identity
    ON ai_semantic_cache(
        tenant_id,
        principal_id,
        channel,
        locale,
        quote_template_version,
        response_provider,
        response_model,
        knowledge_version,
        policy_version,
        source_version);

-- Existing rows remain unreadable until rewritten with the complete identity.
-- rollback: DROP INDEX IF EXISTS idx_ai_semantic_cache_full_response_identity;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS quote_template_version;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS locale;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS channel;
