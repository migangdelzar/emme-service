-- liquibase formatted sql
-- changeset emme:029-ai-embedding-model-name
-- comment: Persist the configured embedding model name as part of semantic identity.

ALTER TABLE ai_intent_reference
    ADD COLUMN IF NOT EXISTS embedding_model_name VARCHAR(120);
ALTER TABLE ai_tool_reference
    ADD COLUMN IF NOT EXISTS embedding_model_name VARCHAR(120);
ALTER TABLE ai_semantic_cache
    ADD COLUMN IF NOT EXISTS embedding_model_name VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_ai_intent_embedding_model_identity
    ON ai_intent_reference(tenant_id, embedding_model_name, embedding_model_version);
CREATE INDEX IF NOT EXISTS idx_ai_tool_embedding_model_identity
    ON ai_tool_reference(tenant_id, embedding_model_name, embedding_model_version);
CREATE INDEX IF NOT EXISTS idx_ai_semantic_embedding_model_identity
    ON ai_semantic_cache(tenant_id, principal_id, embedding_model_name, embedding_model_version);

-- Existing rows with no recorded model name remain readable only after reindexing.
-- rollback: DROP INDEX IF EXISTS idx_ai_semantic_embedding_model_identity;
-- rollback: DROP INDEX IF EXISTS idx_ai_tool_embedding_model_identity;
-- rollback: DROP INDEX IF EXISTS idx_ai_intent_embedding_model_identity;
-- rollback: ALTER TABLE ai_semantic_cache DROP COLUMN IF EXISTS embedding_model_name;
-- rollback: ALTER TABLE ai_tool_reference DROP COLUMN IF EXISTS embedding_model_name;
-- rollback: ALTER TABLE ai_intent_reference DROP COLUMN IF EXISTS embedding_model_name;
