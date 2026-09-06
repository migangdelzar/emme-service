-- liquibase formatted sql
-- changeset emme:037-ai-workflow-correlations
-- comment: Correlate verified provider references with tenant-local AI workflows.

CREATE TABLE ai_payment_workflow_correlation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    workflow_id UUID NOT NULL,
    provider VARCHAR(80) NOT NULL,
    provider_reference VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ai_payment_workflow_correlation_provider_reference
        UNIQUE (tenant_id, provider, provider_reference),
    CONSTRAINT ai_payment_workflow_correlation_workflow
        UNIQUE (tenant_id, workflow_id, provider)
);

CREATE INDEX idx_ai_payment_workflow_correlation_workflow
    ON ai_payment_workflow_correlation (tenant_id, workflow_id);

ALTER TABLE ai_payment_workflow_correlation ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON ai_payment_workflow_correlation
    FOR ALL
    USING (tenant_id = current_tenant_id())
    WITH CHECK (tenant_id = current_tenant_id());

-- rollback: DROP TABLE IF EXISTS ai_payment_workflow_correlation;
