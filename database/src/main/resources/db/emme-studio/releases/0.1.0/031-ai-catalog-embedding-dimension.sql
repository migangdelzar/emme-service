-- liquibase formatted sql
-- changeset emme:031-ai-catalog-embedding-dimension
-- comment: Align catalog search vectors with the canonical configured 768-dimensional embedding space.

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM document_chunk WHERE embedding IS NOT NULL LIMIT 1)
        OR EXISTS (SELECT 1 FROM catalog_item WHERE embedding IS NOT NULL LIMIT 1)
        OR EXISTS (SELECT 1 FROM catalog_item_image WHERE embedding IS NOT NULL LIMIT 1)
    THEN
        RAISE EXCEPTION 'existing catalog embeddings must be reindexed before changing the semantic vector dimension';
    END IF;
END
$$;

DROP INDEX IF EXISTS idx_chunk_embedding;
DROP INDEX IF EXISTS idx_catalog_item_embedding;
DROP INDEX IF EXISTS idx_cii_embedding;

ALTER TABLE document_chunk
    ALTER COLUMN embedding TYPE vector(768);
ALTER TABLE catalog_item
    ALTER COLUMN embedding TYPE vector(768);
ALTER TABLE catalog_item_image
    ALTER COLUMN embedding TYPE vector(768);

CREATE INDEX IF NOT EXISTS idx_chunk_embedding
    ON document_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_catalog_item_embedding
    ON catalog_item USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_cii_embedding
    ON catalog_item_image USING hnsw (embedding vector_cosine_ops);

-- rollback: DROP INDEX IF EXISTS idx_chunk_embedding;
-- rollback: DROP INDEX IF EXISTS idx_catalog_item_embedding;
-- rollback: DROP INDEX IF EXISTS idx_cii_embedding;
-- rollback: ALTER TABLE document_chunk ALTER COLUMN embedding TYPE vector(1024);
-- rollback: ALTER TABLE catalog_item ALTER COLUMN embedding TYPE vector(1024);
-- rollback: ALTER TABLE catalog_item_image ALTER COLUMN embedding TYPE vector(1024);
