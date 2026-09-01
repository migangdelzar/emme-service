-- liquibase formatted sql
-- changeset emme:007-catalog-search
-- comment: Catalog and hybrid search structures for tenant studio schemas.

ALTER TABLE document_chunk
    ADD COLUMN IF NOT EXISTS embedding vector(768),
    ADD COLUMN IF NOT EXISTS content_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('spanish', content)) STORED;

CREATE INDEX IF NOT EXISTS idx_chunk_embedding ON document_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_chunk_tsv ON document_chunk USING gin (content_tsv);

CREATE TABLE IF NOT EXISTS catalog_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    service_id UUID NOT NULL REFERENCES service(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    price_notes VARCHAR(500),
    duration_minutes INTEGER
        CHECK (duration_minutes IS NULL OR (duration_minutes >= 1 AND duration_minutes <= 1440)),
    materials VARCHAR(2000),
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','RETIRED')),
    search_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('spanish', name || ' ' || coalesce(description, ''))) STORED,
    embedding vector(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, service_id, code)
);

CREATE TABLE IF NOT EXISTS catalog_item_image (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    catalog_item_id UUID NOT NULL REFERENCES catalog_item(id) ON DELETE CASCADE,
    storage_key VARCHAR(500) NOT NULL,
    caption VARCHAR(2000),
    caption_tsv tsvector
        GENERATED ALWAYS AS (to_tsvector('spanish', coalesce(caption, ''))) STORED,
    embedding vector(768),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE catalog_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE catalog_item_image ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON catalog_item;
CREATE POLICY tenant_isolation ON catalog_item
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON catalog_item_image;
CREATE POLICY tenant_isolation ON catalog_item_image
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_catalog_item_tenant ON catalog_item(tenant_id);
CREATE INDEX IF NOT EXISTS idx_catalog_item_service ON catalog_item(service_id);
CREATE INDEX IF NOT EXISTS idx_catalog_item_embedding ON catalog_item USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_catalog_item_tsv ON catalog_item USING gin (search_tsv);
CREATE INDEX IF NOT EXISTS idx_cii_tenant ON catalog_item_image(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cii_item ON catalog_item_image(catalog_item_id);
CREATE INDEX IF NOT EXISTS idx_cii_embedding ON catalog_item_image USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_cii_tsv ON catalog_item_image USING gin (caption_tsv);

DROP TABLE IF EXISTS vector_projection;
DROP TABLE IF EXISTS projection_checkpoint;

-- rollback: DROP TABLE IF EXISTS catalog_item_image, catalog_item CASCADE;
-- rollback: DROP INDEX IF EXISTS idx_chunk_embedding;
-- rollback: DROP INDEX IF EXISTS idx_chunk_tsv;
-- rollback: ALTER TABLE document_chunk DROP COLUMN IF EXISTS embedding;
-- rollback: ALTER TABLE document_chunk DROP COLUMN IF EXISTS content_tsv;
-- rollback: Obsolete placeholder tables vector_projection and projection_checkpoint are intentionally not recreated by rollback because they were superseded by in-row search columns and catalog tables.
