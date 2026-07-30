-- liquibase formatted sql
-- changeset emme:003-documents
-- comment: Documents module tables for tenant studio schemas.

CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    source_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED'
        CHECK (status IN ('UPLOADED','PROCESSING','READY','FAILED','RETIRED')),
    version INTEGER NOT NULL DEFAULT 1 CHECK (version >= 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS document_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    document_id UUID NOT NULL REFERENCES document(id),
    chunk_index INTEGER NOT NULL CHECK (chunk_index >= 0),
    content VARCHAR(2000) NOT NULL,
    content_fingerprint VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (document_id, chunk_index)
);

ALTER TABLE document ENABLE ROW LEVEL SECURITY;
ALTER TABLE document_chunk ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON document;
CREATE POLICY tenant_isolation ON document
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON document_chunk;
CREATE POLICY tenant_isolation ON document_chunk
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_document_tenant ON document(tenant_id);
CREATE INDEX IF NOT EXISTS idx_document_chunk_doc ON document_chunk(document_id);

-- rollback: DROP TABLE IF EXISTS document_chunk, document CASCADE;
