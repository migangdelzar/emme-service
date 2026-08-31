-- liquibase formatted sql
-- changeset emme:027-ai-design-images
-- Durable metadata for tenant-scoped design images; bytes remain in storage.

CREATE TABLE IF NOT EXISTS ai_design_image (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL REFERENCES ai_workflow_run(id) ON DELETE CASCADE,
    storage_key VARCHAR(1000) NOT NULL,
    media_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, workflow_id)
);

ALTER TABLE ai_design_image ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON ai_design_image;
CREATE POLICY tenant_isolation ON ai_design_image FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

CREATE INDEX IF NOT EXISTS idx_ai_design_image_tenant_workflow
    ON ai_design_image(tenant_id, workflow_id);

-- rollback: DROP TABLE IF EXISTS ai_design_image;
