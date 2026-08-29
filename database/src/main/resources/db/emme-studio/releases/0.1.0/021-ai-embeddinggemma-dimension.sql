-- liquibase formatted sql
-- changeset emme:021-ai-embeddinggemma-dimension
-- comment: Move semantic AI indexes to the verified EmbeddingGemma 768-dimensional profile.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM ai_intent_reference WHERE embedding IS NOT NULL LIMIT 1)
        OR EXISTS (SELECT 1 FROM ai_tool_reference WHERE embedding IS NOT NULL LIMIT 1)
        OR EXISTS (SELECT 1 FROM ai_semantic_cache WHERE embedding IS NOT NULL LIMIT 1)
    THEN
        RAISE EXCEPTION 'existing embeddings must be reindexed before changing the semantic vector dimension';
    END IF;
END
$$;

ALTER TABLE ai_intent_reference
    ALTER COLUMN embedding TYPE vector(768);
ALTER TABLE ai_tool_reference
    ALTER COLUMN embedding TYPE vector(768);
ALTER TABLE ai_semantic_cache
    ALTER COLUMN embedding TYPE vector(768);

-- rollback: ALTER TABLE ai_intent_reference ALTER COLUMN embedding TYPE vector(1024);
-- rollback: ALTER TABLE ai_tool_reference ALTER COLUMN embedding TYPE vector(1024);
-- rollback: ALTER TABLE ai_semantic_cache ALTER COLUMN embedding TYPE vector(1024);
