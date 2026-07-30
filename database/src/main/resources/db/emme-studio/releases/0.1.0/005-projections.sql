-- liquibase formatted sql
-- changeset emme:005-projections
-- comment: Projection tables retained for tenant studio schema history.

CREATE TABLE IF NOT EXISTS vector_projection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    chunk_id UUID NOT NULL,
    model_name VARCHAR(120) NOT NULL,
    model_version VARCHAR(80) NOT NULL,
    projection_value VARCHAR(2000) NOT NULL,
    projected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS projection_checkpoint (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    projection_type VARCHAR(20) NOT NULL,
    source_reference VARCHAR(120) NOT NULL,
    source_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CURRENT', 'FAILED', 'STALE')),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, projection_type, source_reference)
);

ALTER TABLE vector_projection ENABLE ROW LEVEL SECURITY;
ALTER TABLE projection_checkpoint ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS tenant_isolation ON vector_projection;
CREATE POLICY tenant_isolation ON vector_projection
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());
DROP POLICY IF EXISTS tenant_isolation ON projection_checkpoint;
CREATE POLICY tenant_isolation ON projection_checkpoint
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_vector_proj_tenant ON vector_projection(tenant_id);
CREATE INDEX IF NOT EXISTS idx_vector_proj_chunk ON vector_projection(chunk_id);
CREATE INDEX IF NOT EXISTS idx_vector_proj_model ON vector_projection(model_name, model_version);
CREATE INDEX IF NOT EXISTS idx_proj_checkpoint_tenant ON projection_checkpoint(tenant_id);
CREATE INDEX IF NOT EXISTS idx_proj_checkpoint_type_ref ON projection_checkpoint(projection_type, source_reference);

-- rollback: DROP TABLE IF EXISTS projection_checkpoint, vector_projection CASCADE;
